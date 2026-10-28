import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;

/**
 * Build script written in Java (run with the JDK source launcher, no Maven/Gradle needed).
 *
 *   java tools/Build.java            compile game into build/classes
 *   java tools/Build.java run        compile and start the game
 *   java tools/Build.java test       compile and run the test suite
 *   java tools/Build.java jar        compile and package dist/hollow-shift.jar
 *   java tools/Build.java package    jar + a Windows app with its own trimmed Java runtime,
 *                                    zipped to dist/HollowShift-windows.zip (no Java needed to play)
 *   java tools/Build.java inspect [path/to/www] [out.md]
 *                                    run the Construct 3 export inspector (research tool)
 *   java tools/Build.java convert-audio
 *                                    convert OGG/MP3 sources in third_party/ to WAV (uses JOrbis, JLayer)
 *   java tools/Build.java clean      delete build/ and dist/
 */
public class Build {
    static final Path ROOT = Path.of(System.getProperty("user.dir"));
    static final Path SRC = ROOT.resolve("src/main/java");
    static final Path RES = ROOT.resolve("src/main/resources");
    static final Path TEST_SRC = ROOT.resolve("src/test/java");
    static final Path TOOL_SRC = ROOT.resolve("tools/src");
    static final Path CLASSES = ROOT.resolve("build/classes");
    static final Path TEST_CLASSES = ROOT.resolve("build/test-classes");
    static final Path TOOL_CLASSES = ROOT.resolve("build/tool-classes");
    static final Path DIST = ROOT.resolve("dist");
    static final String MAIN = "hollowshift.Main";
    /** Version stamped on the packaged app; bump it for each release. */
    static final String VERSION = "1.0.1";
    /** Build-time only decoders for convert-audio (OGG, MP3); never packaged into the game. */
    static final Path JORBIS = ROOT.resolve("tools/lib/jorbis-0.0.17.4.jar");
    static final Path JLAYER = ROOT.resolve("tools/lib/jlayer-1.0.1.jar");

    public static void main(String[] args) throws Exception {
        String cmd = args.length > 0 ? args[0] : "compile";
        switch (cmd) {
            case "compile" -> compileMain();
            case "run" -> {
                compileMain();
                System.exit(java(CLASSES.toString(), MAIN, List.of()));
            }
            case "test" -> {
                compileMain();
                compile(TEST_SRC, TEST_CLASSES, CLASSES.toString());
                System.exit(java(CLASSES + java.io.File.pathSeparator + TEST_CLASSES, "hollowshift.tests.RunTests", List.of()));
            }
            case "jar" -> {
                compileMain();
                jar();
            }
            case "package" -> {
                compileMain();
                jar();
                packageApp();
            }
            case "inspect" -> {
                compileTools();
                List<String> rest = List.of(args).subList(1, args.length);
                System.exit(java(toolClasspath(), "hollowshift.tools.C3Inspect", rest));
            }
            case "convert-audio" -> {
                compileTools();
                System.exit(java(toolClasspath(), "hollowshift.tools.AudioToWav", List.of()));
            }
            case "clean" -> {
                delete(ROOT.resolve("build"));
                delete(DIST);
            }
            default -> {
                System.err.println("unknown command: " + cmd);
                System.exit(2);
            }
        }
    }

    static void compileMain() throws IOException {
        compile(SRC, CLASSES, null);
        copyTree(RES, CLASSES);
    }

    static void compileTools() throws IOException {
        compileMain();
        compile(TOOL_SRC, TOOL_CLASSES, String.join(java.io.File.pathSeparator, CLASSES.toString(), JORBIS.toString(), JLAYER.toString()));
    }

    static String toolClasspath() {
        return String.join(java.io.File.pathSeparator, CLASSES.toString(), TOOL_CLASSES.toString(), JORBIS.toString(), JLAYER.toString());
    }

    static void compile(Path srcDir, Path outDir, String classpath) throws IOException {
        delete(outDir);
        Files.createDirectories(outDir);
        List<String> files;
        try (Stream<Path> s = Files.walk(srcDir)) {
            files = s.filter(p -> p.toString().endsWith(".java")).map(Path::toString).toList();
        }
        List<String> opts = new ArrayList<>(List.of("-d", outDir.toString(), "-encoding", "UTF-8", "-Xlint:unchecked", "--release", "21"));
        if (classpath != null) {
            opts.add("-cp");
            opts.add(classpath);
        }
        opts.addAll(files);
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        int rc = javac.run(null, null, null, opts.toArray(String[]::new));
        if (rc != 0) {
            System.err.println("compilation failed: " + srcDir);
            System.exit(rc);
        }
        System.out.println("compiled " + files.size() + " files -> " + ROOT.relativize(outDir));
    }

    static void copyTree(Path from, Path to) throws IOException {
        if (!Files.exists(from)) return;
        try (Stream<Path> s = Files.walk(from)) {
            for (Path p : (Iterable<Path>) s::iterator) {
                Path target = to.resolve(from.relativize(p).toString());
                if (Files.isDirectory(p)) Files.createDirectories(target);
                else Files.copy(p, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    static int java(String classpath, String mainClass, List<String> args) throws Exception {
        String javaBin = ProcessHandle.current().info().command().orElse("java");
        List<String> cmd = new ArrayList<>(List.of(javaBin, "-cp", classpath, mainClass));
        cmd.addAll(args);
        return new ProcessBuilder(cmd).inheritIO().start().waitFor();
    }

    static void jar() throws IOException {
        Files.createDirectories(DIST);
        Path out = DIST.resolve("hollow-shift.jar");
        Manifest mf = new Manifest();
        mf.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        mf.getMainAttributes().put(Attributes.Name.MAIN_CLASS, MAIN);
        try (OutputStream os = Files.newOutputStream(out); JarOutputStream jos = new JarOutputStream(os, mf);
             Stream<Path> s = Files.walk(CLASSES)) {
            for (Path p : (Iterable<Path>) s.filter(Files::isRegularFile)::iterator) {
                jos.putNextEntry(new JarEntry(CLASSES.relativize(p).toString().replace('\\', '/')));
                Files.copy(p, jos);
                jos.closeEntry();
            }
        }
        System.out.println("wrote " + ROOT.relativize(out) + " (" + Files.size(out) / 1024 + " KB)");
    }

    /**
     * Builds a self-contained app with jpackage: HollowShift.exe plus a jlink'ed runtime
     * holding only the modules the game uses (java.base, java.desktop), then zips it.
     */
    static void packageApp() throws Exception {
        Path input = ROOT.resolve("build/package-input");
        Path out = ROOT.resolve("build/package");
        delete(input);
        delete(out);
        Files.createDirectories(input);
        Files.copy(DIST.resolve("hollow-shift.jar"), input.resolve("hollow-shift.jar"));
        java.util.spi.ToolProvider jpackage = java.util.spi.ToolProvider.findFirst("jpackage")
                .orElseThrow(() -> new IllegalStateException("jpackage not found: run this with a full JDK"));
        int rc = jpackage.run(System.out, System.err,
                "--type", "app-image",
                "--name", "HollowShift",
                "--app-version", VERSION,
                "--vendor", "windfox1243",
                "--description", "HOLLOW SHIFT",
                "--input", input.toString(),
                "--main-jar", "hollow-shift.jar",
                "--main-class", MAIN,
                "--add-modules", "java.base,java.desktop",
                "--jlink-options", "--strip-debug --no-header-files --no-man-pages --compress=zip-6",
                "--dest", out.toString());
        if (rc != 0) throw new IllegalStateException("jpackage failed: " + rc);
        Path zip = DIST.resolve("HollowShift-windows.zip");
        Files.deleteIfExists(zip);
        Path app = out.resolve("HollowShift");
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(Files.newOutputStream(zip));
             Stream<Path> s = Files.walk(app)) {
            for (Path p : (Iterable<Path>) s.filter(Files::isRegularFile)::iterator) {
                zos.putNextEntry(new java.util.zip.ZipEntry("HollowShift/" + app.relativize(p).toString().replace('\\', '/')));
                Files.copy(p, zos);
                zos.closeEntry();
            }
        }
        System.out.println("wrote " + ROOT.relativize(zip) + " (" + Files.size(zip) / (1024 * 1024) + " MB)");
    }

    static void delete(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (Stream<Path> s = Files.walk(dir)) {
            for (Path p : (Iterable<Path>) s.sorted(Comparator.reverseOrder())::iterator) Files.delete(p);
        }
    }
}

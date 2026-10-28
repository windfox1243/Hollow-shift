package hollowshift.engine.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Reads text resources bundled in the jar / class path. */
public final class Resources {
    private Resources() {
    }

    /** Returns the resource as UTF-8 text, or null if it does not exist. */
    public static String text(String path) {
        try (InputStream in = Resources.class.getResourceAsStream(path)) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    public static String require(String path) {
        String t = text(path);
        if (t == null) throw new IllegalStateException("missing resource " + path);
        return t;
    }
}

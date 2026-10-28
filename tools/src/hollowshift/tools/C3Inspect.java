package hollowshift.tools;

import hollowshift.engine.io.Json;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Research tool: decodes a Construct 3 HTML5 export (data.json + objRefTable.js + c3runtime.js)
 * into readable pseudo-code, so the structure of the original game can be studied.
 * Output is written to a local file for study only and is not shipped with the game.
 *
 * Usage: C3Inspect [path/to/www] [output file]
 */
public class C3Inspect {
    private List<Object> project;
    private final List<String> refTable = new ArrayList<>();
    private final List<String> exprFuncs = new ArrayList<>();
    private final Map<Long, String> varNames = new HashMap<>();
    private final List<String> objectNames = new ArrayList<>();
    private PrintWriter out;

    public static void main(String[] args) throws IOException {
        Path www = Path.of(args.length > 0 ? args[0] : "../www");
        Path outFile = Path.of(args.length > 1 ? args[1] : "build/c3-dump.txt");
        new C3Inspect().run(www, outFile);
    }

    void run(Path www, Path outFile) throws IOException {
        project = Json.list(Json.obj(Json.parse(read(www.resolve("data.json")))).get("project"));
        loadRefTable(read(www.resolve("scripts/objRefTable.js")));
        loadExprFuncs(read(www.resolve("scripts/c3runtime.js")));
        for (Object ot : Json.list(project.get(3))) objectNames.add(Json.str(Json.list(ot).get(0), "?"));
        for (Object sheet : Json.list(project.get(6))) collectVars(Json.list(Json.list(sheet).get(1)));

        Files.createDirectories(outFile.toAbsolutePath().getParent());
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(outFile, StandardCharsets.UTF_8))) {
            out = w;
            summary();
            for (Object sheet : Json.list(project.get(6))) {
                List<Object> s = Json.list(sheet);
                out.println();
                out.println("==== event sheet: " + s.get(0) + " ====");
                events(Json.list(s.get(1)), 0);
            }
        }
        System.out.println("decoded " + Json.list(project.get(6)).size() + " event sheets, "
                + objectNames.size() + " object types, " + exprFuncs.size() + " expressions -> " + outFile);
    }

    private void summary() {
        out.println("project: " + project.get(0) + "   first layout: " + project.get(1));
        out.println("viewport: " + project.get(10) + "x" + project.get(11));
        out.println();
        out.println("-- object types (index: name <- plugin) --");
        List<Object> types = Json.list(project.get(3));
        for (int i = 0; i < types.size(); i++) {
            List<Object> t = Json.list(types.get(i));
            int plugin = Json.integer(t.get(1), -1);
            out.printf("%3d: %-22s <- %s%n", i, t.get(0), plugin >= 0 && plugin < refTable.size() ? refTable.get(plugin) : "?");
        }
        out.println();
        out.println("-- families (family, members...) --");
        for (Object fam : Json.list(project.get(4))) {
            List<Object> f = Json.list(fam);
            StringBuilder sb = new StringBuilder(obj(Json.integer(f.get(0), -1))).append(" = ");
            for (int i = 1; i < f.size(); i++) sb.append(i > 1 ? ", " : "").append(obj(Json.integer(f.get(i), -1)));
            out.println(sb);
        }
        out.println();
        out.println("-- layouts (name, size, event sheet, layers) --");
        for (Object lay : Json.list(project.get(5))) {
            List<Object> l = Json.list(lay);
            List<String> layers = new ArrayList<>();
            for (Object ly : Json.list(l.get(9))) {
                List<Object> layer = Json.list(ly);
                List<Object> instances = layer.size() > 14 ? Json.list(layer.get(14)) : List.of();
                layers.add(layer.get(0) + "(" + instances.size() + ")");
            }
            out.printf("%-14s %sx%s  sheet=%s  layers=%s%n", l.get(0), l.get(1), l.get(2), l.get(7), layers);
        }
        out.println();
        out.println("-- audio files --");
        for (Object a : Json.list(project.get(7))) out.println("  " + Json.list(a).get(0));
    }

    // ---- events ---------------------------------------------------------------

    private void collectVars(List<Object> evs) {
        for (Object e : evs) {
            List<Object> ev = Json.list(e);
            if (ev.isEmpty()) continue;
            int kind = Json.integer(ev.get(0), -1);
            if (kind == 1) {
                varNames.put(((Number) ev.get(6)).longValue(), Json.str(ev.get(1), "?"));
            } else if (kind == 4) {
                // function parameters are encoded like variables: [1, name, type, initial, ..., sid@6]
                for (Object p : Json.list(Json.list(ev.get(1)).get(2))) {
                    List<Object> param = Json.list(p);
                    if (param.size() > 6 && param.get(6) instanceof Number sid) varNames.put(sid.longValue(), Json.str(param.get(1), "?"));
                }
            } else if (kind == 6) {
                // custom action: [objIndex?, name, objectType, ..., params@4]
                for (Object p : Json.list(Json.list(ev.get(1)).get(4))) {
                    List<Object> param = Json.list(p);
                    if (param.size() > 6 && param.get(6) instanceof Number sid) varNames.put(sid.longValue(), Json.str(param.get(1), "?"));
                }
            }
            if (kind == 0 || kind == 3 || kind == 4 || kind == 6) {
                if (ev.size() > 8) collectVars(Json.list(ev.get(8)));
            }
        }
    }

    private void events(List<Object> evs, int depth) {
        String ind = "  ".repeat(depth);
        for (Object e : evs) {
            List<Object> ev = Json.list(e);
            if (ev.isEmpty()) continue;
            int kind = Json.integer(ev.get(0), -1);
            switch (kind) {
                case 1 -> out.println(ind + (Json.bool(ev.get(5), false) ? "const " : "var ")
                        + ev.get(1) + " = " + ev.get(3) + (Json.bool(ev.get(4), false) ? " (static)" : ""));
                case 2 -> out.println(ind + "include " + ev.get(1));
                case 3 -> {
                    List<Object> g = Json.list(ev.get(1));
                    out.println(ind + "group \"" + g.get(1) + "\"" + (Json.bool(g.get(0), true) ? "" : " (inactive)") + " {");
                    block(ev, depth + 1);
                    out.println(ind + "}");
                }
                case 4 -> {
                    List<Object> f = Json.list(ev.get(1));
                    List<String> params = new ArrayList<>();
                    for (Object p : Json.list(f.get(2))) params.add(Json.str(Json.list(p).get(1), "?"));
                    out.println(ind + "function " + f.get(0) + "(" + String.join(", ", params) + ") {");
                    block(ev, depth + 1);
                    out.println(ind + "}");
                }
                case 6 -> {
                    List<Object> f = Json.list(ev.get(1));
                    List<String> params = new ArrayList<>();
                    for (Object p : Json.list(f.get(4))) params.add(Json.str(Json.list(p).get(1), "?"));
                    out.println(ind + "custom action " + obj(Json.integer(f.get(2), -1)) + "." + f.get(1) + "(" + String.join(", ", params) + ") {");
                    block(ev, depth + 1);
                    out.println(ind + "}");
                }
                case 0 -> {
                    List<String> conds = new ArrayList<>();
                    for (Object c : Json.list(ev.get(6))) conds.add(condition(Json.list(c)));
                    out.println(ind + "when " + (conds.isEmpty() ? "(every tick)" : String.join(" AND ", conds)) + " {");
                    actions(Json.list(ev.get(7)), depth + 1);
                    if (ev.size() > 8) events(Json.list(ev.get(8)), depth + 1);
                    out.println(ind + "}");
                }
                default -> out.println(ind + "?? " + Json.write(ev));
            }
        }
    }

    private void block(List<Object> ev, int depth) {
        List<Object> conds = Json.list(ev.get(6));
        if (!conds.isEmpty()) {
            List<String> cs = new ArrayList<>();
            for (Object c : conds) cs.add(condition(Json.list(c)));
            out.println("  ".repeat(depth) + "// conditions: " + String.join(" AND ", cs));
        }
        actions(Json.list(ev.get(7)), depth);
        if (ev.size() > 8) events(Json.list(ev.get(8)), depth);
    }

    private String condition(List<Object> c) {
        boolean inverted = c.size() > 5 && Json.bool(c.get(5), false);
        List<Object> params = c.size() > 9 ? Json.list(c.get(9)) : List.of();
        return (inverted ? "NOT " : "") + ace(c) + params(params);
    }

    private void actions(List<Object> acts, int depth) {
        String ind = "  ".repeat(depth);
        for (Object a : acts) {
            List<Object> act = Json.list(a);
            if (act.isEmpty()) continue;
            int objIdx = Json.integer(act.get(0), 0);
            List<Object> params = act.size() > 6 ? Json.list(act.get(6)) : List.of();
            if (objIdx == -2) out.println(ind + "call " + act.get(1) + params(params));
            else if (act.get(1) instanceof String js) out.println(ind + obj(objIdx) + "." + js + params(params));
            else out.println(ind + ace(act) + params(params));
        }
    }

    private String ace(List<Object> ace) {
        int objIdx = Json.integer(ace.get(0), -1);
        int aceIdx = Json.integer(ace.get(1), -1);
        String name = aceIdx >= 0 && aceIdx < refTable.size() ? refTable.get(aceIdx) : "ace#" + aceIdx;
        name = name.replaceFirst("^C3\\.(Plugins|Behaviors)\\.", "");
        return (objIdx < 0 ? "System" : obj(objIdx)) + ":" + name.substring(name.lastIndexOf('.') + 1)
                + (name.contains(".") ? "<" + name.substring(0, name.indexOf('.')) + ">" : "");
    }

    private String params(List<Object> params) {
        if (params.isEmpty()) return "";
        List<String> ps = new ArrayList<>();
        for (Object p : params) ps.add(param(Json.list(p)));
        return "(" + String.join(", ", ps) + ")";
    }

    private String param(List<Object> p) {
        if (p.size() < 2) return Json.write(p);
        int kind = Json.integer(p.get(0), -1);
        Object v = p.get(1);
        if (kind == 11 && v instanceof Number sid) return varName(sid.longValue());
        if (kind == 4 && v instanceof Number idx) return obj(idx.intValue());
        if (kind == 8 && v instanceof Number op) return switch (op.intValue()) {
            case 0 -> "=";
            case 1 -> "!=";
            case 2 -> "<";
            case 3 -> "<=";
            case 4 -> ">";
            case 5 -> ">=";
            default -> "op" + op;
        };
        if (v instanceof List<?> l && !l.isEmpty() && l.get(0) instanceof Number) return expr(Json.list(v));
        return Json.write(v);
    }

    /** Expression: [funcIndex, node0, node1, ...]; the body comes from C3_ExpressionFuncs. */
    private String expr(List<Object> e) {
        int idx = Json.integer(e.get(0), -1);
        String body = idx >= 0 && idx < exprFuncs.size() ? exprFuncs.get(idx) : "expr#" + idx;
        for (int i = 1; i < e.size(); i++) {
            List<Object> node = Json.list(e.get(i));
            String repl = null;
            if (node.size() == 2 && Json.integer(node.get(0), -1) == 3 && node.get(1) instanceof Number sid) repl = varName(sid.longValue());
            else if (node.size() == 2 && Json.integer(node.get(0), -1) == 4 && node.get(1) instanceof Number lit) repl = lit.toString();
            if (repl != null) body = body.replace("v" + (i - 1) + ".GetValue()", repl).replace("n" + (i - 1) + ".", repl + ".");
        }
        return "{" + body + "}";
    }

    private String obj(int idx) {
        return idx >= 0 && idx < objectNames.size() ? objectNames.get(idx) : "obj#" + idx;
    }

    private String varName(long sid) {
        return varNames.getOrDefault(sid, "var#" + sid);
    }

    // ---- JS tables --------------------------------------------------------------

    private void loadRefTable(String js) {
        Matcher m = Pattern.compile("C3\\.[A-Za-z0-9_.]+").matcher(js);
        while (m.find()) refTable.add(m.group());
    }

    /** Splits the array literal assigned to self.C3_ExpressionFuncs into its top-level elements. */
    private void loadExprFuncs(String js) {
        int start = js.lastIndexOf("C3_ExpressionFuncs = [");
        if (start < 0) return;
        start = js.indexOf('[', start) + 1;
        int depth = 0;
        char quote = 0;
        StringBuilder cur = new StringBuilder();
        for (int i = start; i < js.length(); i++) {
            char c = js.charAt(i);
            if (quote != 0) {
                cur.append(c);
                if (c == '\\') cur.append(js.charAt(++i));
                else if (c == quote) quote = 0;
                continue;
            }
            if (c == '"' || c == '\'' || c == '`') quote = c;
            if (c == '(' || c == '[' || c == '{') depth++;
            if (c == ')' || c == ']' || c == '}') {
                if (depth == 0) break;
                depth--;
            }
            if (c == ',' && depth == 0) {
                exprFuncs.add(simplify(cur.toString()));
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        if (!cur.toString().isBlank()) exprFuncs.add(simplify(cur.toString()));
    }

    private static String simplify(String fn) {
        String s = fn.strip();
        int ret = s.lastIndexOf("return () =>");
        if (ret >= 0) {
            s = s.substring(ret + "return () =>".length());
            int end = s.lastIndexOf(';');
            if (end >= 0) s = s.substring(0, end);
        } else if (s.startsWith("() =>")) {
            s = s.substring(5);
        }
        return s.strip().replaceAll("\\s+", " ");
    }

    private static String read(Path p) throws IOException {
        return Files.readString(p, StandardCharsets.UTF_8);
    }
}

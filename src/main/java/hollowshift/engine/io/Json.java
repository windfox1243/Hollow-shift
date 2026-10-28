package hollowshift.engine.io;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON parser/writer (no external libraries).
 * Objects become LinkedHashMap, arrays ArrayList, numbers Long or Double.
 * Accepts // and /* *&#47; comments and trailing commas so data files can be annotated.
 */
public final class Json {
    private final String src;
    private int pos;

    private Json(String src) {
        this.src = src;
    }

    public static Object parse(String text) {
        Json p = new Json(text);
        p.skipWs();
        Object v = p.value();
        p.skipWs();
        if (p.pos != p.src.length()) throw p.error("trailing data");
        return v;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String text) {
        return (Map<String, Object>) parse(text);
    }

    private Object value() {
        if (pos >= src.length()) throw error("unexpected end");
        char c = src.charAt(pos);
        return switch (c) {
            case '{' -> object();
            case '[' -> array();
            case '"' -> string();
            case 't' -> literal("true", Boolean.TRUE);
            case 'f' -> literal("false", Boolean.FALSE);
            case 'n' -> literal("null", null);
            default -> number();
        };
    }

    private Map<String, Object> object() {
        Map<String, Object> m = new LinkedHashMap<>();
        pos++;
        skipWs();
        while (peek() != '}') {
            String key = string();
            skipWs();
            expect(':');
            skipWs();
            m.put(key, value());
            skipWs();
            if (peek() == ',') {
                pos++;
                skipWs();
            } else if (peek() != '}') {
                throw error("expected , or }");
            }
        }
        pos++;
        return m;
    }

    private List<Object> array() {
        List<Object> list = new ArrayList<>();
        pos++;
        skipWs();
        while (peek() != ']') {
            list.add(value());
            skipWs();
            if (peek() == ',') {
                pos++;
                skipWs();
            } else if (peek() != ']') {
                throw error("expected , or ]");
            }
        }
        pos++;
        return list;
    }

    private String string() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (pos >= src.length()) throw error("unterminated string");
            char c = src.charAt(pos++);
            if (c == '"') break;
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            char e = src.charAt(pos++);
            switch (e) {
                case 'n' -> sb.append('\n');
                case 't' -> sb.append('\t');
                case 'r' -> sb.append('\r');
                case 'b' -> sb.append('\b');
                case 'f' -> sb.append('\f');
                case 'u' -> {
                    sb.append((char) Integer.parseInt(src.substring(pos, pos + 4), 16));
                    pos += 4;
                }
                default -> sb.append(e);
            }
        }
        return sb.toString();
    }

    private Object number() {
        int start = pos;
        boolean isFloat = false;
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (c == '.' || c == 'e' || c == 'E') isFloat = true;
            else if (!(c == '-' || c == '+' || Character.isDigit(c))) break;
            pos++;
        }
        String s = src.substring(start, pos);
        if (s.isEmpty()) throw error("unexpected character '" + src.charAt(pos) + "'");
        if (!isFloat) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException ignored) {
                // too large for long: fall through to double
            }
        }
        return Double.parseDouble(s);
    }

    private Object literal(String word, Object v) {
        if (!src.startsWith(word, pos)) throw error("bad literal");
        pos += word.length();
        return v;
    }

    private void skipWs() {
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (Character.isWhitespace(c)) {
                pos++;
            } else if (src.startsWith("//", pos)) {
                while (pos < src.length() && src.charAt(pos) != '\n') pos++;
            } else if (src.startsWith("/*", pos)) {
                int end = src.indexOf("*/", pos + 2);
                pos = end < 0 ? src.length() : end + 2;
            } else {
                break;
            }
        }
    }

    private char peek() {
        if (pos >= src.length()) throw error("unexpected end");
        return src.charAt(pos);
    }

    private void expect(char c) {
        if (peek() != c) throw error("expected '" + c + "'");
        pos++;
    }

    private IllegalArgumentException error(String msg) {
        int line = 1;
        for (int i = 0; i < Math.min(pos, src.length()); i++) if (src.charAt(i) == '\n') line++;
        return new IllegalArgumentException("JSON " + msg + " at line " + line);
    }

    // ---- writing -------------------------------------------------------------

    public static String write(Object v) {
        StringBuilder sb = new StringBuilder();
        write(v, sb, 0);
        return sb.toString();
    }

    private static void write(Object v, StringBuilder sb, int indent) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof String s) {
            quote(s, sb);
        } else if (v instanceof Number || v instanceof Boolean) {
            sb.append(v);
        } else if (v instanceof Map<?, ?> m) {
            sb.append("{\n");
            int i = 0;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                sb.append("  ".repeat(indent + 1));
                quote(String.valueOf(e.getKey()), sb);
                sb.append(": ");
                write(e.getValue(), sb, indent + 1);
                if (++i < m.size()) sb.append(',');
                sb.append('\n');
            }
            sb.append("  ".repeat(indent)).append('}');
        } else if (v instanceof List<?> l) {
            sb.append('[');
            for (int i = 0; i < l.size(); i++) {
                if (i > 0) sb.append(", ");
                write(l.get(i), sb, indent + 1);
            }
            sb.append(']');
        } else {
            quote(v.toString(), sb);
        }
    }

    private static void quote(String s, StringBuilder sb) {
        sb.append('"');
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        sb.append('"');
    }

    // ---- typed accessors -------------------------------------------------------

    public static double num(Object v, double def) {
        return v instanceof Number n ? n.doubleValue() : def;
    }

    public static int integer(Object v, int def) {
        return v instanceof Number n ? n.intValue() : def;
    }

    public static String str(Object v, String def) {
        return v instanceof String s ? s : def;
    }

    public static boolean bool(Object v, boolean def) {
        return v instanceof Boolean b ? b : def;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> list(Object v) {
        return v instanceof List<?> l ? (List<Object>) l : List.of();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> obj(Object v) {
        return v instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }
}

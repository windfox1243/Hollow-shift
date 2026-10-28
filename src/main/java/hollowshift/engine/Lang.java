package hollowshift.engine;

import hollowshift.engine.io.Json;
import hollowshift.engine.io.Resources;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Translated text. Strings live in /lang/&lt;code&gt;.json as a flat map of keys;
 * "{0}", "{1}"... are replaced by arguments. A key missing from the current
 * language falls back to English, then to the key itself.
 */
public final class Lang {
    public enum Language {
        EN("en", "English"),
        VI("vi", "Tiếng Việt");

        public final String code;
        public final String nativeName;

        Language(String code, String nativeName) {
            this.code = code;
            this.nativeName = nativeName;
        }

        public static Language fromCode(String code) {
            for (Language l : values()) if (l.code.equals(code)) return l;
            return EN;
        }

        public Language next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private static final Map<String, Object> ENGLISH = load(Language.EN);
    private static Language current = Language.EN;
    private static Map<String, Object> strings = ENGLISH;

    private Lang() {
    }

    private static Map<String, Object> load(Language l) {
        String text = Resources.text("/lang/" + l.code + ".json");
        return text == null ? Map.of() : Json.parseObject(text);
    }

    public static void set(Language l) {
        current = l;
        strings = l == Language.EN ? ENGLISH : load(l);
    }

    public static Language current() {
        return current;
    }

    /** Translated string for the key, with {0}, {1}... filled in. */
    public static String t(String key, Object... args) {
        Object v = strings.get(key);
        if (!(v instanceof String)) v = ENGLISH.get(key);
        String s = v instanceof String str ? str : key;
        for (int i = 0; i < args.length; i++) s = s.replace("{" + i + "}", String.valueOf(args[i]));
        return s;
    }

    /** Translated list of strings (tips). */
    public static List<String> list(String key) {
        Object v = strings.get(key);
        if (!(v instanceof List<?>)) v = ENGLISH.get(key);
        List<String> out = new ArrayList<>();
        for (Object o : Json.list(v)) out.add(String.valueOf(o));
        return out;
    }
}

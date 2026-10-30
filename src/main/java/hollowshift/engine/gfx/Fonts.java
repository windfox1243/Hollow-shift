package hollowshift.engine.gfx;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** System fonts for text drawn at full window resolution (signs, wall writing), with fallbacks. */
public final class Fonts {
    private static final Set<String> AVAILABLE = Set.of(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
    private static final Map<String, Font> CACHE = new HashMap<>();

    private Fonts() {
    }

    /** Painted stencil lettering that can draw every letter of text. */
    public static Font sign(float size, String text) {
        return pick(List.of("Bahnschrift", "Arial Narrow", "Segoe UI", "Arial"), Font.BOLD, size, text);
    }

    /**
     * Hasty handwriting in chalk or coal that can draw every letter of text. The script fonts
     * lack most Vietnamese letters, so those lines fall back to a slanted plain face.
     */
    public static Font scrawl(float size, String text) {
        Font f = pick(List.of("Segoe Print", "Ink Free", "Segoe Script", "Comic Sans MS"), Font.PLAIN, size, text);
        return f.getFamily().equals(Font.DIALOG) || f.canDisplayUpTo(text) != -1
                ? pick(List.of("Segoe UI", "Tahoma"), Font.ITALIC, size, text) : f;
    }

    /** The first installed family that has all the glyphs (Vietnamese needs many); Dialog otherwise. */
    private static Font pick(List<String> families, int style, float size, String text) {
        for (String f : families) {
            if (!AVAILABLE.contains(f)) continue;
            Font font = CACHE.computeIfAbsent(f + style + size, k -> new Font(f, style, 1).deriveFont(size));
            if (font.canDisplayUpTo(text) == -1) return font;
        }
        return CACHE.computeIfAbsent(Font.DIALOG + style + size, k -> new Font(Font.DIALOG, style, 1).deriveFont(size));
    }
}

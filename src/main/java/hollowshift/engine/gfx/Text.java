package hollowshift.engine.gfx;

import hollowshift.engine.Lang;

import java.awt.*;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Text drawing with a drop shadow. Uses the bundled pixel font (/assets/font.ttf)
 * when it has every glyph of the string; otherwise (e.g. Vietnamese diacritics,
 * which the pixel font lacks) a system font with full Unicode coverage, drawn
 * without anti-aliasing so it still sits crisply on the pixel grid.
 */
public final class Text {
    public static final int LEFT = 0;
    public static final int CENTER = 1;
    public static final int RIGHT = 2;

    /** The bundled pixel font is drawn on an 8 px grid; other sizes distort its glyphs. */
    private static final int PIXEL_GRID = 8;

    private static final Font[] SIZES = new Font[64];
    private static final Font[] FALLBACK_SIZES = new Font[64];
    private static boolean pixelFont;
    private static final Font BASE = loadBase();
    private static final String FALLBACK_FAMILY = pickFallback();

    private Text() {
    }

    private static Font loadBase() {
        try (InputStream in = Text.class.getResourceAsStream("/assets/font.ttf")) {
            if (in != null) {
                pixelFont = true;
                return Font.createFont(Font.TRUETYPE_FONT, in);
            }
        } catch (Exception ignored) {
            // fall back to the system font
        }
        return new Font(Font.MONOSPACED, Font.BOLD, 12);
    }

    /** Tahoma reads well at small sizes and covers Vietnamese; Dialog maps to a Unicode font everywhere. */
    private static String pickFallback() {
        for (String f : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            if (f.equals("Tahoma")) return f;
        }
        return Font.DIALOG;
    }

    /** Requested sizes are rounded to the pixel font's grid: body text 8, menus/headings 16, titles 24/32. */
    static int snap(int size) {
        if (!pixelFont) return size;
        if (size < 12) return PIXEL_GRID;
        if (size < 20) return 2 * PIXEL_GRID;
        return size < 28 ? 3 * PIXEL_GRID : 4 * PIXEL_GRID;
    }

    public static Font font(int size) {
        int s = snap(size);
        if (SIZES[s] == null) SIZES[s] = BASE.deriveFont((float) s);
        return SIZES[s];
    }

    /** The font for this particular string: the pixel font if it can draw all of it. */
    public static Font font(int size, String text) {
        Font f = font(size);
        // the pixel font has a few accented Latin letters but not Vietnamese ones, so a
        // non-English language uses the fallback for every string to keep one consistent look
        boolean english = Lang.current() == Lang.Language.EN;
        if (!pixelFont || (english && f.canDisplayUpTo(text) == -1)) return f;
        int s = snap(size);
        if (FALLBACK_SIZES[s] == null) {
            // a touch larger than the pixel font: diacritics need the room
            int px = s <= 8 ? 11 : s <= 16 ? 15 : s <= 24 ? 22 : 30;
            FALLBACK_SIZES[s] = new Font(FALLBACK_FAMILY, s <= 8 ? Font.PLAIN : Font.BOLD, px);
        }
        return FALLBACK_SIZES[s];
    }

    public static int width(Graphics2D g, String s, int size) {
        return g.getFontMetrics(font(size, s)).stringWidth(s);
    }

    /** Draws text with its baseline at y. */
    public static void draw(Graphics2D g, String s, int x, int y, int size, Color color, int align) {
        g.setFont(font(size, s));
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        int w = g.getFontMetrics().stringWidth(s);
        int dx = align == CENTER ? -w / 2 : align == RIGHT ? -w : 0;
        g.setColor(new Color(0, 0, 0, Math.min(200, color.getAlpha())));
        g.drawString(s, x + dx + 1, y + 1);
        g.setColor(color);
        g.drawString(s, x + dx, y);
    }

    /** Word-wraps text to maxWidth and draws it line by line; returns the y after the last line. */
    public static int drawWrapped(Graphics2D g, String s, int x, int y, int maxWidth, int size, Color color, int align) {
        int lineH = size + 4;
        for (String line : wrap(g, s, maxWidth, size)) {
            draw(g, line, x, y, size, color, align);
            y += lineH;
        }
        return y;
    }

    public static List<String> wrap(Graphics2D g, String s, int maxWidth, int size) {
        FontMetrics fm = g.getFontMetrics(font(size, s));
        List<String> lines = new ArrayList<>();
        for (String para : s.split("\n")) {
            StringBuilder cur = new StringBuilder();
            for (String word : para.split(" ")) {
                String test = cur.isEmpty() ? word : cur + " " + word;
                if (fm.stringWidth(test) > maxWidth && !cur.isEmpty()) {
                    lines.add(cur.toString());
                    cur = new StringBuilder(word);
                } else {
                    cur = new StringBuilder(test);
                }
            }
            lines.add(cur.toString());
        }
        return lines;
    }
}

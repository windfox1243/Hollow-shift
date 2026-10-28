package hollowshift.engine.gfx;

import hollowshift.engine.io.Json;
import hollowshift.engine.io.Resources;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Sprite registry. Lookup order for a sprite name:
 * 1. an entry in /assets/sprites.json pointing at a region of a sheet (e.g. a CC0 tileset),
 * 2. /assets/sprites/&lt;name&gt;.png,
 * 3. procedurally drawn placeholder from {@link ProceduralArt}.
 */
public final class Art {
    private final Map<String, BufferedImage> cache = new HashMap<>();
    private final Map<String, BufferedImage> sheets = new HashMap<>();
    private final Map<String, Object> manifest;
    private final ProceduralArt procedural = new ProceduralArt();

    public Art() {
        String text = Resources.text("/assets/sprites.json");
        manifest = text == null ? Map.of() : Json.parseObject(text);
    }

    public BufferedImage get(String name) {
        BufferedImage img = cache.get(name);
        if (img == null) {
            img = load(name);
            cache.put(name, img);
        }
        return img;
    }

    private BufferedImage load(String name) {
        Object entry = manifest.get(name);
        if (entry != null) {
            Map<String, Object> e = Json.obj(entry);
            BufferedImage sheet = sheet(Json.str(e.get("sheet"), ""));
            if (sheet != null) {
                BufferedImage sub = copy(sheet.getSubimage(Json.integer(e.get("x"), 0), Json.integer(e.get("y"), 0),
                        Json.integer(e.get("w"), 16), Json.integer(e.get("h"), 16)));
                String tint = Json.str(e.get("tint"), null);
                return tint == null ? sub : tint(sub, java.awt.Color.decode(tint));
            }
        }
        BufferedImage png = image("/assets/sprites/" + name + ".png");
        return png != null ? png : procedural.draw(name);
    }

    private BufferedImage sheet(String path) {
        return sheets.computeIfAbsent(path, p -> image("/assets/" + p));
    }

    private static BufferedImage image(String path) {
        try (InputStream in = Art.class.getResourceAsStream(path)) {
            return in == null ? null : copy(ImageIO.read(in));
        } catch (Exception e) {
            return null;
        }
    }

    /** Multiplies every pixel by a colour (used e.g. to turn floor tiles into moss). */
    private static BufferedImage tint(BufferedImage img, java.awt.Color c) {
        for (int y = 0; y < img.getHeight(); y++)
            for (int x = 0; x < img.getWidth(); x++) {
                int p = img.getRGB(x, y);
                int r = ((p >> 16) & 0xff) * c.getRed() / 255;
                int g = ((p >> 8) & 0xff) * c.getGreen() / 255;
                int b = (p & 0xff) * c.getBlue() / 255;
                img.setRGB(x, y, (p & 0xff000000) | (r << 16) | (g << 8) | b);
            }
        return img;
    }

    /** Converts to INT_ARGB so drawing is fast and alpha works the same for every source. */
    private static BufferedImage copy(BufferedImage src) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        out.createGraphics().drawImage(src, 0, 0, null);
        return out;
    }
}

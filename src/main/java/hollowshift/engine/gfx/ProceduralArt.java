package hollowshift.engine.gfx;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

/** Code-drawn pixel art used as the fallback for every sprite name. */
final class ProceduralArt {
    private static final Color FLOOR = new Color(46, 44, 52);
    private static final Color MOSS = new Color(38, 62, 54);
    private static final Color WALL_TOP = new Color(72, 60, 56);
    private static final Color WALL_FACE = new Color(48, 38, 36);
    private static final Color OUTLINE = new Color(14, 12, 16);

    BufferedImage draw(String name) {
        int variant = 0;
        int us = name.lastIndexOf('_');
        if (us > 0 && name.substring(us + 1).chars().allMatch(Character::isDigit)) {
            variant = Integer.parseInt(name.substring(us + 1));
            name = name.substring(0, us);
        }
        return switch (name) {
            case "tile_floor" -> speckled(FLOOR, 10, variant, 0.18);
            case "tile_moss" -> speckled(MOSS, 14, variant + 50, 0.35);
            case "tile_plank" -> planks(variant);
            case "tile_wall" -> wallTop(variant);
            case "tile_wall_face" -> wallFace(variant);
            case "player" -> player(variant);
            case "creature" -> creature(variant);
            case "item_fuse" -> fuse();
            case "item_battery" -> battery();
            case "item_food" -> can();
            case "item_stone" -> stone();
            case "item_flare" -> flare(false);
            case "item_flare_lit" -> flare(true);
            case "fusebox" -> fuseBox(variant == 1);
            case "heater" -> heater(variant);
            case "lamp" -> lamp();
            case "elevator" -> elevator(variant == 1);
            case "lever" -> lever(variant == 1);
            case "crate" -> crate();
            case "note" -> note(variant == 1);
            case "valve" -> valve(variant == 1);
            case "item_key" -> key();
            case "item_fuel" -> fuelCan();
            case "generator" -> generator(variant);
            case "phone" -> phone(variant == 1);
            case "junction" -> junction(variant == 1);
            case "decor_bones" -> bones();
            case "decor_rubble" -> rubble(variant);
            case "decor_web" -> web();
            case "decor_pick" -> pick();
            case "decor_drum" -> drum();
            case "decor_beam" -> beam();
            case "decor_sandbags" -> sandbags();
            case "decor_coal" -> coalHeap();
            case "decor_tnt" -> tnt();
            case "decor_lockers" -> lockers();
            case "decor_spool" -> spool();
            case "decor_wheelbarrow" -> wheelbarrow();
            case "decor_toolrack" -> toolRack();
            case "decor_tipped" -> tippedCart();
            case "decor_helmet" -> helmet();
            case "decor_boots" -> boots();
            case "decor_tin" -> tin();
            case "decor_rope" -> rope();
            case "decor_coalbits" -> coalBits(variant);
            case "decor_chain" -> chain();
            case "decor_puddle" -> puddle(variant);
            case "decor_shrooms" -> shrooms();
            case "decor_lantern" -> brokenLantern();
            case "decor_papers" -> papers();
            case "decor_rail" -> rail();
            case "decor_shovel" -> shovel();
            case "decor_bolts" -> bolts();
            case "decor_candle" -> candle();
            case "decor_stain" -> stain(variant);
            default -> missing();
        };
    }

    private static BufferedImage img(int w, int h) {
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    }

    private static Color shade(Color c, int d) {
        return new Color(clamp(c.getRed() + d), clamp(c.getGreen() + d), clamp(c.getBlue() + d), c.getAlpha());
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private BufferedImage speckled(Color base, int spread, int seed, double density) {
        BufferedImage im = img(16, 16);
        Random r = new Random(seed * 7919L + base.getRGB());
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int d = r.nextDouble() < density ? r.nextInt(spread * 2) - spread : r.nextInt(5) - 2;
                im.setRGB(x, y, shade(base, d).getRGB());
            }
        // a few cracks / pebbles
        for (int i = 0; i < 2; i++) {
            int x = r.nextInt(14), y = r.nextInt(14);
            im.setRGB(x, y, shade(base, -14).getRGB());
            im.setRGB(x + 1, y, shade(base, -10).getRGB());
            im.setRGB(x + 1, y + 1, shade(base, 10).getRGB());
        }
        return im;
    }

    private BufferedImage planks(int v) {
        BufferedImage im = img(16, 16);
        Graphics2D g = im.createGraphics();
        Color wood = new Color(78, 58, 40);
        for (int i = 0; i < 4; i++) {
            g.setColor(shade(wood, (i + v) % 2 == 0 ? 0 : -8));
            g.fillRect(0, i * 4, 16, 4);
            g.setColor(shade(wood, -26));
            g.drawLine(0, i * 4 + 3, 15, i * 4 + 3);
            g.drawLine((i * 5 + v * 3) % 16, i * 4, (i * 5 + v * 3) % 16, i * 4 + 2);
        }
        g.dispose();
        return im;
    }

    private BufferedImage wallTop(int v) {
        BufferedImage im = speckled(WALL_TOP, 8, v + 100, 0.25);
        Graphics2D g = im.createGraphics();
        g.setColor(shade(WALL_TOP, -12));
        g.drawLine(0, 7, 15, 7);
        g.drawLine(v % 2 == 0 ? 5 : 10, 0, v % 2 == 0 ? 5 : 10, 7);
        g.drawLine(v % 2 == 0 ? 12 : 3, 8, v % 2 == 0 ? 12 : 3, 15);
        g.dispose();
        return im;
    }

    private BufferedImage wallFace(int v) {
        BufferedImage im = img(16, 16);
        Random r = new Random(v + 300);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int d = (int) (-y * 1.2) + r.nextInt(7) - 3;
                im.setRGB(x, y, shade(WALL_FACE, d).getRGB());
            }
        Graphics2D g = im.createGraphics();
        g.setColor(shade(WALL_FACE, 18));
        g.drawLine(0, 0, 15, 0);
        g.setColor(shade(WALL_FACE, -16));
        for (int i = 0; i < 3; i++) {
            int x = r.nextInt(16);
            g.drawLine(x, 2 + r.nextInt(4), x, 9 + r.nextInt(5));
        }
        g.dispose();
        return im;
    }

    /** 12x18 miner: frame 0 idle, 1-3 walk cycle. Faces right; mirrored at draw time. */
    private BufferedImage player(int frame) {
        BufferedImage im = img(12, 18);
        Graphics2D g = im.createGraphics();
        int bob = frame == 0 ? 0 : (frame % 2 == 1 ? -1 : 0);
        int legA = frame == 1 ? 1 : (frame == 3 ? -1 : 0);
        // legs
        g.setColor(new Color(40, 46, 70));
        g.fillRect(3, 13, 2, 4 + Math.max(0, legA));
        g.fillRect(7, 13, 2, 4 + Math.max(0, -legA));
        g.setColor(OUTLINE);
        g.fillRect(3, 17 + Math.max(0, legA) - 1, 2, 1);
        g.fillRect(7, 17 + Math.max(0, -legA) - 1, 2, 1);
        // body (overalls)
        g.setColor(new Color(58, 76, 120));
        g.fillRect(2, 7 + bob, 8, 7);
        g.setColor(new Color(214, 160, 60));
        g.fillRect(2, 9 + bob, 8, 1); // hi-vis stripe
        // head
        g.setColor(new Color(224, 186, 150));
        g.fillRect(3, 3 + bob, 6, 4);
        g.setColor(OUTLINE);
        g.fillRect(7, 4 + bob, 1, 1);
        // helmet
        g.setColor(new Color(236, 196, 72));
        g.fillRect(2, 1 + bob, 8, 3);
        g.fillRect(3, 0 + bob, 6, 1);
        g.setColor(new Color(255, 244, 200));
        g.fillRect(9, 2 + bob, 2, 1); // lamp
        g.dispose();
        return im;
    }

    /**
     * 44x36 side view, facing right: a gaunt crawler with an arched, ridged back,
     * short hind legs and a long head thrust forward on a low neck. Its arms are
     * not in the sprite; the Creature draws them as IK chains from the shoulder
     * at (30, 17). Frame 1 is mid-stride: the back arches and the legs gather.
     */
    private BufferedImage creature(int frame) {
        BufferedImage im = img(44, 36);
        Graphics2D g = im.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        Color skin = new Color(204, 196, 186);
        Color mid = new Color(162, 152, 144);
        Color dark = new Color(98, 88, 86);
        int arch = frame == 1 ? 2 : 0;
        // hind legs: thigh down-back, shin forward, splayed foot
        g.setColor(dark);
        g.fillPolygon(new int[]{6, 11, 9, 4}, new int[]{20, 20, 30 - arch, 30 - arch}, 4);
        g.fillPolygon(new int[]{4, 9, 13, 8}, new int[]{29 - arch, 29 - arch, 35, 35}, 4);
        g.setColor(mid);
        g.fillPolygon(new int[]{11, 16, 15, 10}, new int[]{20, 20, 30 + arch / 2, 30 + arch / 2}, 4);
        g.fillPolygon(new int[]{10, 15, 19, 14}, new int[]{29 + arch / 2, 29 + arch / 2, 35, 35}, 4);
        // torso: arched hump from the hips up to the shoulders
        g.setColor(mid);
        g.fillOval(3, 7 - arch, 30, 20 + arch);
        g.setColor(skin);
        g.fillOval(5, 6 - arch, 26, 15 + arch);
        // belly shadow
        g.setColor(dark);
        g.fillRect(9, 23, 18, 3);
        // spine ridge
        g.setColor(new Color(232, 226, 214));
        for (int i = 0; i < 5; i++) g.fillRect(9 + i * 4, 6 - arch + Math.abs(2 - i) / 2, 2, 2);
        // ribs
        g.setColor(mid);
        for (int i = 0; i < 3; i++) g.drawLine(13 + i * 4, 13 - arch, 11 + i * 4, 19);
        // low neck and long head, jaw hanging open
        g.setColor(mid);
        g.fillPolygon(new int[]{26, 33, 35, 28}, new int[]{12 - arch, 13 - arch, 21, 22}, 4);
        g.setColor(skin);
        g.fillOval(29, 13 - arch / 2, 14, 9);
        g.setColor(mid);
        g.fillPolygon(new int[]{31, 43, 41, 32}, new int[]{20 - arch / 2, 20 - arch / 2, 24, 25}, 4);
        g.setColor(new Color(30, 14, 16));
        g.fillRect(32, 20 - arch / 2, 10, 2);
        g.setColor(new Color(230, 224, 210));
        for (int i = 0; i < 5; i++) g.fillRect(33 + i * 2, 20 - arch / 2, 1, 1);
        // eye pits (glowing eyes are drawn over the darkness by the Creature)
        g.setColor(new Color(24, 14, 16));
        g.fillRect(36, 15 - arch / 2, 2, 2);
        g.fillRect(39, 15 - arch / 2, 2, 2);
        // shoulder socket
        g.setColor(dark);
        g.fillOval(27, 14 - arch, 6, 6);
        g.setColor(OUTLINE);
        g.drawArc(3, 7 - arch, 29, 19 + arch, 20, 160);
        g.dispose();
        return im;
    }

    private BufferedImage fuse() {
        BufferedImage im = img(10, 10);
        Graphics2D g = im.createGraphics();
        g.setColor(new Color(150, 150, 160));
        g.fillRect(1, 3, 2, 4);
        g.fillRect(7, 3, 2, 4);
        g.setColor(new Color(230, 200, 80));
        g.fillRect(3, 2, 4, 6);
        g.setColor(new Color(255, 240, 160));
        g.fillRect(3, 3, 4, 1);
        g.setColor(OUTLINE);
        g.drawRect(3, 2, 3, 5);
        g.dispose();
        return im;
    }

    private BufferedImage battery() {
        BufferedImage im = img(10, 10);
        Graphics2D g = im.createGraphics();
        g.setColor(new Color(70, 170, 90));
        g.fillRect(2, 2, 6, 8);
        g.setColor(new Color(200, 200, 200));
        g.fillRect(4, 0, 2, 2);
        g.setColor(new Color(240, 240, 220));
        g.fillRect(4, 4, 2, 4);
        g.fillRect(3, 5, 4, 2);
        g.setColor(OUTLINE);
        g.drawRect(2, 2, 5, 7);
        g.dispose();
        return im;
    }

    private BufferedImage can() {
        BufferedImage im = img(10, 10);
        Graphics2D g = im.createGraphics();
        g.setColor(new Color(160, 160, 170));
        g.fillRect(2, 2, 6, 7);
        g.setColor(new Color(190, 60, 50));
        g.fillRect(2, 4, 6, 3);
        g.setColor(new Color(210, 210, 220));
        g.fillRect(2, 1, 6, 1);
        g.setColor(OUTLINE);
        g.drawRect(2, 1, 5, 8);
        g.dispose();
        return im;
    }

    private BufferedImage stone() {
        BufferedImage im = img(10, 10);
        Graphics2D g = im.createGraphics();
        g.setColor(new Color(110, 106, 112));
        g.fillOval(1, 3, 8, 6);
        g.setColor(new Color(140, 136, 142));
        g.fillOval(2, 3, 4, 3);
        g.setColor(OUTLINE);
        g.drawOval(1, 3, 7, 5);
        g.dispose();
        return im;
    }

    private BufferedImage flare(boolean lit) {
        BufferedImage im = img(10, 10);
        Graphics2D g = im.createGraphics();
        g.setColor(new Color(200, 40, 40));
        g.fillRect(4, 3, 2, 7);
        g.setColor(lit ? new Color(255, 230, 160) : new Color(80, 30, 30));
        g.fillRect(4, 1, 2, 2);
        if (lit) {
            g.setColor(new Color(255, 120, 60, 180));
            g.fillRect(3, 0, 4, 1);
        }
        g.dispose();
        return im;
    }

    private BufferedImage fuseBox(boolean on) {
        BufferedImage im = img(14, 18);
        Graphics2D g = im.createGraphics();
        g.setColor(new Color(90, 96, 104));
        g.fillRect(1, 0, 12, 16);
        g.setColor(new Color(60, 64, 72));
        g.fillRect(3, 2, 8, 9);
        g.setColor(on ? new Color(120, 255, 140) : new Color(110, 30, 30));
        g.fillRect(5, 12, 4, 2);
        if (on) {
            g.setColor(new Color(230, 200, 80));
            g.fillRect(5, 4, 4, 5);
        }
        g.setColor(new Color(240, 200, 40));
        g.drawLine(2, 15, 11, 15);
        g.setColor(OUTLINE);
        g.drawRect(1, 0, 11, 16);
        g.fillRect(3, 17, 8, 1);
        g.dispose();
        return im;
    }

    private BufferedImage heater(int frame) {
        BufferedImage im = img(16, 16);
        Graphics2D g = im.createGraphics();
        g.setColor(new Color(60, 56, 60));
        g.fillRect(1, 5, 14, 11);
        g.setColor(new Color(30, 26, 28));
        g.fillRect(3, 7, 10, 6);
        Color[] flame = {new Color(255, 180, 60), new Color(255, 120, 40), new Color(255, 220, 120)};
        for (int i = 0; i < 4; i++) {
            g.setColor(flame[(i + frame) % 3]);
            int h = 2 + (i * 7 + frame * 3) % 3;
            g.fillRect(4 + i * 2, 12 - h, 2, h);
        }
        g.setColor(new Color(90, 84, 88));
        g.fillRect(6, 0, 4, 5);
        g.setColor(OUTLINE);
        g.drawRect(1, 5, 13, 10);
        g.dispose();
        return im;
    }

    private BufferedImage lamp() {
        BufferedImage im = img(8, 12);
        Graphics2D g = im.createGraphics();
        g.setColor(new Color(70, 70, 76));
        g.fillRect(3, 0, 2, 5);
        g.setColor(new Color(255, 230, 170));
        g.fillOval(1, 4, 6, 6);
        g.setColor(new Color(40, 40, 44));
        g.drawOval(1, 4, 5, 5);
        g.dispose();
        return im;
    }

    private BufferedImage elevator(boolean open) {
        BufferedImage im = img(40, 36);
        Graphics2D g = im.createGraphics();
        g.setColor(new Color(70, 72, 80));
        g.fillRect(0, 0, 40, 36);
        g.setColor(new Color(30, 30, 36));
        g.fillRect(4, 6, 32, 30);
        if (open) {
            g.setColor(new Color(255, 236, 170));
            g.fillRect(8, 8, 24, 28);
            g.setColor(new Color(90, 90, 100));
            g.fillRect(4, 6, 4, 30);
            g.fillRect(32, 6, 4, 30);
        } else {
            g.setColor(new Color(110, 112, 120));
            g.fillRect(5, 7, 15, 29);
            g.fillRect(20, 7, 15, 29);
            g.setColor(new Color(60, 60, 66));
            g.drawLine(20, 7, 20, 35);
            for (int y = 10; y < 36; y += 5) g.drawLine(6, y, 34, y);
        }
        g.setColor(open ? new Color(120, 255, 140) : new Color(200, 50, 40));
        g.fillRect(18, 1, 4, 3);
        g.setColor(OUTLINE);
        g.drawRect(0, 0, 39, 35);
        g.dispose();
        return im;
    }

    private BufferedImage lever(boolean on) {
        BufferedImage im = img(12, 16);
        Graphics2D g = im.createGraphics();
        g.setColor(new Color(80, 84, 92));
        g.fillRect(2, 6, 8, 10);
        g.setColor(new Color(40, 40, 46));
        g.fillRect(5, 7, 2, 6);
        g.setColor(new Color(170, 170, 176));
        if (on) g.drawLine(6, 8, 10, 2);
        else g.drawLine(6, 8, 2, 2);
        g.setColor(on ? new Color(120, 255, 140) : new Color(230, 60, 50));
        g.fillRect(on ? 9 : 1, 1, 2, 2);
        g.setColor(OUTLINE);
        g.drawRect(2, 6, 7, 9);
        g.dispose();
        return im;
    }

    /** A crumpled scrap of paper; it fades once read. */
    private BufferedImage note(boolean read) {
        BufferedImage im = img(10, 8);
        Graphics2D g = im.createGraphics();
        g.setColor(read ? new Color(130, 122, 104) : new Color(214, 202, 172));
        g.fillPolygon(new int[]{0, 9, 10, 1}, new int[]{1, 0, 7, 8}, 4);
        g.setColor(read ? new Color(100, 92, 80) : new Color(120, 100, 80));
        for (int i = 0; i < 3; i++) g.drawLine(2, 2 + i * 2, 7, 2 + i * 2);
        g.dispose();
        return im;
    }

    private BufferedImage crate() {
        BufferedImage im = img(16, 16);
        Graphics2D g = im.createGraphics();
        g.setColor(new Color(110, 82, 52));
        g.fillRect(1, 2, 14, 14);
        g.setColor(new Color(80, 58, 36));
        g.drawRect(1, 2, 13, 13);
        g.drawLine(1, 2, 14, 15);
        g.drawLine(14, 2, 1, 15);
        g.setColor(new Color(140, 108, 70));
        g.drawLine(2, 3, 13, 3);
        g.dispose();
        return im;
    }

    private BufferedImage missing() {
        BufferedImage im = img(8, 8);
        for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) im.setRGB(x, y, ((x + y) % 2 == 0) ? 0xffff00ff : 0xff000000);
        return im;
    }

    /** A pressure valve: a pipe stub with a hand wheel; open = wheel turned, a wisp of steam. */
    private BufferedImage valve(boolean open) {
        BufferedImage im = img(16, 16);
        Graphics2D g = im.createGraphics();
        g.setColor(OUTLINE);
        g.fillRect(6, 7, 4, 9);
        g.setColor(new Color(92, 84, 80));
        g.fillRect(7, 8, 2, 8);
        g.setColor(OUTLINE);
        g.drawOval(2, 1, 11, 11);
        g.setColor(open ? new Color(110, 150, 110) : new Color(170, 40, 34));
        g.drawOval(3, 2, 9, 9);
        if (open) {
            g.drawLine(4, 6, 11, 6);
            g.drawLine(7, 3, 7, 10);
        } else {
            g.drawLine(4, 3, 11, 10);
            g.drawLine(11, 3, 4, 10);
        }
        g.setColor(new Color(60, 56, 54));
        g.fillRect(6, 5, 3, 3);
        g.dispose();
        return im;
    }

    /** The lift gate key: big, iron, on a tag. */
    private BufferedImage key() {
        BufferedImage im = img(10, 10);
        Graphics2D g = im.createGraphics();
        g.setColor(OUTLINE);
        g.fillOval(0, 0, 6, 6);
        g.fillRect(4, 2, 6, 3);
        g.fillRect(7, 4, 3, 4);
        g.setColor(new Color(214, 176, 82));
        g.drawOval(1, 1, 3, 3);
        g.fillRect(5, 3, 4, 1);
        g.fillRect(8, 4, 1, 3);
        g.setColor(new Color(170, 40, 34));
        g.fillRect(0, 7, 3, 3);
        g.dispose();
        return im;
    }

    private BufferedImage bones() {
        BufferedImage im = img(14, 8);
        Graphics2D g = im.createGraphics();
        Color bone = new Color(196, 186, 162);
        g.setColor(bone);
        g.drawLine(1, 6, 9, 2);
        g.drawLine(4, 1, 12, 6);
        g.fillOval(0, 5, 2, 2);
        g.fillOval(8, 1, 2, 2);
        g.fillOval(3, 0, 2, 2);
        g.fillOval(11, 5, 2, 2);
        g.setColor(shade(bone, -40));
        g.fillOval(9, 4, 4, 4); // a skull, half buried
        g.setColor(OUTLINE);
        g.fillRect(10, 5, 1, 1);
        g.dispose();
        return im;
    }

    private BufferedImage rubble(int v) {
        BufferedImage im = img(16, 10);
        Random r = new Random(v * 31L + 7);
        Graphics2D g = im.createGraphics();
        for (int i = 0; i < 7; i++) {
            int s = 2 + r.nextInt(4), x = r.nextInt(16 - s), y = 3 + r.nextInt(7 - Math.min(s, 6));
            g.setColor(OUTLINE);
            g.fillRect(x, y + 1, s, s - 1);
            g.setColor(shade(WALL_TOP, r.nextInt(30) - 10));
            g.fillRect(x, y, s - 1, s - 1);
        }
        g.dispose();
        return im;
    }

    private BufferedImage web() {
        BufferedImage im = img(16, 16);
        Graphics2D g = im.createGraphics();
        g.setColor(new Color(210, 210, 220, 90));
        for (int i = 0; i < 5; i++) g.drawLine(0, 0, 15, i * 4);
        for (int i = 0; i < 4; i++) g.drawLine(0, 0, i * 4, 15);
        for (int k = 4; k <= 14; k += 5) {
            g.drawArc(-k, -k, k * 2, k * 2, 270, 90);
        }
        g.dispose();
        return im;
    }

    /** A miner's pick left where it was dropped. */
    private BufferedImage pick() {
        BufferedImage im = img(14, 10);
        Graphics2D g = im.createGraphics();
        g.setColor(new Color(110, 78, 50));
        g.drawLine(2, 9, 10, 2);
        g.setColor(OUTLINE);
        g.drawLine(6, 0, 13, 5);
        g.setColor(new Color(150, 150, 160));
        g.drawLine(7, 0, 13, 4);
        g.dispose();
        return im;
    }

    /** A red jerrycan. */
    private BufferedImage fuelCan() {
        BufferedImage im = img(10, 12);
        Graphics2D g = im.createGraphics();
        g.setColor(OUTLINE);
        g.fillRect(0, 2, 10, 10);
        g.setColor(new Color(170, 40, 34));
        g.fillRect(1, 3, 8, 8);
        g.setColor(new Color(210, 70, 56));
        g.drawLine(2, 4, 7, 9);
        g.setColor(OUTLINE);
        g.fillRect(2, 0, 4, 3);
        g.setColor(new Color(150, 150, 160));
        g.fillRect(6, 0, 2, 3);
        g.dispose();
        return im;
    }

    /** Generator: 0 = dead, 1/2 = running (the exhaust puffs). */
    private BufferedImage generator(int frame) {
        BufferedImage im = img(18, 16);
        Graphics2D g = im.createGraphics();
        g.setColor(OUTLINE);
        g.fillRect(0, 5, 18, 11);
        g.setColor(new Color(176, 140, 50));
        g.fillRect(1, 6, 16, 8);
        g.setColor(new Color(90, 72, 30));
        for (int x = 3; x < 12; x += 2) g.drawLine(x, 8, x, 12);
        g.setColor(new Color(70, 70, 76));
        g.fillRect(13, 7, 3, 6);
        g.setColor(frame == 0 ? new Color(110, 30, 30) : new Color(120, 255, 140));
        g.fillRect(14, 8, 1, 1);
        g.setColor(new Color(60, 60, 66));
        g.fillRect(2, 2, 2, 4);
        if (frame > 0) {
            g.setColor(new Color(120, 120, 130, 140));
            g.fillOval(frame == 1 ? 1 : 2, frame == 1 ? 0 : -1, 4, 3);
        }
        g.dispose();
        return im;
    }

    /** A crank telephone in a box on the wall. */
    private BufferedImage phone(boolean used) {
        BufferedImage im = img(12, 16);
        Graphics2D g = im.createGraphics();
        g.setColor(OUTLINE);
        g.fillRect(0, 0, 12, 14);
        g.setColor(new Color(96, 70, 46));
        g.fillRect(1, 1, 10, 12);
        g.setColor(new Color(200, 190, 160));
        g.fillOval(3, 2, 3, 3);
        g.fillOval(6, 2, 3, 3);
        g.setColor(OUTLINE);
        g.fillRect(2, 7, 7, 2);
        g.setColor(used ? new Color(120, 255, 140) : new Color(150, 150, 160));
        g.drawLine(10, 8, 11, 11);
        g.setColor(OUTLINE);
        g.fillRect(4, 14, 4, 2);
        g.dispose();
        return im;
    }

    /** A junction box on the wall: door hanging open, wires torn loose (or neatly back in). */
    private BufferedImage junction(boolean wired) {
        BufferedImage im = img(14, 16);
        Graphics2D g = im.createGraphics();
        g.setColor(OUTLINE);
        g.fillRect(0, 1, 12, 13);
        g.setColor(new Color(84, 92, 86));
        g.fillRect(1, 2, 10, 11);
        g.setColor(new Color(40, 40, 42));
        g.fillRect(2, 3, 8, 9);
        Color[] wires = {new Color(200, 50, 40), new Color(230, 200, 60), new Color(60, 110, 220), new Color(80, 190, 90)};
        for (int i = 0; i < 4; i++) {
            g.setColor(wires[i]);
            if (wired) g.drawLine(3 + i * 2, 4, 3 + i * 2, 10);
            else g.drawLine(3 + i * 2, 4, 2 + i * 3 - (i % 2) * 2, 13 + i % 2);
        }
        // the door, swung open to the right
        g.setColor(OUTLINE);
        g.fillRect(12, 1, 2, 13);
        g.setColor(new Color(110, 118, 110));
        g.fillRect(12, 2, 1, 11);
        g.setColor(new Color(214, 176, 82));
        g.fillRect(4, 1, 4, 1); // a hazard stripe on the lid
        g.dispose();
        return im;
    }

    // ---- props: heavy ones stand on a blocked tile (16x16), clutter lies flat --------------------

    private static Graphics2D pen(BufferedImage im) {
        return im.createGraphics();
    }

    /** A rusty oil drum with hoops. */
    private BufferedImage drum() {
        BufferedImage im = img(14, 16);
        Graphics2D g = pen(im);
        g.setColor(OUTLINE);
        g.fillRect(1, 1, 12, 15);
        Color rust = new Color(120, 62, 38);
        g.setColor(rust);
        g.fillRect(2, 2, 10, 13);
        g.setColor(shade(rust, 30));
        g.fillRect(3, 2, 2, 13);
        g.setColor(new Color(70, 44, 32));
        g.drawLine(2, 6, 11, 6);
        g.drawLine(2, 11, 11, 11);
        g.setColor(new Color(150, 90, 50));
        g.fillOval(3, 1, 8, 3);
        g.dispose();
        return im;
    }

    /** A timber prop holding up the roof, with a cross brace. */
    private BufferedImage beam() {
        BufferedImage im = img(12, 16);
        Graphics2D g = pen(im);
        Color wood = new Color(96, 70, 44);
        g.setColor(OUTLINE);
        g.fillRect(3, 0, 6, 16);
        g.setColor(wood);
        g.fillRect(4, 0, 4, 16);
        g.setColor(shade(wood, 20));
        g.drawLine(4, 0, 4, 15);
        g.setColor(OUTLINE);
        g.fillRect(0, 2, 12, 3);
        g.setColor(shade(wood, -10));
        g.fillRect(1, 3, 10, 1);
        g.setColor(new Color(150, 150, 160));
        g.fillRect(5, 8, 2, 1);
        g.dispose();
        return im;
    }

    private BufferedImage sandbags() {
        BufferedImage im = img(16, 12);
        Graphics2D g = pen(im);
        Color bag = new Color(150, 130, 96);
        int[][] bags = {{0, 6}, {5, 6}, {10, 6}, {2, 1}, {8, 1}};
        for (int[] b : bags) {
            g.setColor(OUTLINE);
            g.fillRoundRect(b[0], b[1], 7, 6, 3, 3);
            g.setColor(bag);
            g.fillRoundRect(b[0] + 1, b[1], 5, 5, 3, 3);
            g.setColor(shade(bag, 25));
            g.drawLine(b[0] + 2, b[1] + 1, b[0] + 4, b[1] + 1);
        }
        g.dispose();
        return im;
    }

    private BufferedImage coalHeap() {
        BufferedImage im = img(16, 10);
        Graphics2D g = pen(im);
        g.setColor(OUTLINE);
        g.fillOval(0, 2, 16, 9);
        g.setColor(new Color(34, 32, 36));
        g.fillOval(1, 2, 14, 7);
        Random r = new Random(5);
        for (int i = 0; i < 14; i++) {
            g.setColor(new Color(70, 70, 80));
            g.fillRect(2 + r.nextInt(11), 3 + r.nextInt(5), 1, 1);
        }
        g.dispose();
        return im;
    }

    /** A crate stencilled with a red warning: blasting charges. */
    private BufferedImage tnt() {
        BufferedImage im = img(14, 12);
        Graphics2D g = pen(im);
        g.setColor(OUTLINE);
        g.fillRect(0, 1, 14, 11);
        g.setColor(new Color(110, 82, 50));
        g.fillRect(1, 2, 12, 9);
        g.setColor(new Color(170, 36, 30));
        g.fillRect(3, 4, 8, 4);
        g.setColor(new Color(230, 210, 170));
        g.drawLine(4, 6, 9, 6);
        g.setColor(new Color(170, 36, 30));
        g.fillRect(10, 0, 2, 2);
        g.dispose();
        return im;
    }

    private BufferedImage lockers() {
        BufferedImage im = img(16, 16);
        Graphics2D g = pen(im);
        g.setColor(OUTLINE);
        g.fillRect(0, 0, 16, 16);
        Color metal = new Color(74, 88, 84);
        for (int i = 0; i < 3; i++) {
            g.setColor(metal);
            g.fillRect(1 + i * 5, 1, 4, 14);
            g.setColor(shade(metal, -25));
            for (int y = 3; y < 7; y += 2) g.drawLine(2 + i * 5, y, 3 + i * 5, y);
            g.setColor(new Color(180, 180, 170));
            g.fillRect(4 + i * 5, 8, 1, 2);
        }
        g.setColor(new Color(40, 46, 44));
        g.fillRect(6, 1, 4, 14); // one door hangs open on darkness
        g.dispose();
        return im;
    }

    private BufferedImage spool() {
        BufferedImage im = img(14, 14);
        Graphics2D g = pen(im);
        g.setColor(OUTLINE);
        g.fillOval(0, 0, 14, 14);
        g.setColor(new Color(110, 80, 52));
        g.fillOval(1, 1, 12, 12);
        g.setColor(new Color(40, 40, 46));
        g.fillOval(3, 3, 8, 8);
        g.setColor(new Color(150, 60, 40));
        for (int i = 4; i < 10; i += 2) g.drawArc(3, 3, 8, 8, i * 20, 140);
        g.setColor(OUTLINE);
        g.fillOval(6, 6, 2, 2);
        g.dispose();
        return im;
    }

    private BufferedImage wheelbarrow() {
        BufferedImage im = img(16, 12);
        Graphics2D g = pen(im);
        g.setColor(OUTLINE);
        g.fillPolygon(new int[]{1, 12, 10, 3}, new int[]{2, 2, 8, 8}, 4);
        g.setColor(new Color(90, 100, 96));
        g.fillPolygon(new int[]{2, 11, 9, 4}, new int[]{3, 3, 7, 7}, 4);
        g.setColor(new Color(96, 70, 44));
        g.drawLine(11, 4, 15, 9);
        g.setColor(OUTLINE);
        g.fillOval(3, 7, 5, 5);
        g.setColor(new Color(60, 60, 66));
        g.fillOval(4, 8, 3, 3);
        g.dispose();
        return im;
    }

    /** A rack with a shovel and a pick hung on it. */
    private BufferedImage toolRack() {
        BufferedImage im = img(16, 16);
        Graphics2D g = pen(im);
        Color wood = new Color(90, 66, 42);
        g.setColor(OUTLINE);
        g.fillRect(0, 2, 16, 3);
        g.fillRect(1, 2, 2, 14);
        g.fillRect(13, 2, 2, 14);
        g.setColor(wood);
        g.fillRect(1, 3, 14, 1);
        g.setColor(new Color(110, 78, 50));
        g.drawLine(5, 4, 5, 13);
        g.drawLine(10, 4, 10, 14);
        g.setColor(new Color(150, 150, 160));
        g.fillRect(4, 12, 3, 3);
        g.drawLine(8, 5, 12, 6);
        g.dispose();
        return im;
    }

    /** A mine cart on its side, ore spilled. */
    private BufferedImage tippedCart() {
        BufferedImage im = img(16, 14);
        Graphics2D g = pen(im);
        g.setColor(OUTLINE);
        g.fillPolygon(new int[]{1, 10, 13, 4}, new int[]{3, 0, 9, 12}, 4);
        g.setColor(new Color(84, 90, 96));
        g.fillPolygon(new int[]{2, 9, 12, 5}, new int[]{4, 1, 8, 11}, 4);
        g.setColor(OUTLINE);
        g.fillOval(0, 8, 4, 4);
        g.fillOval(6, 10, 4, 4);
        g.setColor(new Color(60, 56, 58));
        for (int i = 0; i < 5; i++) g.fillRect(11 + i % 3, 10 + i / 3 * 2, 2, 2);
        g.dispose();
        return im;
    }

    private BufferedImage helmet() {
        BufferedImage im = img(10, 7);
        Graphics2D g = pen(im);
        g.setColor(OUTLINE);
        g.fillArc(0, 0, 10, 12, 0, 180);
        g.fillRect(0, 5, 10, 2);
        g.setColor(new Color(200, 160, 50));
        g.fillArc(1, 1, 8, 10, 0, 180);
        g.setColor(new Color(220, 220, 200));
        g.fillRect(4, 1, 2, 2);
        g.dispose();
        return im;
    }

    private BufferedImage boots() {
        BufferedImage im = img(12, 7);
        Graphics2D g = pen(im);
        Color leather = new Color(70, 50, 36);
        for (int b = 0; b < 2; b++) {
            int x = b * 6;
            g.setColor(OUTLINE);
            g.fillRect(x, 0, 4, 7);
            g.fillRect(x, 4, 6, 3);
            g.setColor(leather);
            g.fillRect(x + 1, 1, 2, 5);
            g.fillRect(x + 1, 5, 4, 1);
        }
        g.dispose();
        return im;
    }

    /** A dented lunch tin, lid open. */
    private BufferedImage tin() {
        BufferedImage im = img(10, 7);
        Graphics2D g = pen(im);
        g.setColor(OUTLINE);
        g.fillRect(0, 2, 10, 5);
        g.setColor(new Color(120, 126, 130));
        g.fillRect(1, 3, 8, 3);
        g.setColor(OUTLINE);
        g.drawLine(1, 2, 3, 0);
        g.drawLine(3, 0, 9, 0);
        g.dispose();
        return im;
    }

    private BufferedImage rope() {
        BufferedImage im = img(11, 7);
        Graphics2D g = pen(im);
        g.setColor(new Color(150, 124, 80));
        g.drawOval(0, 0, 10, 6);
        g.drawOval(2, 1, 6, 4);
        g.setColor(new Color(110, 90, 58));
        g.drawOval(1, 1, 8, 5);
        g.drawLine(9, 4, 10, 6);
        g.dispose();
        return im;
    }

    private BufferedImage coalBits(int v) {
        BufferedImage im = img(12, 6);
        Random r = new Random(v * 17L + 3);
        Graphics2D g = pen(im);
        for (int i = 0; i < 6; i++) {
            int x = r.nextInt(10), y = r.nextInt(4);
            g.setColor(new Color(28, 26, 30));
            g.fillRect(x, y, 2, 2);
            g.setColor(new Color(80, 80, 90));
            g.fillRect(x, y, 1, 1);
        }
        g.dispose();
        return im;
    }

    private BufferedImage chain() {
        BufferedImage im = img(14, 6);
        Graphics2D g = pen(im);
        for (int i = 0; i < 5; i++) {
            g.setColor(new Color(110, 110, 118));
            g.drawOval(i * 3 - 1, (i % 2) * 2, 4, 3);
        }
        g.dispose();
        return im;
    }

    /** Standing water, a faint sheen. */
    private BufferedImage puddle(int v) {
        BufferedImage im = img(16, 8);
        Graphics2D g = pen(im);
        g.setColor(new Color(20, 26, 34, 200));
        g.fillOval(v % 2 == 0 ? 0 : 2, 1, 14, 7);
        g.fillOval(v % 2 == 0 ? 6 : 0, 0, 9, 5);
        g.setColor(new Color(110, 130, 150, 120));
        g.drawLine(4, 3, 7, 3);
        g.dispose();
        return im;
    }

    /** Pale cave mushrooms. */
    private BufferedImage shrooms() {
        BufferedImage im = img(10, 8);
        Graphics2D g = pen(im);
        int[][] caps = {{0, 3, 4}, {4, 0, 5}, {7, 4, 3}};
        for (int[] c : caps) {
            g.setColor(new Color(200, 196, 180));
            g.fillRect(c[0] + c[2] / 2, c[1] + 2, 1, 8 - c[1] - 2);
            g.setColor(OUTLINE);
            g.fillArc(c[0], c[1], c[2] + 1, 4, 0, 180);
            g.setColor(new Color(170, 190, 160));
            g.fillArc(c[0], c[1] + 1, c[2], 3, 0, 180);
        }
        g.dispose();
        return im;
    }

    private BufferedImage brokenLantern() {
        BufferedImage im = img(10, 8);
        Graphics2D g = pen(im);
        g.setColor(OUTLINE);
        g.fillRect(1, 1, 8, 6);
        g.setColor(new Color(80, 74, 66));
        g.fillRect(2, 2, 6, 4);
        g.setColor(new Color(170, 170, 150, 160));
        g.drawLine(3, 3, 5, 5);
        g.setColor(new Color(150, 140, 120));
        g.fillRect(8, 0, 2, 1);
        g.dispose();
        return im;
    }

    private BufferedImage papers() {
        BufferedImage im = img(14, 8);
        Graphics2D g = pen(im);
        g.setColor(new Color(180, 170, 140));
        g.fillPolygon(new int[]{0, 7, 8, 1}, new int[]{2, 0, 5, 7}, 4);
        g.setColor(new Color(160, 150, 120));
        g.fillPolygon(new int[]{6, 13, 12, 6}, new int[]{3, 2, 7, 8}, 4);
        g.setColor(new Color(90, 80, 70));
        g.drawLine(2, 3, 5, 2);
        g.drawLine(8, 4, 11, 4);
        g.dispose();
        return im;
    }

    /** A loose length of rail with two sleepers. */
    private BufferedImage rail() {
        BufferedImage im = img(16, 6);
        Graphics2D g = pen(im);
        g.setColor(new Color(80, 58, 38));
        g.fillRect(3, 0, 3, 6);
        g.fillRect(11, 0, 3, 6);
        g.setColor(new Color(120, 120, 128));
        g.fillRect(0, 1, 16, 1);
        g.fillRect(0, 4, 16, 1);
        g.dispose();
        return im;
    }

    private BufferedImage shovel() {
        BufferedImage im = img(14, 6);
        Graphics2D g = pen(im);
        g.setColor(new Color(100, 74, 46));
        g.drawLine(0, 1, 9, 3);
        g.setColor(OUTLINE);
        g.fillRect(9, 1, 5, 5);
        g.setColor(new Color(130, 130, 136));
        g.fillRect(10, 2, 3, 3);
        g.dispose();
        return im;
    }

    private BufferedImage bolts() {
        BufferedImage im = img(10, 5);
        Random r = new Random(9);
        Graphics2D g = pen(im);
        for (int i = 0; i < 7; i++) {
            g.setColor(new Color(150, 146, 140));
            int x = r.nextInt(9), y = r.nextInt(4);
            g.fillRect(x, y, 1, 1);
            g.setColor(new Color(90, 88, 84));
            g.fillRect(x + 1, y, 1, 1);
        }
        g.dispose();
        return im;
    }

    private BufferedImage candle() {
        BufferedImage im = img(6, 7);
        Graphics2D g = pen(im);
        g.setColor(new Color(200, 196, 170));
        g.fillOval(0, 4, 6, 3);
        g.fillRect(2, 2, 2, 4);
        g.setColor(OUTLINE);
        g.fillRect(2, 1, 1, 1);
        g.dispose();
        return im;
    }

    /** An old dark stain on the floor. */
    private BufferedImage stain(int v) {
        BufferedImage im = img(16, 9);
        Random r = new Random(v * 11L + 1);
        Graphics2D g = pen(im);
        Color c = v % 2 == 0 ? new Color(60, 14, 14, 150) : new Color(28, 24, 20, 150);
        g.setColor(c);
        g.fillOval(2, 2, 11, 6);
        for (int i = 0; i < 5; i++) g.fillOval(r.nextInt(14), r.nextInt(7), 2, 2);
        g.dispose();
        return im;
    }
}

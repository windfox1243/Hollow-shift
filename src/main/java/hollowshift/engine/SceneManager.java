package hollowshift.engine;

import java.awt.Color;
import java.awt.Graphics2D;

/** Holds the active scene and performs fade-to-black transitions between scenes. */
public final class SceneManager {
    private static final double FADE_TIME = 0.35;

    private Scene current;
    private Scene next;
    private double fade; // 0 = clear, 1 = black
    private boolean fadingOut;

    public void set(Scene scene) {
        if (current == null) {
            current = scene;
            current.enter();
            fade = 1;
            return;
        }
        next = scene;
        fadingOut = true;
    }

    public Scene current() {
        return current;
    }

    public boolean transitioning() {
        return next != null;
    }

    public void update(double dt) {
        if (fadingOut) {
            fade += dt / FADE_TIME;
            if (fade >= 1) {
                fade = 1;
                fadingOut = false;
                current.exit();
                current = next;
                next = null;
                current.enter();
            }
        } else if (fade > 0) {
            fade = Math.max(0, fade - dt / FADE_TIME);
        }
        if (current != null && next == null) current.update(dt);
    }

    public void render(Graphics2D g) {
        if (current != null) current.render(g);
        fade(g);
    }

    /** The full-resolution layer; the fade covers it too. */
    public void renderOverlay(Graphics2D g) {
        if (current != null) current.renderOverlay(g);
        fade(g);
    }

    private void fade(Graphics2D g) {
        if (fade > 0) {
            g.setColor(new Color(0, 0, 0, (int) (255 * Math.min(1, fade))));
            g.fillRect(0, 0, Window.WIDTH, Window.HEIGHT);
        }
    }
}

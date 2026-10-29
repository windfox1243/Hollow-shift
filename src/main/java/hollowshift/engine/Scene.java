package hollowshift.engine;

import java.awt.Graphics2D;

/** One screen of the game (menu, gameplay, death screen...). */
public abstract class Scene {
    protected final Game game;

    protected Scene(Game game) {
        this.game = game;
    }

    /** Called when the scene becomes active. */
    public void enter() {
    }

    /** Called when another scene replaces this one. */
    public void exit() {
    }

    public abstract void update(double dt);

    public abstract void render(Graphics2D g);

    /**
     * Drawn after the pixel buffer is scaled to the window, at the window's full resolution.
     * Coordinates are still the 640x360 logical ones. Used for text that must stay sharp.
     */
    public void renderOverlay(Graphics2D g) {
    }
}

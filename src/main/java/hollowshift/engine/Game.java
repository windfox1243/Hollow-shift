package hollowshift.engine;

import hollowshift.engine.audio.Audio;
import hollowshift.engine.gfx.Art;
import hollowshift.scenes.SplashScene;
import hollowshift.story.Campaign;
import hollowshift.story.SaveData;

import java.awt.Graphics2D;

/** Owns the shared services and runs the fixed-timestep loop (120 updates per second). */
public final class Game {
    public static final String TITLE = "HOLLOW SHIFT";
    // 120 Hz keeps motion smooth on high refresh rate screens; a frame costs ~1.5 ms
    private static final double STEP = 1.0 / 120.0;

    public final Input input = new Input();
    public final Window window = new Window(TITLE, input);
    public final Audio audio = new Audio();
    public final Art art = new Art();
    public final SceneManager scenes = new SceneManager();
    public final SaveData save = SaveData.load();
    public final Campaign campaign = Campaign.load();
    private volatile boolean quit;

    public Game() {
        Lang.set(Lang.Language.fromCode(save.language));
        save.applyBindings(input);
        applySettings();
    }

    public void applySettings() {
        audio.setVolumes(save.master, save.music, save.sfx);
        window.setFullscreen(save.fullscreen);
    }

    public void quit() {
        quit = true;
    }

    public void run() {
        scenes.set(new SplashScene(this));
        long last = System.nanoTime();
        double acc = 0;
        while (!quit && !window.closeRequested()) {
            long now = System.nanoTime();
            acc += Math.min(0.25, (now - last) / 1e9);
            last = now;
            boolean updated = false;
            while (acc >= STEP) {
                input.poll();
                scenes.update(STEP);
                acc -= STEP;
                updated = true;
            }
            if (updated) {
                Graphics2D g = window.beginFrame();
                scenes.render(g);
                g.dispose();
                window.present();
            } else {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        save.save();
        audio.shutdown();
        window.close();
        System.exit(0);
    }
}

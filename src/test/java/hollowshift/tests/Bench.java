package hollowshift.tests;

import hollowshift.engine.Input;
import hollowshift.engine.Window;
import hollowshift.engine.audio.Audio;
import hollowshift.engine.gfx.Art;
import hollowshift.game.Hud;
import hollowshift.game.Services;
import hollowshift.game.World;
import hollowshift.game.WorldBuilder;
import hollowshift.story.Campaign;
import hollowshift.story.SaveData;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/** Measures average update and render time per frame on a large endless shift. */
public final class Bench {
    public static void main(String[] args) {
        Audio audio = new Audio();
        audio.setVolumes(0, 0, 0);
        Services sv = new Services(audio, new Art(), new Input(), new SaveData());
        World w = WorldBuilder.build(sv, Campaign.load().endless(9, 42), 42);
        w.start();
        BufferedImage screen = new BufferedImage(Window.WIDTH, Window.HEIGHT, BufferedImage.TYPE_INT_RGB);
        Hud hud = new Hud();
        int warm = 300, frames = 1200;
        long upd = 0, ren = 0;
        for (int i = 0; i < warm + frames; i++) {
            long a = System.nanoTime();
            w.update(1 / 60.0);
            long b = System.nanoTime();
            Graphics2D g = screen.createGraphics();
            w.render(g);
            hud.render(g, w);
            g.dispose();
            long c = System.nanoTime();
            if (i >= warm) {
                upd += b - a;
                ren += c - b;
            }
        }
        System.out.printf("update %.3f ms/frame, render %.3f ms/frame (%d frames)%n", upd / 1e6 / frames, ren / 1e6 / frames, frames);
        System.exit(0);
    }
}

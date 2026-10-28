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
import hollowshift.story.ShiftDef;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;

/** Renders gameplay frames to build/snapshots/*.png so visuals can be checked without playing. */
public final class Snapshots {
    public static void main(String[] args) throws Exception {
        File dir = new File("build/snapshots");
        dir.mkdirs();
        Campaign c = Campaign.load();
        for (ShiftDef def : c.shifts()) {
            Audio audio = new Audio();
            audio.setVolumes(0, 0, 0);
            Services sv = new Services(audio, new Art(), new Input(), new SaveData());
            World w = WorldBuilder.build(sv, def, 77 + def.number());
            w.start();
            for (int i = 0; i < 30; i++) w.update(1 / 60.0);
            save(w, new File(dir, "shift" + def.number() + "_start.png"));
            // walk down for two seconds
            sv.input().simulate(KeyEvent.VK_S, true);
            sv.input().poll();
            for (int i = 0; i < 120; i++) w.update(1 / 60.0);
            save(w, new File(dir, "shift" + def.number() + "_walk.png"));
            // bring the Tenant into view, chasing, mid-stride
            sv.input().simulate(KeyEvent.VK_S, false);
            sv.input().poll();
            var tenant = w.creatures().get(0);
            tenant.setDormant(0);
            tenant.placeAt(w.player.x + 110, w.player.y - 20);
            for (int i = 0; i < 90; i++) {
                tenant.perceive(new hollowshift.ai.Stimulus(w.player.x, w.player.y, 1, hollowshift.ai.Stimulus.Kind.SIGHT));
                w.update(1 / 60.0);
            }
            save(w, new File(dir, "shift" + def.number() + "_tenant.png"));
        }
        System.out.println("snapshots written to " + dir.getPath());
        System.exit(0);
    }

    private static void save(World w, File f) throws Exception {
        BufferedImage img = new BufferedImage(Window.WIDTH, Window.HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        w.render(g);
        new hollowshift.game.FearOverlay().render(g, w);
        new Hud().render(g, w);
        g.dispose();
        ImageIO.write(img, "png", f);
    }
}

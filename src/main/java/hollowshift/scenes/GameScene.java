package hollowshift.scenes;

import hollowshift.engine.Lang;
import hollowshift.engine.Action;
import hollowshift.engine.Game;
import hollowshift.engine.Scene;
import hollowshift.engine.Window;
import hollowshift.engine.audio.Bus;
import hollowshift.engine.gfx.Text;
import hollowshift.game.FearOverlay;
import hollowshift.game.Hud;
import hollowshift.game.Services;
import hollowshift.game.World;
import hollowshift.game.WorldBuilder;
import hollowshift.rules.DeathCause;
import hollowshift.story.Run;

import java.awt.Color;
import java.awt.Graphics2D;

/** Plays one shift. Handles pause, death and completion. */
public final class GameScene extends Scene {
    private static final double DEATH_DELAY = 1.4;

    private final Run run;
    private final World world;
    private final Hud hud = new Hud();
    private final FearOverlay fearOverlay = new FearOverlay();
    private final Jumpscare jumpscare = new Jumpscare();
    private final NotePaper notePaper = new NotePaper();
    private final MenuList pauseMenu;
    private boolean paused;
    private double deathTimer;
    private double endTimer;

    public GameScene(Game game, Run run) {
        super(game);
        this.run = run;
        this.world = WorldBuilder.build(new Services(game.audio, game.art, game.input, game.save), run.def(game.campaign), run.levelSeed());
        pauseMenu = new MenuList(game)
                .add(Lang.t("pause.resume"), () -> paused = false)
                .add(Lang.t("pause.restart"), () -> {
                    run.newAttempt();
                    game.scenes.set(new GameScene(game, run));
                })
                .add(Lang.t("pause.settings"), () -> game.scenes.set(new SettingsScene(game, () -> this)))
                .add(Lang.t("pause.quit"), () -> game.scenes.set(new MenuScene(game)));
    }

    @Override
    public void enter() {
        if (world.time == 0) {
            world.start();
            world.toast(world.shift.title());
        } else {
            world.startAudio(); // back from Settings opened in the pause menu
        }
    }

    @Override
    public void exit() {
        world.stopAudio();
    }

    @Override
    public void update(double dt) {
        if (paused) {
            pauseMenu.update();
            if (game.input.pressed(Action.BACK)) paused = false;
            return;
        }
        if (world.death() != null) {
            if (deathTimer == 0) {
                world.stopAudio();
                // the recording's first hit lands ~0.1 s in, right on the visual snap
                if (scared()) game.audio.play("scare_main", Bus.SFX, 1.0, 1);
                else game.audio.play("death");
            }
            deathTimer += dt;
            world.camera.update(dt);
            if (deathTimer >= (scared() ? Jumpscare.DURATION : DEATH_DELAY)) {
                game.scenes.set(new DeathScene(game, run, world.death()));
            }
            return;
        }
        if (game.input.pressed(Action.BACK)) {
            paused = true;
            pauseMenu.select(0);
            game.audio.ui("ui_back");
            return;
        }
        if (world.reading() != null) {
            if (game.input.pressed(Action.INTERACT) || game.input.pressed(Action.CONFIRM)) {
                world.closeNote();
                game.audio.play("grab", Bus.SFX, 0.5, 1.1);
            }
            return;
        }
        world.update(dt);
        if (world.objectives().complete()) {
            endTimer += dt;
            if (endTimer > 0.3) finishShift();
        }
    }

    /** Caught by the Tenant with jumpscares enabled. */
    private boolean scared() {
        return world.death() == DeathCause.TENANT && game.save.jumpscare;
    }

    private void finishShift() {
        world.stopAudio();
        if (run.mode == Run.Mode.STORY) {
            boolean last = run.isLastStoryShift(game.campaign);
            game.save.shiftsUnlocked = Math.max(game.save.shiftsUnlocked, Math.min(run.shift() + 1, game.campaign.shifts().size()));
            if (last) game.save.storyFinished = true;
            if (world.objectives().stayed()) game.save.stayedBelow = true;
            game.save.save();
            if (last) {
                game.scenes.set(new EndingScene(game, world.objectives().stayed()));
                return;
            }
        } else {
            game.save.bestEndless = Math.max(game.save.bestEndless, run.shift());
            game.save.save();
        }
        game.scenes.set(new ShiftCompleteScene(game, run, world.time));
    }

    @Override
    public void render(Graphics2D g) {
        world.render(g);
        if (world.objectives().escaping()) {
            int h = (int) (Window.HEIGHT / 2.0 * world.objectives().escapeProgress());
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, Window.WIDTH, h);
            g.fillRect(0, Window.HEIGHT - h, Window.WIDTH, h);
        } else if (world.death() == null) {
            fearOverlay.render(g, world);
            hud.render(g, world);
            if (world.reading() != null) {
                String key = hollowshift.engine.Input.keyName(game.input.binding(Action.INTERACT)[0]);
                notePaper.render(g, world.readingNumber(), world.reading(), Lang.t("note.close", key));
            }
        }
        if (scared()) {
            jumpscare.render(g, deathTimer);
        } else if (world.death() != null) {
            int a = (int) Math.min(255, deathTimer / DEATH_DELAY * 255);
            g.setColor(new Color(90, 0, 0, Math.max(0, Math.min(200, 200 - a / 2))));
            g.fillRect(0, 0, Window.WIDTH, Window.HEIGHT);
            g.setColor(new Color(0, 0, 0, a));
            g.fillRect(0, 0, Window.WIDTH, Window.HEIGHT);
        }
        if (paused) {
            g.setColor(new Color(0, 0, 0, 180));
            g.fillRect(0, 0, Window.WIDTH, Window.HEIGHT);
            Text.draw(g, Lang.t("pause.title"), Window.WIDTH / 2, 110, 20, new Color(236, 214, 170), Text.CENTER);
            pauseMenu.render(g, Window.WIDTH / 2, 150, 20, 12);
            int y = 260;
            for (var r : world.rules.ordered()) {
                Text.draw(g, r.title(), Window.WIDTH / 2, y, 9, new Color(170, 160, 150), Text.CENTER);
                y += 12;
            }
        }
    }
}

package hollowshift.scenes;

import hollowshift.engine.Lang;
import hollowshift.engine.Action;
import hollowshift.engine.Game;
import hollowshift.engine.Scene;
import hollowshift.engine.Window;
import hollowshift.engine.gfx.Text;
import hollowshift.rules.Rule;
import hollowshift.rules.RuleSet;
import hollowshift.story.Run;
import hollowshift.story.ShiftDef;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Announces the shift: intro text (typewriter), then the rules. In the story, rules carried over
 * from earlier shifts are listed together on one line; only the new ones are announced in full.
 */
public final class BriefingScene extends Scene {
    private static final double CHARS_PER_SECOND = 55;
    private static final double RULE_INTERVAL = 0.9;

    private final Run run;
    private final ShiftDef shift;
    /** The rules announced one by one: new this shift (story) or all of them (endless). */
    private final List<Rule> rules;
    /** Rules kept from earlier story shifts. */
    private final List<Rule> carried = new ArrayList<>();
    private double t;
    private int typed;
    private int rulesShown;
    private double ruleTimer;
    private double typeSound;

    public BriefingScene(Game game, Run run) {
        super(game);
        this.run = run;
        this.shift = run.def(game.campaign);
        List<Rule> all = shift.rules().ordered();
        if (!shift.endless() && shift.number() > 1) {
            RuleSet before = game.campaign.shift(shift.number() - 1).rules();
            for (Rule r : all) if (before.has(r)) carried.add(r);
        }
        List<Rule> fresh = new ArrayList<>(all);
        fresh.removeAll(carried);
        this.rules = fresh;
    }

    private boolean introDone() {
        return typed >= shift.intro().length();
    }

    private boolean allShown() {
        return introDone() && rulesShown >= rules.size();
    }

    @Override
    public void update(double dt) {
        t += dt;
        if (game.input.pressed(Action.BACK)) {
            game.scenes.set(new MenuScene(game));
            return;
        }
        if (!introDone()) {
            int before = typed;
            typed = Math.min(shift.intro().length(), (int) (t * CHARS_PER_SECOND));
            typeSound -= dt;
            if (typed > before && typeSound <= 0) {
                game.audio.ui("type");
                typeSound = 0.05;
            }
            if (game.input.pressed(Action.CONFIRM)) typed = shift.intro().length();
            return;
        }
        if (rulesShown < rules.size()) {
            ruleTimer -= dt;
            if (ruleTimer <= 0 || game.input.pressed(Action.CONFIRM)) {
                rulesShown++;
                ruleTimer = RULE_INTERVAL;
                game.audio.play("rule");
            }
            return;
        }
        ruleTimer -= dt;
        if (ruleTimer <= 0 && game.input.pressed(Action.CONFIRM)) game.scenes.set(new GameScene(game, run));
    }

    @Override
    public void render(Graphics2D g) {
        int cx = Window.WIDTH / 2;
        Color head = new Color(236, 214, 170);
        String title = shift.endless() ? shift.title() + "   " + Lang.t("brief.seed", run.seedText) : shift.title();
        Text.draw(g, title, cx, 40, 16, head, Text.CENTER);
        int y = 62;
        if (!shift.intro().isEmpty()) {
            y = Text.drawWrapped(g, shift.intro().substring(0, typed), cx, y, 520, 10, new Color(190, 184, 172), Text.CENTER) + 10;
        }
        if (introDone()) {
            if (rules.isEmpty() && carried.isEmpty()) {
                Text.draw(g, Lang.t("brief.norules"), cx, y + 16, 12, new Color(200, 190, 170), Text.CENTER);
            }
            if (!carried.isEmpty()) {
                StringBuilder names = new StringBuilder();
                for (Rule r : carried) names.append(names.isEmpty() ? "" : "  /  ").append(Lang.t("rule." + r.name() + ".short"));
                Text.draw(g, Lang.t("brief.carried"), cx, y + 10, 9, new Color(150, 144, 136), Text.CENTER);
                Text.draw(g, names.toString(), cx, y + 22, 10, new Color(200, 180, 160), Text.CENTER);
                y += 32;
            }
            if (!rules.isEmpty() && !carried.isEmpty()) {
                Text.draw(g, Lang.t("brief.new"), cx, y + 8, 9, new Color(255, 214, 120), Text.CENTER);
                y += 10;
            }
            for (int i = 0; i < rulesShown; i++) {
                Rule r = rules.get(i);
                Color c = r.subject == Rule.Subject.TENANT ? new Color(235, 120, 100) : new Color(120, 190, 240);
                Text.draw(g, r.title(), cx, y + 12, 12, c, Text.CENTER);
                // long hints wrap instead of running off the screen
                y = Text.drawWrapped(g, r.hint(), cx, y + 24, 540, 9, new Color(160, 154, 146), Text.CENTER) - 6;
            }
        }
        if (allShown() && ruleTimer <= 0 && (int) (t * 2) % 2 == 0) {
            Text.draw(g, Lang.t("brief.begin"), cx, Window.HEIGHT - 16, 11, new Color(255, 214, 120), Text.CENTER);
        }
    }
}

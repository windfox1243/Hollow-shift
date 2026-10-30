package hollowshift.story;

import hollowshift.engine.Lang;
import hollowshift.engine.Rng;
import hollowshift.engine.io.Json;
import hollowshift.engine.io.Resources;
import hollowshift.game.Job;
import hollowshift.rules.Rule;
import hollowshift.rules.RuleDeck;
import hollowshift.rules.RuleSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The story shifts and the generator for endless shifts. Layout and difficulty
 * come from /levels.json; labels, intros, tips and the ending come from the
 * current language file, so they follow a language change immediately.
 */
public final class Campaign {
    private final List<Map<String, Object>> shifts = new ArrayList<>();

    public static Campaign load() {
        return parse(Resources.require("/levels.json"));
    }

    public static Campaign parse(String json) {
        Campaign c = new Campaign();
        for (Object o : Json.list(Json.parseObject(json).get("shifts"))) c.shifts.add(Json.obj(o));
        return c;
    }

    public List<ShiftDef> shifts() {
        List<ShiftDef> out = new ArrayList<>();
        for (int n = 1; n <= shifts.size(); n++) out.add(shift(n));
        return out;
    }

    public ShiftDef shift(int number) {
        Map<String, Object> m = shifts.get(number - 1);
        // rules stack: everything from earlier shifts first, then what this shift adds
        RuleSet rules = new RuleSet();
        for (int i = 0; i < number; i++) {
            for (Object r : Json.list(shifts.get(i).get("rules"))) rules.add(Rule.valueOf(Json.str(r, "")));
        }
        return new ShiftDef(number,
                // shifts have no names, only a number (the last one is simply "the final shift")
                Json.bool(m.get("finale"), false) ? Lang.t("shift.final") : Lang.t("shift.label", number),
                Lang.t("shift." + number + ".intro"),
                Json.integer(m.get("cellsW"), 3),
                Json.integer(m.get("cellsH"), 3),
                Json.integer(m.get("rooms"), 5),
                Json.integer(m.get("fuses"), 2),
                Json.num(m.get("loopChance"), 0.15),
                Json.num(m.get("creatureSpeed"), 1.0),
                rules,
                Json.bool(m.get("finale"), false),
                false,
                Json.integer(m.get("jobs"), 0));
    }

    public List<String> tips() {
        return Lang.list("tips");
    }

    public String ending() {
        return Lang.t("ending");
    }

    /** Endless shifts grow in size, jobs, speed and number of rules. */
    public ShiftDef endless(int n, long seed) {
        Rng rng = new Rng(seed * 31 + n);
        int w = Math.min(3 + n / 2, 8);
        int h = Math.min(3 + (n - 1) / 2, 7);
        int rooms = Math.min(4 + n * 2, (int) (w * h * 0.8));
        int fuses = Math.min(3 + n / 2, 8);
        int jobs = Math.min(n / 2, Job.EXTRAS.length);
        double speed = Math.min(1.35, 1 + 0.04 * (n - 1));
        RuleSet rules = RuleDeck.draw(n, rng);
        return new ShiftDef(n, Lang.t("endless.shift", n), "", w, h, rooms, fuses, 0.15, speed, rules, false, true, jobs);
    }
}

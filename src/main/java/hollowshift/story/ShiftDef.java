package hollowshift.story;

import hollowshift.rules.RuleSet;

/**
 * Everything needed to build one shift (level). The lift runs once every fuse box has power and
 * {@code jobs} more kinds of work, drawn per run (see {@code game.Job}), are done. In the final
 * shift {@code fuses} is the number of main breakers.
 */
public record ShiftDef(
        int number,
        String title,
        String intro,
        int cellsW,
        int cellsH,
        int rooms,
        int fuses,
        double loopChance,
        double creatureSpeed,
        RuleSet rules,
        boolean finale,
        boolean endless,
        int jobs) {

    /** A shift whose only job is the fuses. */
    public ShiftDef(int number, String title, String intro, int cellsW, int cellsH, int rooms, int fuses,
                    double loopChance, double creatureSpeed, RuleSet rules, boolean finale, boolean endless) {
        this(number, title, intro, cellsW, cellsH, rooms, fuses, loopChance, creatureSpeed, rules, finale, endless, 0);
    }
}

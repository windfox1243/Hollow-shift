package hollowshift.story;

import hollowshift.rules.RuleSet;

/**
 * Everything needed to build one shift (level). The lift runs once every fuse box has power,
 * {@code valves} valves are bled and, if {@code liftKey}, the gate key is found. In the final
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
        int valves,
        boolean liftKey) {

    /** A shift whose only job is the fuses. */
    public ShiftDef(int number, String title, String intro, int cellsW, int cellsH, int rooms, int fuses,
                    double loopChance, double creatureSpeed, RuleSet rules, boolean finale, boolean endless) {
        this(number, title, intro, cellsW, cellsH, rooms, fuses, loopChance, creatureSpeed, rules, finale, endless, 0, false);
    }
}

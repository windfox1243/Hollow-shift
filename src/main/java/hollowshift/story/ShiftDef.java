package hollowshift.story;

import hollowshift.rules.RuleSet;

/** Everything needed to build one shift (level). */
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
        boolean endless) {
}

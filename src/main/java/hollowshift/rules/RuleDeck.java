package hollowshift.rules;

import hollowshift.engine.Rng;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws a random rule set for endless mode. Difficulty grows with the shift number:
 * shift 1 has no rules, from shift 2 there is always at least one sense,
 * ECHO appears from shift 3 and HASTE from shift 4.
 */
public final class RuleDeck {
    private RuleDeck() {
    }

    public static int ruleCount(int shift) {
        return Math.min(6, (shift + 1) / 2 + (shift >= 6 ? 1 : 0));
    }

    public static RuleSet draw(int shift, Rng rng) {
        RuleSet rules = new RuleSet();
        int count = ruleCount(shift);
        if (shift <= 1 || count == 0) return rules;

        List<Rule> senses = new ArrayList<>(List.of(Rule.SIGHT, Rule.HEARING, Rule.SMELL, Rule.TREMOR));
        rules.add(rng.pick(senses));

        List<Rule> pool = new ArrayList<>(List.of(Rule.values()));
        rng.shuffle(pool);
        for (Rule r : pool) {
            if (rules.size() >= count) break;
            if (r == Rule.HASTE && shift < 4) continue;
            if (r == Rule.ECHO && shift < 3) continue;
            // at most two survival rules, otherwise early shifts become a resource grind
            long you = rules.ordered().stream().filter(x -> x.subject == Rule.Subject.YOU).count();
            if (r.subject == Rule.Subject.YOU && you >= 2) continue;
            rules.add(r);
        }
        return rules;
    }
}

package hollowshift.rules;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** The active rules of one shift, in the order they are announced. */
public final class RuleSet {
    private final List<Rule> ordered = new ArrayList<>();
    private final Set<Rule> set = EnumSet.noneOf(Rule.class);

    public static RuleSet of(Rule... rules) {
        RuleSet rs = new RuleSet();
        for (Rule r : rules) rs.add(r);
        return rs;
    }

    public boolean add(Rule r) {
        if (!set.add(r)) return false;
        ordered.add(r);
        return true;
    }

    public boolean has(Rule r) {
        return set.contains(r);
    }

    public List<Rule> ordered() {
        return List.copyOf(ordered);
    }

    public int size() {
        return ordered.size();
    }
}

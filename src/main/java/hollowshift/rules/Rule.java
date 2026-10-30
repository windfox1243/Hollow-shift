package hollowshift.rules;

import hollowshift.engine.Lang;

/** The rules announced at the start of a shift. */
public enum Rule {
    SIGHT(Subject.TENANT),
    HEARING(Subject.TENANT),
    SMELL(Subject.TENANT),
    CONSUME(Subject.TENANT),
    ECHO(Subject.TENANT),
    HASTE(Subject.TENANT),
    BATTERY(Subject.YOU),
    COLD(Subject.YOU),
    HUNGER(Subject.YOU);

    public enum Subject { TENANT, YOU }

    public final Subject subject;

    Rule(Subject subject) {
        this.subject = subject;
    }

    public String title() {
        return Lang.t("rule." + name() + ".title");
    }

    public String hint() {
        return Lang.t("rule." + name() + ".hint");
    }

    /** Rules that let the Tenant locate the player. */
    public boolean isSense() {
        return this == SIGHT || this == HEARING || this == SMELL;
    }
}

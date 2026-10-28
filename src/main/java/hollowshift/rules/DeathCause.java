package hollowshift.rules;

import hollowshift.engine.Lang;

/** Why a run ended, with the text shown on the death screen. */
public enum DeathCause {
    TENANT, COLD, HUNGER;

    public String title() {
        return Lang.t("death." + name() + ".title");
    }

    public String text() {
        return Lang.t("death." + name() + ".text");
    }
}

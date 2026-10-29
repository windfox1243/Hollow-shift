package hollowshift.engine;

import java.awt.event.KeyEvent;

/** Logical input actions; each can be bound to up to two keys (rebindable in Settings). */
public enum Action {
    UP(KeyEvent.VK_W, KeyEvent.VK_UP),
    DOWN(KeyEvent.VK_S, KeyEvent.VK_DOWN),
    LEFT(KeyEvent.VK_A, KeyEvent.VK_LEFT),
    RIGHT(KeyEvent.VK_D, KeyEvent.VK_RIGHT),
    RUN(KeyEvent.VK_SHIFT, 0),
    INTERACT(KeyEvent.VK_E, KeyEvent.VK_SPACE),
    THROW(KeyEvent.VK_Q, 0),
    LANTERN(KeyEvent.VK_F, 0),
    DROP(KeyEvent.VK_G, 0),
    NEXT_ITEM(KeyEvent.VK_TAB, KeyEvent.VK_R),
    JOURNAL(KeyEvent.VK_J, 0),
    CONFIRM(KeyEvent.VK_ENTER, KeyEvent.VK_SPACE),
    BACK(KeyEvent.VK_ESCAPE, KeyEvent.VK_BACK_SPACE);

    public final int defaultPrimary;
    public final int defaultSecondary;

    Action(int primary, int secondary) {
        this.defaultPrimary = primary;
        this.defaultSecondary = secondary;
    }

    public String label() {
        return Lang.t("action." + name());
    }

    /** Actions shown in the key rebinding menu. */
    public boolean rebindable() {
        return this != CONFIRM && this != BACK;
    }
}

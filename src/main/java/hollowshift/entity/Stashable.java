package hollowshift.entity;

/** An item that goes into a pocket of the {@link Pack} when picked up, instead of into the hands. */
public interface Stashable {
    Supply supply();

    /** Prompt shared by every stashable item: pick it up, or say the pocket is full. */
    static String prompt(Player p, Supply s) {
        return hollowshift.engine.Lang.t(p.pack().hasRoom(s) ? "prompt.pickup" : "prompt.pocketFull", s.displayName());
    }
}

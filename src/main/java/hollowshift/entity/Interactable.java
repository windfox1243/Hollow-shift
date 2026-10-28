package hollowshift.entity;

/** Something the player can use with the Interact key. */
public interface Interactable {
    /** Short prompt shown above the object, e.g. "Pick up fuse"; null hides the prompt. */
    String prompt(Player p);

    boolean canInteract(Player p);

    void interact(Player p);

    /** Seconds the key must be held; 0 for instant. */
    default double holdTime() {
        return 0;
    }

    default double interactRange() {
        return 18;
    }
}

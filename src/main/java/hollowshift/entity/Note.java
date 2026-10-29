package hollowshift.entity;

import hollowshift.engine.Lang;

/** A scrap of paper left behind by someone who was here before. Picking it up reads it and keeps it in the journal. */
public final class Note extends Fixture implements Interactable {
    private final int number;
    private boolean read;

    public Note(double x, double y, int number) {
        super(x, y);
        this.number = number;
    }

    @Override
    protected String sprite() {
        return read ? "note_1" : "note_0";
    }

    @Override
    public String prompt(Player p) {
        return Lang.t("prompt.note");
    }

    @Override
    public boolean canInteract(Player p) {
        return true;
    }

    @Override
    public void interact(Player p) {
        read = true;
        world.audio.play("grab", hollowshift.engine.audio.Bus.SFX, 0.7, 1.3);
        if (world.settings().notesRead.add(number)) world.settings().save();
        world.readNote(number, Lang.t("note." + number));
        remove();
    }

    @Override
    public double interactRange() {
        return 16;
    }
}

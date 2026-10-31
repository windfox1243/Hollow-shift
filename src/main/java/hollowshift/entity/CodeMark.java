package hollowshift.entity;

import hollowshift.engine.Lang;

import java.awt.Graphics2D;

/**
 * One digit of the phone number, stencilled black on a band of safety yellow with its place
 * shown ("DIAL ? 7 ?"). Like any writing down here you only make it out with a light on it; once read,
 * the miner remembers it and the keypad shows it.
 */
public final class CodeMark extends Entity {
    /** How close, and how lit, the wall has to be for the digit to be read. */
    public static final double READ_RANGE = 64;
    public static final double READ_LIGHT = 0.12;
    private final int place;
    private final char digit;
    private final String text;
    private final RoomSign plate;

    public CodeMark(double x, double y, int place, char digit, int length) {
        super(x, y);
        this.place = place;
        this.digit = digit;
        this.text = label(place, digit, length);
        // painted like the shaft's own sign, so it reads as the mine's business
        this.plate = new RoomSign(x, y, text, RoomSign.Damage.NONE, true);
    }

    /** What gets written on the wall for this digit, e.g. "DIAL ? 7 ?". */
    public static String label(int place, char digit, int length) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < length; i++) b.append(i == 0 ? "" : " ").append(i == place ? digit : '?');
        return Lang.t("code.mark", b.toString());
    }

    public String text() {
        return text;
    }

    @Override
    public double sortY() {
        return y - 16;
    }

    @Override
    public void update(double dt) {
        Player p = world.player;
        if (p.distanceTo(x, y) > READ_RANGE || world.visibilityAt(x, y - 12, true) < READ_LIGHT) return;
        if (world.learnDigit(place, digit)) {
            world.audio.play("type", hollowshift.engine.audio.Bus.SFX, 0.6, 0.8);
            world.toast(Lang.t("toast.digitRead", world.codeMemo()));
        }
    }

    @Override
    public void render(Graphics2D g, int ox, int oy) {
        plate.render(g, ox, oy);
    }
}

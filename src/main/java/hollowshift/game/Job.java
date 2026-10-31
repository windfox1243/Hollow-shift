package hollowshift.game;

/** The kinds of work the lift needs before it will run. */
public enum Job {
    /** Power a fuse box (always). */
    FUSE,
    /** Bleed a pressure valve on the lift's hydraulics. */
    VALVE,
    /** Find the key to the lift gate. */
    KEY,
    /** Carry a can of fuel to the generator and pour it in. */
    FUEL,
    /** Dial the surface on the old wall phone, with the number chalked on the walls. Everything down here hears it ring. */
    PHONE,
    /** Rewire a junction box on the lift's signal line. */
    WIRES;

    /** The jobs a shift can draw on top of the fuses. */
    public static final Job[] EXTRAS = {VALVE, KEY, FUEL, PHONE, WIRES};
}

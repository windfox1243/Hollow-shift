package hollowshift.engine.audio;

/** A playing instance of a {@link Sound}. Gain/pan/pitch may be changed while it plays. */
public final class Voice {
    final Sound sound;
    final Bus bus;
    final boolean loop;
    double position;
    volatile float gain;
    volatile float pan; // -1 left .. 1 right
    volatile float pitch;
    volatile boolean stopped;

    Voice(Sound sound, Bus bus, float gain, float pan, float pitch, boolean loop) {
        this.sound = sound;
        this.bus = bus;
        this.gain = gain;
        this.pan = pan;
        this.pitch = pitch;
        this.loop = loop;
    }

    public void setGain(double g) {
        gain = (float) g;
    }

    public void setPan(double p) {
        pan = (float) Math.max(-1, Math.min(1, p));
    }

    public void setPitch(double p) {
        pitch = (float) p;
    }

    public void stop() {
        stopped = true;
    }

    public boolean isDone() {
        return stopped;
    }
}

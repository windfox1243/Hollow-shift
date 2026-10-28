package hollowshift.engine.audio;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Sound facade. A sound named "x" is loaded from /assets/sfx/x.wav when that
 * resource exists (e.g. from a CC0 pack); otherwise it is synthesised.
 * Optional variants x_2.wav ... x_9.wav are picked at random on each play.
 */
public final class Audio {
    private final Mixer mixer = new Mixer();
    private final Synth synth = new Synth();
    private final Map<String, List<Sound>> cache = new HashMap<>();
    private final Random variantPick = new Random();
    private double listenerX;
    private double listenerY;

    public Sound get(String name) {
        List<Sound> variants = cache.computeIfAbsent(name, this::load);
        return variants.size() == 1 ? variants.get(0) : variants.get(variantPick.nextInt(variants.size()));
    }

    private List<Sound> load(String name) {
        List<Sound> out = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            String file = i == 1 ? name : name + "_" + i;
            InputStream in = Audio.class.getResourceAsStream("/assets/sfx/" + file + ".wav");
            if (in == null) break;
            try (in) {
                out.add(Sound.load(file, in));
            } catch (Exception e) {
                System.err.println("could not decode " + file + ".wav: " + e.getMessage());
            }
        }
        if (out.isEmpty()) out.add(synth.make(name));
        return out;
    }

    /** Builds every sound up front so the first play has no hitch. */
    public void preload(String... names) {
        for (String n : names) get(n);
    }

    public Voice play(String name) {
        return play(name, Bus.SFX, 1, 1);
    }

    public Voice ui(String name) {
        return play(name, Bus.UI, 0.8, 1);
    }

    public Voice play(String name, Bus bus, double gain, double pitch) {
        return mixer.play(get(name), bus, gain, 0, pitch, false);
    }

    public Voice loop(String name, Bus bus, double gain) {
        return mixer.play(get(name), bus, gain, 0, 1, true);
    }

    public void setListener(double x, double y) {
        listenerX = x;
        listenerY = y;
    }

    /** Positional one-shot: volume falls off linearly to zero at {@code range} pixels, panned by x offset. */
    public Voice playAt(String name, double x, double y, double range, double gain, double pitch) {
        double g = spatialGain(x, y, range) * gain;
        if (g <= 0.01) return null;
        return mixer.play(get(name), Bus.SFX, g, spatialPan(x), pitch, false);
    }

    public double spatialGain(double x, double y, double range) {
        double d = Math.hypot(x - listenerX, y - listenerY);
        double t = 1 - d / range;
        return t <= 0 ? 0 : t * t;
    }

    public double spatialPan(double x) {
        return Math.max(-1, Math.min(1, (x - listenerX) / 220));
    }

    public void setVolumes(double master, double music, double sfx) {
        mixer.setMaster(master);
        mixer.setBusVolume(Bus.MUSIC, music);
        mixer.setBusVolume(Bus.SFX, sfx);
        mixer.setBusVolume(Bus.UI, sfx);
    }

    public void stopAll() {
        mixer.stopAll();
    }

    public void stopBus(Bus b) {
        mixer.stopBus(b);
    }

    public void shutdown() {
        mixer.shutdown();
    }
}

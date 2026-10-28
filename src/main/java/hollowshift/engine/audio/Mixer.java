package hollowshift.engine.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Software mixer: a daemon thread sums all active voices into one stereo
 * SourceDataLine. Doing the mixing ourselves gives per-voice gain/pan/pitch
 * and seamless loops with nothing but javax.sound.
 */
public final class Mixer implements Runnable {
    public static final int RATE = 44100;
    private static final int FRAMES = 512;

    private final CopyOnWriteArrayList<Voice> voices = new CopyOnWriteArrayList<>();
    private final Map<Bus, Float> busVolume = new EnumMap<>(Bus.class);
    private volatile float master = 0.8f;
    private volatile boolean running = true;
    private SourceDataLine line;

    public Mixer() {
        for (Bus b : Bus.values()) busVolume.put(b, 1f);
        try {
            AudioFormat fmt = new AudioFormat(RATE, 16, 2, true, false);
            line = AudioSystem.getSourceDataLine(fmt);
            line.open(fmt, FRAMES * 4 * 4);
            line.start();
            Thread t = new Thread(this, "audio-mixer");
            t.setDaemon(true);
            t.start();
        } catch (Exception | LinkageError e) {
            System.err.println("audio disabled: " + e.getMessage());
            line = null;
        }
    }

    public boolean enabled() {
        return line != null;
    }

    public Voice play(Sound s, Bus bus, double gain, double pan, double pitch, boolean loop) {
        Voice v = new Voice(s, bus, (float) gain, (float) pan, (float) pitch, loop);
        if (line != null && s.samples.length > 0) voices.add(v);
        else v.stopped = true;
        return v;
    }

    public void setMaster(double v) {
        master = (float) v;
    }

    public void setBusVolume(Bus b, double v) {
        busVolume.put(b, (float) v);
    }

    public void stopAll() {
        for (Voice v : voices) v.stopped = true;
    }

    public void stopBus(Bus b) {
        for (Voice v : voices) if (v.bus == b) v.stopped = true;
    }

    public void shutdown() {
        running = false;
    }

    @Override
    public void run() {
        float[] mixL = new float[FRAMES];
        float[] mixR = new float[FRAMES];
        byte[] out = new byte[FRAMES * 4];
        while (running) {
            java.util.Arrays.fill(mixL, 0);
            java.util.Arrays.fill(mixR, 0);
            for (Voice v : voices) {
                if (v.stopped) {
                    voices.remove(v);
                    continue;
                }
                mixVoice(v, mixL, mixR);
            }
            float m = master;
            for (int i = 0; i < FRAMES; i++) {
                int l = (int) (softClip(mixL[i] * m) * 32767);
                int r = (int) (softClip(mixR[i] * m) * 32767);
                out[i * 4] = (byte) l;
                out[i * 4 + 1] = (byte) (l >> 8);
                out[i * 4 + 2] = (byte) r;
                out[i * 4 + 3] = (byte) (r >> 8);
            }
            line.write(out, 0, out.length);
        }
        line.drain();
        line.close();
    }

    private void mixVoice(Voice v, float[] mixL, float[] mixR) {
        float[] data = v.sound.samples;
        float g = v.gain * busVolume.get(v.bus);
        float pan = v.pan;
        float gl = g * (float) Math.sqrt(0.5 * (1 - pan));
        float gr = g * (float) Math.sqrt(0.5 * (1 + pan));
        double step = Math.max(0.05, v.pitch);
        double pos = v.position;
        int n = data.length;
        for (int i = 0; i < FRAMES; i++) {
            if (pos >= n) {
                if (!v.loop) {
                    v.stopped = true;
                    break;
                }
                pos -= n;
            }
            int i0 = (int) pos;
            int i1 = i0 + 1 < n ? i0 + 1 : (v.loop ? 0 : i0);
            float t = (float) (pos - i0);
            float s = data[i0] + (data[i1] - data[i0]) * t;
            mixL[i] += s * gl;
            mixR[i] += s * gr;
            pos += step;
        }
        v.position = pos;
    }

    private static float softClip(float x) {
        if (x > 1) return 1;
        if (x < -1) return -1;
        return x * (1.5f - 0.5f * x * x);
    }
}

package hollowshift.engine.audio;

import java.util.Random;

/**
 * Procedural sound effects. Used whenever no WAV override exists in
 * resources/assets/sfx, so the game is fully playable without external audio.
 */
public final class Synth {
    private static final int R = Mixer.RATE;
    private final Random rnd = new Random(1234);

    public Sound make(String name) {
        float[] s = switch (name) {
            case "step" -> step(0.06, 900);
            case "step_soft" -> step(0.05, 400);
            case "creature_step" -> thump(0.35, 55, 0.9);
            case "creature_step_heavy" -> thump(0.45, 42, 1.0);
            case "sniff" -> sniff();
            case "grab" -> click(0.05, 2400);
            case "drop" -> thump(0.12, 160, 0.5);
            case "throw" -> whoosh(0.25, 0.4);
            case "stone_land" -> mix(thump(0.25, 90, 0.8), click(0.08, 1800));
            case "fuse_insert" -> zap();
            case "power_on" -> sweep(0.9, 110, 440, 0.35);
            case "surge" -> siren();
            case "eat" -> crunch();
            case "flare" -> hiss(1.2);
            case "lantern" -> click(0.04, 1200);
            case "elevator" -> hum(3.0, 60);
            case "elevator_bell" -> bell(1.5, 660);
            case "echo" -> reverseSwell();
            case "stun" -> mix(thump(0.3, 70, 1), bell(0.4, 220));
            case "death" -> deathSting();
            case "heartbeat" -> heartbeat();
            case "knock" -> knock();
            case "drone" -> drone(8.0);
            case "menu_drone" -> menuDrone(10.0);
            case "ui_move" -> blip(0.05, 520);
            case "ui_select" -> blip(0.09, 780);
            case "ui_back" -> blip(0.08, 330);
            case "type" -> click(0.02, 3000);
            case "rule" -> mix(thump(0.4, 50, 0.7), whoosh(0.4, 0.25));
            default -> new float[R / 20];
        };
        return new Sound(name, s);
    }

    // ---- building blocks ------------------------------------------------------

    private float[] buf(double seconds) {
        return new float[(int) (seconds * R)];
    }

    private static double env(double t, double attack, double decay) {
        return t < attack ? t / attack : Math.exp(-(t - attack) / decay);
    }

    private float noise() {
        return rnd.nextFloat() * 2 - 1;
    }

    private float[] lowpass(float[] s, double cutoff) {
        double rc = 1.0 / (2 * Math.PI * cutoff);
        double a = (1.0 / R) / (rc + 1.0 / R);
        float y = 0;
        for (int i = 0; i < s.length; i++) {
            y += (float) (a * (s[i] - y));
            s[i] = y;
        }
        return s;
    }

    private static float[] mix(float[] a, float[] b) {
        float[] out = new float[Math.max(a.length, b.length)];
        for (int i = 0; i < out.length; i++) out[i] = (i < a.length ? a[i] : 0) + (i < b.length ? b[i] : 0);
        return out;
    }

    private static float[] gain(float[] s, double g) {
        for (int i = 0; i < s.length; i++) s[i] *= (float) g;
        return s;
    }

    // ---- effects ----------------------------------------------------------------

    private float[] step(double len, double cutoff) {
        float[] s = buf(len);
        for (int i = 0; i < s.length; i++) s[i] = noise() * (float) env(i / (double) R, 0.002, len / 4);
        return gain(lowpass(s, cutoff), 0.6);
    }

    private float[] thump(double len, double freq, double amp) {
        float[] s = buf(len);
        double phase = 0;
        for (int i = 0; i < s.length; i++) {
            double t = i / (double) R;
            double f = freq * (1 + 1.5 * Math.exp(-t * 30));
            phase += 2 * Math.PI * f / R;
            s[i] = (float) ((Math.sin(phase) * 0.9 + noise() * 0.15 * Math.exp(-t * 40)) * env(t, 0.004, len / 4) * amp);
        }
        return s;
    }

    private float[] click(double len, double freq) {
        float[] s = buf(len);
        for (int i = 0; i < s.length; i++) {
            double t = i / (double) R;
            s[i] = (float) ((Math.sin(2 * Math.PI * freq * t) * 0.5 + noise() * 0.5) * env(t, 0.001, len / 5) * 0.5);
        }
        return s;
    }

    private float[] blip(double len, double freq) {
        float[] s = buf(len);
        for (int i = 0; i < s.length; i++) {
            double t = i / (double) R;
            s[i] = (float) (Math.signum(Math.sin(2 * Math.PI * freq * t)) * 0.18 * env(t, 0.002, len / 3));
        }
        return lowpass(s, 3000);
    }

    private float[] whoosh(double len, double amp) {
        float[] s = buf(len);
        for (int i = 0; i < s.length; i++) {
            double t = i / len / R;
            s[i] = (float) (noise() * Math.sin(Math.PI * t) * amp);
        }
        return lowpass(s, 1200);
    }

    private float[] sniff() {
        float[] s = buf(0.9);
        for (int k = 0; k < 3; k++) {
            int start = (int) ((0.05 + k * 0.22) * R);
            int len = (int) (0.15 * R);
            for (int i = 0; i < len && start + i < s.length; i++) {
                double t = i / (double) len;
                s[start + i] += (float) (noise() * Math.sin(Math.PI * t) * 0.7);
            }
        }
        return lowpass(lowpass(s, 1800), 1400);
    }

    private float[] zap() {
        float[] s = buf(0.35);
        for (int i = 0; i < s.length; i++) {
            double t = i / (double) R;
            double sq = Math.signum(Math.sin(2 * Math.PI * (120 + 40 * Math.sin(t * 90)) * t));
            s[i] = (float) ((sq * 0.3 + noise() * 0.3) * env(t, 0.003, 0.08));
        }
        return lowpass(s, 2500);
    }

    private float[] sweep(double len, double f0, double f1, double amp) {
        float[] s = buf(len);
        double phase = 0;
        for (int i = 0; i < s.length; i++) {
            double t = i / (double) R;
            double f = f0 + (f1 - f0) * (t / len);
            phase += 2 * Math.PI * f / R;
            s[i] = (float) ((Math.sin(phase) + 0.3 * Math.sin(phase * 2)) * amp * Math.sin(Math.PI * t / len));
        }
        return s;
    }

    private float[] siren() {
        float[] s = buf(2.4);
        double phase = 0;
        for (int i = 0; i < s.length; i++) {
            double t = i / (double) R;
            double f = 380 + 160 * Math.sin(2 * Math.PI * 1.25 * t);
            phase += 2 * Math.PI * f / R;
            double saw = 2 * ((phase / (2 * Math.PI)) % 1.0) - 1;
            s[i] = (float) (saw * 0.22 * Math.min(1, t * 8) * Math.min(1, (2.4 - t) * 3));
        }
        return lowpass(s, 2200);
    }

    private float[] crunch() {
        float[] s = buf(0.4);
        for (int k = 0; k < 4; k++) {
            int start = (int) (k * 0.09 * R);
            for (int i = 0; i < 0.06 * R && start + i < s.length; i++) {
                double t = i / (double) R;
                s[start + i] += (float) (noise() * env(t, 0.001, 0.015) * 0.6);
            }
        }
        return lowpass(s, 3500);
    }

    private float[] hiss(double len) {
        float[] s = buf(len);
        for (int i = 0; i < s.length; i++) {
            double t = i / (double) R;
            s[i] = (float) (noise() * 0.35 * Math.min(1, t * 20) * Math.min(1, (len - t) * 2));
        }
        return s;
    }

    private float[] hum(double len, double freq) {
        float[] s = buf(len);
        for (int i = 0; i < s.length; i++) {
            double t = i / (double) R;
            s[i] = (float) ((Math.sin(2 * Math.PI * freq * t) * 0.5 + Math.sin(2 * Math.PI * freq * 2.01 * t) * 0.25
                    + noise() * 0.05) * 0.4);
        }
        return s;
    }

    private float[] bell(double len, double freq) {
        float[] s = buf(len);
        for (int i = 0; i < s.length; i++) {
            double t = i / (double) R;
            double e = Math.exp(-t * 3);
            s[i] = (float) ((Math.sin(2 * Math.PI * freq * t) + 0.5 * Math.sin(2 * Math.PI * freq * 2.76 * t)
                    + 0.25 * Math.sin(2 * Math.PI * freq * 5.4 * t)) * e * 0.25);
        }
        return s;
    }

    private float[] reverseSwell() {
        float[] s = whoosh(1.0, 0.6);
        for (int i = 0; i < s.length; i++) s[i] *= (float) Math.pow(i / (double) s.length, 2) * 1.5f;
        return s;
    }

    private float[] deathSting() {
        float[] s = buf(2.2);
        double phase = 0;
        for (int i = 0; i < s.length; i++) {
            double t = i / (double) R;
            double f = 220 * Math.exp(-t * 1.2) + 30;
            phase += 2 * Math.PI * f / R;
            s[i] = (float) ((Math.sin(phase) * 0.5 + noise() * 0.45) * env(t, 0.01, 0.7));
        }
        return lowpass(s, 1500);
    }

    private float[] knock() {
        float[] s = buf(1.3);
        for (int k = 0; k < 3; k++) {
            float[] hit = mix(thump(0.14, 150, 0.9), click(0.03, 900));
            int off = (int) (k * 0.38 * R);
            for (int i = 0; i < hit.length && off + i < s.length; i++) s[off + i] += hit[i];
        }
        return lowpass(s, 1800);
    }

    private float[] heartbeat() {
        float[] s = buf(1.0);
        float[] a = thump(0.18, 48, 1);
        float[] b = thump(0.18, 44, 0.7);
        for (int i = 0; i < a.length; i++) s[i] += a[i];
        int off = (int) (0.22 * R);
        for (int i = 0; i < b.length && off + i < s.length; i++) s[off + i] += b[i];
        return s;
    }

    /** Seamless loop: all oscillators complete whole cycles over the loop length. */
    private float[] drone(double len) {
        float[] s = buf(len);
        double[] freqs = {41.0, 55, 61.75, 82.5};
        float[] n = lowpass(lowpassNoise(s.length), 200);
        for (int i = 0; i < s.length; i++) {
            double t = i / (double) R;
            double v = 0;
            for (int k = 0; k < freqs.length; k++) {
                double f = Math.round(freqs[k] * len) / len;
                v += Math.sin(2 * Math.PI * f * t) * (0.22 / (k + 1)) * (0.6 + 0.4 * Math.sin(2 * Math.PI * t / len * (k + 1)));
            }
            s[i] = (float) (v + n[i] * 0.6);
        }
        return crossfadeLoop(s, (int) (0.5 * R));
    }

    private float[] menuDrone(double len) {
        float[] s = drone(len);
        double loopLen = s.length / (double) R;
        for (int i = 0; i < s.length; i++) {
            double t = i / (double) R;
            double f = Math.round(220 * loopLen) / loopLen;
            s[i] += (float) (Math.sin(2 * Math.PI * f * t) * 0.03 * (0.5 + 0.5 * Math.sin(2 * Math.PI * t / loopLen)));
        }
        return s;
    }

    private float[] lowpassNoise(int n) {
        float[] s = new float[n];
        for (int i = 0; i < n; i++) s[i] = noise();
        return s;
    }

    /** Blends the tail into the head and drops the tail, so the buffer loops without a click. */
    private static float[] crossfadeLoop(float[] s, int fade) {
        for (int i = 0; i < fade; i++) {
            double t = i / (double) fade;
            int end = s.length - fade + i;
            s[i] = (float) (s[i] * t + s[end] * (1 - t));
        }
        return java.util.Arrays.copyOf(s, s.length - fade);
    }
}

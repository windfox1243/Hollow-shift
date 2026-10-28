package hollowshift.engine.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.BufferedInputStream;
import java.io.InputStream;

/** Mono PCM samples in [-1, 1] at {@link Mixer#RATE}. */
public final class Sound {
    public final String name;
    public final float[] samples;

    public Sound(String name, float[] samples) {
        this.name = name;
        this.samples = samples;
    }

    /** Decodes a WAV (or any format javax.sound can read) and converts it to mono at the mixer rate. */
    public static Sound load(String name, InputStream in) throws Exception {
        try (AudioInputStream src = AudioSystem.getAudioInputStream(new BufferedInputStream(in))) {
            AudioFormat f = src.getFormat();
            AudioFormat pcm = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, f.getSampleRate(), 16,
                    f.getChannels(), f.getChannels() * 2, f.getSampleRate(), false);
            try (AudioInputStream s = AudioSystem.getAudioInputStream(pcm, src)) {
                byte[] bytes = s.readAllBytes();
                int ch = pcm.getChannels();
                int frames = bytes.length / (2 * ch);
                float[] mono = new float[frames];
                for (int i = 0; i < frames; i++) {
                    float sum = 0;
                    for (int c = 0; c < ch; c++) {
                        int idx = (i * ch + c) * 2;
                        sum += (short) ((bytes[idx] & 0xff) | (bytes[idx + 1] << 8)) / 32768f;
                    }
                    mono[i] = sum / ch;
                }
                return new Sound(name, resample(mono, pcm.getSampleRate(), Mixer.RATE));
            }
        }
    }

    private static float[] resample(float[] in, float fromRate, float toRate) {
        if (Math.abs(fromRate - toRate) < 1) return in;
        double ratio = fromRate / toRate;
        float[] out = new float[(int) (in.length / ratio)];
        for (int i = 0; i < out.length; i++) {
            double p = i * ratio;
            int i0 = (int) p;
            int i1 = Math.min(i0 + 1, in.length - 1);
            double t = p - i0;
            out[i] = (float) (in[i0] * (1 - t) + in[i1] * t);
        }
        return out;
    }
}

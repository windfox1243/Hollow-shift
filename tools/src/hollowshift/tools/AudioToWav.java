package hollowshift.tools;

import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Build-time tool: converts the OGG and MP3 recordings listed in third_party/audio-map.txt
 * into 16-bit mono WAV files under src/main/resources/assets/sfx. The decoders (JOrbis for
 * OGG, JLayer for MP3; both pure Java, LGPL) are only needed here; the game itself reads
 * plain WAV with javax.sound.
 */
public class AudioToWav {
    public static void main(String[] args) throws Exception {
        Path root = Path.of("third_party");
        Path out = Path.of("src/main/resources/assets/sfx");
        Files.createDirectories(out);
        int n = 0;
        for (String line : Files.readAllLines(root.resolve("audio-map.txt"), StandardCharsets.UTF_8)) {
            line = line.strip();
            if (line.isEmpty() || line.startsWith(";")) continue;
            String[] kv = line.split("=", 2);
            String name = kv[0].strip();
            Path src = root.resolve(kv[1].strip());
            try (InputStream in = new java.io.BufferedInputStream(Files.newInputStream(src))) {
                Decoded d = src.toString().toLowerCase().endsWith(".mp3") ? decodeMp3(in) : decode(in);
                byte[] mono = toMono16(d);
                AudioFormat fmt = new AudioFormat(d.rate, 16, 1, true, false);
                try (AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(mono), fmt, mono.length / 2)) {
                    AudioSystem.write(ais, AudioFileFormat.Type.WAVE, out.resolve(name + ".wav").toFile());
                }
                n++;
                System.out.printf("  %-12s <- %s (%.2fs)%n", name, src.getFileName(), mono.length / 2.0 / d.rate);
            }
        }
        System.out.println("converted " + n + " sounds -> " + out);
    }

    /** JLayer frame loop: each MPEG frame decodes to interleaved 16-bit samples. */
    private static Decoded decodeMp3(InputStream in) throws Exception {
        javazoom.jl.decoder.Bitstream bitstream = new javazoom.jl.decoder.Bitstream(in);
        javazoom.jl.decoder.Decoder decoder = new javazoom.jl.decoder.Decoder();
        FloatList samples = new FloatList();
        int rate = 44100, channels = 2;
        javazoom.jl.decoder.Header h;
        while ((h = bitstream.readFrame()) != null) {
            javazoom.jl.decoder.SampleBuffer buf = (javazoom.jl.decoder.SampleBuffer) decoder.decodeFrame(h, bitstream);
            rate = buf.getSampleFrequency();
            channels = buf.getChannelCount();
            short[] pcm = buf.getBuffer();
            for (int i = 0; i < buf.getBufferLength(); i++) samples.add(pcm[i] / 32768f);
            bitstream.closeFrame();
        }
        bitstream.close();
        return new Decoded(rate, channels, samples.toArray());
    }

    private record Decoded(int rate, int channels, float[] interleaved) {
    }

    private static byte[] toMono16(Decoded d) {
        int frames = d.interleaved.length / d.channels;
        byte[] b = new byte[frames * 2];
        for (int i = 0; i < frames; i++) {
            float s = 0;
            for (int c = 0; c < d.channels; c++) s += d.interleaved[i * d.channels + c];
            s /= d.channels;
            int v = (int) Math.max(-32768, Math.min(32767, s * 32767));
            b[i * 2] = (byte) v;
            b[i * 2 + 1] = (byte) (v >> 8);
        }
        return b;
    }

    /** Standard JOrbis decode loop: Ogg pages -> packets -> Vorbis PCM. */
    private static Decoded decode(InputStream in) throws IOException {
        SyncState oy = new SyncState();
        StreamState os = new StreamState();
        Page og = new Page();
        Packet op = new Packet();
        Info vi = new Info();
        Comment vc = new Comment();
        DspState vd = new DspState();
        Block vb = new Block(vd);
        oy.init();

        final int chunk = 4096;
        // headers
        int headers = 0;
        boolean streamInit = false;
        while (headers < 3) {
            int r = oy.pageout(og);
            if (r == 0) {
                int idx = oy.buffer(chunk);
                int read = in.read(oy.data, idx, chunk);
                if (read <= 0) throw new IOException("truncated ogg header");
                oy.wrote(read);
                continue;
            }
            if (r < 0) continue;
            if (!streamInit) {
                os.init(og.serialno());
                vi.init();
                vc.init();
                streamInit = true;
            }
            os.pagein(og);
            while (headers < 3 && os.packetout(op) == 1) {
                if (vi.synthesis_headerin(vc, op) < 0) throw new IOException("not a vorbis stream");
                headers++;
            }
        }
        vd.synthesis_init(vi);
        vb.init(vd);

        int channels = vi.channels;
        float[][][] pcmHolder = new float[1][][];
        int[] index = new int[channels];
        FloatList samples = new FloatList();
        boolean eos = false;
        while (!eos) {
            drain(os, op, vb, vd, pcmHolder, index, channels, samples);
            int r = oy.pageout(og);
            if (r > 0) {
                os.pagein(og);
                if (og.eos() != 0) {
                    drain(os, op, vb, vd, pcmHolder, index, channels, samples);
                    eos = true;
                }
            } else if (r == 0) {
                int idx = oy.buffer(chunk);
                int read = in.read(oy.data, idx, chunk);
                if (read <= 0) eos = true;
                else oy.wrote(read);
            }
        }
        return new Decoded(vi.rate, channels, samples.toArray());
    }

    /** Decodes every complete packet buffered in the stream into PCM samples. */
    private static void drain(StreamState os, Packet op, Block vb, DspState vd, float[][][] pcmHolder,
                              int[] index, int channels, FloatList samples) {
        while (os.packetout(op) == 1) {
            if (vb.synthesis(op) == 0) vd.synthesis_blockin(vb);
            int got;
            while ((got = vd.synthesis_pcmout(pcmHolder, index)) > 0) {
                float[][] pcm = pcmHolder[0];
                for (int i = 0; i < got; i++)
                    for (int c = 0; c < channels; c++) samples.add(pcm[c][index[c] + i]);
                vd.synthesis_read(got);
            }
        }
    }

    private static final class FloatList {
        private float[] a = new float[1 << 16];
        private int n;

        void add(float v) {
            if (n == a.length) a = java.util.Arrays.copyOf(a, n * 2);
            a[n++] = v;
        }

        float[] toArray() {
            return java.util.Arrays.copyOf(a, n);
        }
    }
}

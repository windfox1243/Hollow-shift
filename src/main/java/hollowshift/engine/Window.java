package hollowshift.engine;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;

/**
 * Game window. Everything is drawn into a fixed 640x360 back buffer which is then
 * scaled to the window with nearest-neighbour filtering and letterboxing.
 */
public final class Window {
    public static final int WIDTH = 640;
    public static final int HEIGHT = 360;

    private final JFrame frame;
    private final Canvas canvas;
    private final BufferedImage screen = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
    private volatile boolean closeRequested;
    private boolean fullscreen;

    public Window(String title, Input input) {
        frame = new JFrame(title);
        canvas = new Canvas();
        canvas.setPreferredSize(new Dimension(WIDTH * 2, HEIGHT * 2));
        canvas.setBackground(Color.BLACK);
        canvas.setIgnoreRepaint(true);
        canvas.addKeyListener(input);
        canvas.setFocusTraversalKeysEnabled(false);
        canvas.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                input.releaseAll();
            }
        });
        frame.add(canvas);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeRequested = true;
            }
        });
        frame.setIconImage(icon());
        frame.setVisible(true);
        canvas.createBufferStrategy(2);
        canvas.requestFocus();
    }

    public Graphics2D beginFrame() {
        Graphics2D g = screen.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        return g;
    }

    public void present(java.util.function.Consumer<Graphics2D> overlay) {
        BufferStrategy bs = canvas.getBufferStrategy();
        if (bs == null) return;
        do {
            do {
                Graphics2D g = (Graphics2D) bs.getDrawGraphics();
                int cw = canvas.getWidth();
                int ch = canvas.getHeight();
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, cw, ch);
                double scale = Math.min(cw / (double) WIDTH, ch / (double) HEIGHT);
                // prefer a crisp integer scale unless it would waste more than 10% of the window
                if (scale >= 1 && Math.floor(scale) / scale > 0.9) scale = Math.floor(scale);
                int w = (int) (WIDTH * scale);
                int h = (int) (HEIGHT * scale);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g.drawImage(screen, (cw - w) / 2, (ch - h) / 2, w, h, null);
                // a sharp layer on top, in logical coordinates but rasterised at window size
                g.translate((cw - w) / 2, (ch - h) / 2);
                g.scale(w / (double) WIDTH, h / (double) HEIGHT);
                g.clipRect(0, 0, WIDTH, HEIGHT);
                overlay.accept(g);
                g.dispose();
            } while (bs.contentsRestored());
            bs.show();
        } while (bs.contentsLost());
        Toolkit.getDefaultToolkit().sync();
    }

    public boolean closeRequested() {
        return closeRequested;
    }

    public boolean isFullscreen() {
        return fullscreen;
    }

    public void setFullscreen(boolean on) {
        if (on == fullscreen) return;
        fullscreen = on;
        SwingUtilities.invokeLater(() -> {
            frame.dispose();
            frame.setUndecorated(on);
            if (on) frame.setExtendedState(Frame.MAXIMIZED_BOTH);
            else {
                frame.setExtendedState(Frame.NORMAL);
                frame.pack();
                frame.setLocationRelativeTo(null);
            }
            frame.setVisible(true);
            canvas.createBufferStrategy(2);
            canvas.requestFocus();
        });
    }

    public void close() {
        frame.dispose();
    }

    private static Image icon() {
        BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(20, 18, 24));
        g.fillRect(0, 0, 32, 32);
        g.setColor(new Color(255, 196, 92));
        g.fillOval(10, 8, 12, 14);
        g.setColor(new Color(60, 52, 40));
        g.fillRect(12, 22, 8, 4);
        g.dispose();
        return img;
    }
}

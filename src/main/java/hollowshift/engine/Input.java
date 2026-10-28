package hollowshift.engine;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Keyboard state sampled once per fixed update. AWT events arrive on the event thread,
 * so they are queued and applied in {@link #poll()} on the game thread.
 */
public final class Input implements KeyListener {
    private final Map<Action, int[]> bindings = new EnumMap<>(Action.class);
    private final Set<Integer> down = new HashSet<>();
    private final Set<Integer> pressedThisTick = new HashSet<>();
    private final Set<Integer> releasedThisTick = new HashSet<>();
    private final ArrayDeque<int[]> pending = new ArrayDeque<>(); // {keyCode, 1=down/0=up}
    private final ArrayDeque<Character> typedPending = new ArrayDeque<>();
    private final StringBuilder typedThisTick = new StringBuilder();
    private int lastPressedKey;

    public Input() {
        resetBindings();
    }

    public void resetBindings() {
        for (Action a : Action.values()) bindings.put(a, new int[]{a.defaultPrimary, a.defaultSecondary});
    }

    public int[] binding(Action a) {
        return bindings.get(a);
    }

    public void bind(Action a, int slot, int keyCode) {
        bindings.get(a)[slot] = keyCode;
    }

    /** Applies queued events; call once at the start of each fixed update. */
    public void poll() {
        pressedThisTick.clear();
        releasedThisTick.clear();
        typedThisTick.setLength(0);
        lastPressedKey = 0;
        synchronized (pending) {
            while (!pending.isEmpty()) {
                int[] ev = pending.poll();
                if (ev[1] == 1) {
                    if (down.add(ev[0])) {
                        pressedThisTick.add(ev[0]);
                        lastPressedKey = ev[0];
                    }
                } else if (down.remove(ev[0])) {
                    releasedThisTick.add(ev[0]);
                }
            }
            while (!typedPending.isEmpty()) typedThisTick.append(typedPending.poll());
        }
    }

    public boolean held(Action a) {
        for (int k : bindings.get(a)) if (k != 0 && down.contains(k)) return true;
        return false;
    }

    public boolean pressed(Action a) {
        for (int k : bindings.get(a)) if (k != 0 && pressedThisTick.contains(k)) return true;
        return false;
    }

    public boolean released(Action a) {
        for (int k : bindings.get(a)) if (k != 0 && releasedThisTick.contains(k)) return true;
        return false;
    }

    public boolean keyPressed(int keyCode) {
        return pressedThisTick.contains(keyCode);
    }

    /** Key code pressed this tick (0 if none), used for rebinding. */
    public int lastPressedKey() {
        return lastPressedKey;
    }

    /** Printable characters typed this tick, used for text fields. */
    public String typed() {
        return typedThisTick.toString();
    }

    /** Queues a synthetic key event (used by automated tests and bots). */
    public void simulate(int keyCode, boolean pressed) {
        synchronized (pending) {
            pending.add(new int[]{keyCode, pressed ? 1 : 0});
        }
    }

    public void releaseAll() {
        synchronized (pending) {
            for (int k : down) pending.add(new int[]{k, 0});
        }
    }

    public static String keyName(int keyCode) {
        return keyCode == 0 ? "-" : KeyEvent.getKeyText(keyCode).toUpperCase();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        synchronized (pending) {
            pending.add(new int[]{e.getKeyCode(), 1});
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        synchronized (pending) {
            pending.add(new int[]{e.getKeyCode(), 0});
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        char c = e.getKeyChar();
        if (c >= 32 && c < 127) {
            synchronized (pending) {
                typedPending.add(c);
            }
        }
    }
}

package hollowshift;

import hollowshift.engine.Game;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        System.setProperty("sun.java2d.uiScale", "1");
        new Game().run();
    }
}

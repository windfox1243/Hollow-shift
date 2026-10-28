package hollowshift.game;

import hollowshift.engine.Input;
import hollowshift.engine.audio.Audio;
import hollowshift.engine.gfx.Art;
import hollowshift.story.SaveData;

/** The engine services a {@link World} needs; lets tests run a world without opening a window. */
public record Services(Audio audio, Art art, Input input, SaveData save) {
}

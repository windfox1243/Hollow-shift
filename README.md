# HOLLOW SHIFT

A top-down survival horror game written entirely in Java SE: the game uses no external libraries, no native code, and needs no build tool to install. Art and sound are CC0 assets by Kenney (see CREDITS.md) plus pixel art and sounds generated in code. It is a non-commercial research project.

You are a night-shift miner in Pit 9. Find fuses, power the fuse boxes and ride the lift out. Something lives down here. At the start of every shift you are told the **rules**: what it can sense (sight, hearing, smell, tremor, echo...) and what can kill you besides it (cold, hunger, a dead lantern).

## Requirements
- JDK 21 or newer. Tested on JDK 27.

## Commands (run from this folder)
```
java tools/Build.java run        # compile and play
java tools/Build.java test       # run the test suite
java tools/Build.java jar        # build dist/hollow-shift.jar
java -jar dist/hollow-shift.jar  # play the packaged game
java tools/Build.java inspect ../www build/c3-dump.txt   # research tool, see docs/RE_ANALYSIS.md
java tools/Build.java convert-audio   # re-convert CC0 OGG sources to WAV (see CREDITS.md)
```

## Controls (rebindable in Settings → Controls)
| Key | Action |
|---|---|
| WASD / arrows | move |
| Shift | run (loud, uses stamina) |
| E / Space | interact, pick up (hold for breakers) |
| Q | throw stone or flare / drop fuse |
| F | lantern on/off |
| Esc | pause |

## Layout
- `src/main/java/hollowshift/`
  - `engine/`: loop, window, input, audio mixer, lighting, JSON.
  - `world/`: tiles, room templates, generator, A*.
  - `entity/`, `ai/`, `rules/`: gameplay.
  - `game/`: World, builder, HUD, objectives.
  - `scenes/`: menus and screens.
  - `story/`: campaign and save data.
- `src/main/resources/`: rooms, final map, campaign text, credits.
- `src/test/java/`: self-contained test runner and a snapshot renderer.
- `docs/RE_ANALYSIS.md`: research notes. `docs/DESIGN.md`: game design.

Settings and progress are saved in `~/.hollowshift/save.json`.

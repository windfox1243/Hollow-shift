# Credits & asset licences

## Code, rooms, maps, writing
Original work of the HOLLOW SHIFT project (non-commercial research project).

## Art and sound
Sprites without an entry in `assets/sprites.json` are drawn in code (`engine/gfx/ProceduralArt.java`): items, fuse boxes, lift, lamps, heaters. The Tenant and its tentacles are drawn procedurally every frame (`entity/Creature.java`, `entity/Tentacle.java`), as is the jumpscare (`scenes/Jumpscare.java`).
Vietnamese text uses a system font (Tahoma, or Java's Dialog font) because the pixel font has no Vietnamese glyphs.
Sounds without a WAV in `assets/sfx/` are synthesised (`engine/audio/Synth.java`): creature steps, drones, chase music, power surge, death sting.

## Third-party CC0 assets
Kenney packs were downloaded from Kenney's account on OpenGameArt (kenney.nl itself was unreachable). Licence files are kept next to the files.

| Pack | Author | Source | Licence | Used for | Location |
|---|---|---|---|---|---|
| Tiny Dungeon 1.0 | Kenney | https://opengameart.org/content/tiny-dungeon | CC0 | floor, moss (tinted), planks, walls, crates, player, stone | `src/main/resources/assets/third_party/kenney_tiny_dungeon/` |
| Kenney Fonts (KenPixel) | Kenney | https://opengameart.org/content/kenney-fonts | CC0 | all text (`assets/font.ttf`) | `src/main/resources/assets/third_party/kenney_fonts/` |
| RPG Sound Pack (50 RPG sound effects) | Kenney | https://opengameart.org/content/50-rpg-sound-effects | CC0 | footsteps, grab/drop/throw, stone impact, fuse latch, lantern click, eating | OGG sources in `third_party/kenney_rpg_audio/` |
| Interface Sounds 1.0 | Kenney | https://opengameart.org/content/interface-sounds | CC0 | menu move/select/back, typewriter | OGG sources in `third_party/kenney_interface_sounds/` |

## Sounds provided by the project owner
Downloaded by the project owner from Pixabay and used under the Pixabay Content License (free use, no attribution required; not redistributed on their own). Only the converted WAVs inside the game are in this repository; the original MP3s are kept locally and ignored by git (`convert-audio` skips them when missing).

| File | Author (Pixabay) | Used for |
|---|---|---|
| `third_party/user_audio/chase_tension.mp3` (horror transition, jumpscares & chase tension, #443387) | tomas_herudek | chase music when the Tenant finds you (`chase_found`) |
| `third_party/user_audio/jumpscare.mp3` (jumpscare, #154489) | kingcookiewastaken | jumpscare sound (`scare_main`) |

All OGG/MP3 sources are converted to WAV (`src/main/resources/assets/sfx/`) with `java tools/Build.java convert-audio`; the mapping is `third_party/audio-map.txt`.

## Build-time tool dependency
| Library | Version | Licence | Use |
|---|---|---|---|
| JOrbis (pure Java Ogg Vorbis decoder) | 0.0.17.4, Maven Central `com.googlecode.soundlibs:jorbis` | LGPL 2.0 | `tools/lib/`, only for `convert-audio`; **not** packaged into the game jar |
| JLayer (pure Java MP3 decoder) | 1.0.1, Maven Central `javazoom:jlayer` | LGPL 2.1 | `tools/lib/`, only for `convert-audio`; **not** packaged into the game jar |

## Research reference
The mechanics study in `docs/RE_ANALYSIS.md` was made from a Construct 3 export, for research only. No assets, maps, text or code from that game are included in HOLLOW SHIFT.

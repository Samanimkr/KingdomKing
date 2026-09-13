# KingdomKing — The Stolen Crown

A complete small pixel-art action-adventure built on Samani Mukhtar's 2017 Java game. Reclaim three crown jewels, rescue your people, rebuild your castle, and defeat the Pretender. The original character artwork is preserved, and the restored 2017 exploration prototype is still available in **Classic mode**.

The new campaign includes seven connected areas, sword and crown combat, dodge and ground slam, enemy behaviors and interactions, environmental puzzles, secrets, equipment choices, castle restoration, an original synthesized soundtrack, and saved campaign progress.

## Play

### Standalone Mac app

Double-click **KingdomKing.app**, then press **Enter** or click **Begin / continue adventure**. The app includes Java. The distributed build is for Apple Silicon; build it on an Intel Mac to produce an Intel version.

### From source

Use **JDK 17 or newer**. No Maven, Gradle, downloads, or third-party libraries are required.

```sh
./run.sh
```

On macOS, you can also double-click `KingdomKing.command`. On Windows, use `run.bat`. The launchers honor `JAVA_HOME` when set and otherwise use Java on `PATH`.

To build and run the portable JAR:

```sh
./build.sh build
java -jar dist/KingdomKing.jar
```

On Windows use `build.bat build`. The JAR requires Java 17+ and a graphical desktop.

## Controls

| Control | Action |
| --- | --- |
| Enter | Begin / continue; respawn after defeat |
| WASD / arrows | Move |
| Shift / Ctrl | Sprint |
| J / Z / left click | Sword attack; holding J chains three strikes |
| Q / right click | Throw crown; recall it; time a returning catch for a powered strike |
| Space | Dodge dash, with brief invulnerability |
| X | Ground slam; costs stamina |
| E | Interact, talk, operate mechanisms, purchase an upgrade, or use a portal |
| U / I | Cycle the two crown-jewel sockets |
| H | Field journal, controls, objective, and local puzzle clues |
| M | World atlas |
| Esc | Pause / resume; close atlas or journal |
| R | Return to the castle fountain and save discoveries |
| F | Toggle sound and music |
| F2 | Toggle reduced screen shake |
| N, then Enter | Start a new campaign from the title/pause menu; Esc cancels |

The native **Game** and **Settings** menus provide alternatives for starting, pausing, opening the journal/atlas, returning home, and toggling sound/effects. Switching applications clears held keys and pauses the simulation.

## Your adventure

1. **The Quiet Crown:** speak to Sir Bramble in the southeast courtyard. He teaches the third-strike guard break. The fountain heals and saves.
2. **Briarfield, north:** throw your crown east through three bells in order, timing the moving shutter. Clear the camp, rescue the blacksmith, and take the ruby.
3. **Mosswater Mill, east:** cycle the water lever. Low water exposes a cache; high water powers the wheel and opens the island gate. Rescue the cook and collect the emerald. Cut a support rope to drop a bridge, or bait an archer into ringing the projectile bell.
4. **Temple of Three Suns, west:** rotate three statues to face the central seal. Keep your body out of the aligned light beams. Rescue the cartographer and collect the sapphire.
5. **The Pretender's Keep, south:** deposit your crown to power the gate, cross without it, and pull the inner release. All three jewels are needed to enter the throne room. Look for a shadow treasury, an enemy-operated pressure seal, and hollow stone.
6. **The Hollow Throne:** defeat the Pretender through three phases. His charge and thrown crown can leave him stunned against obstacles. Dodge his slam and the final-phase fire.
7. **The Forgotten Treasury:** an optional crypt beneath the keep contains guardians, a permanent heart container, and treasure.

The campaign is designed as a compact adventure. Its length depends on exploration and learning the puzzles; the automated test can speedrun the core route in about 2–3 minutes of simulation time.

## Combat and equipment

- **Sword:** a three-hit combo. Sir Bramble's training lets the third strike bypass a knight's shield.
- **Crown:** travels out and returns, hits foes, collects coins, rings switches, cuts grass and support ropes. Press Q just before catching a returning crown for an empowered attack.
- **Dodge:** uses stamina and briefly avoids damage. Telegraphed enemy attacks show their direction or area before firing.
- **Ground slam:** uses more stamina, stuns nearby enemies, breaks brittle scenery, and uncovers the crypt. Bosses resist prolonged slam stuns.
- **Ruby:** crown strikes ignite enemies and combustible wood.
- **Emerald:** a crown that struck an enemy heals on return, with a cooldown; perfect catches also heal.
- **Sapphire:** crown throws ricochet from walls up to three times.
- Only **two jewels** can be equipped at once. Empty sockets are a valid choice when cycling.

Enemies include hopping, splitting, sticky, and explosive slimes; coin-stealing goblins; shield knights; mushroom archers; fire casters; occasional angry chickens; and the Pretender. Enemy attacks can hurt other enemies, ignite scenery, trigger a bell, shatter a wall, or activate a heavy pressure seal.

## Rebuild your kingdom

Rescued villagers return to the courtyard. Their interactions show the next purchase and its price before you press E.

- **Blacksmith:** restore the forge, then buy three blade upgrades and additional heart containers.
- **Cook:** restore the garden, then buy a three-minute stamina-recovery buff and a full heal.
- **Cartographer:** restore the bell tower to reveal secrets on the local map.
- **Royal decrees:** interact with the throne to cycle fair rule, no taxes, and royal hunt. No taxes discounts supplies but increases building costs. A royal hunt repopulates revisited regions with tougher enemies and better coin rewards.

Each restored building changes the scenery and adds a layer to the soundtrack. Defeating the Pretender permanently clears the marsh and restores the kingdom's final musical layer.

## Saving

Progress saves automatically after important interactions, periodically after changes, on returning home, and when closing the window. Continue starts you safely at the castle fountain with full health.

Saved data includes coins, jewels and equipped sockets, weapon and heart upgrades, rescued villagers, restored buildings, solved puzzles, claimed secrets, the decree, and boss victory. Ordinary enemies and loose loot reset after restarting; the boss stays defeated. Defeat preserves campaign progress and costs at most ten coins.

Default save locations:

- macOS: `~/Library/Application Support/KingdomKing/campaign.properties`
- Windows: `%APPDATA%\KingdomKing\campaign.properties`
- Linux: `$XDG_DATA_HOME/KingdomKing/campaign.properties`, or `~/.local/share/KingdomKing/campaign.properties`

Override the path when testing:

```sh
java -Dkingdomking.save=/tmp/kingdomking-test.properties -jar dist/KingdomKing.jar
```

Saves use atomic replacement. Starting a new campaign creates `campaign.previous.properties` first. An unreadable save is protected from overwriting, and the game reports the problem in its message panel.

## Original Classic mode

```sh
./run.sh --classic
```

This retains the restored original 64×48 map and software renderer. Controls: WASD/arrows move, Shift/Ctrl sprint, Esc pause, R return to the original spawn.

## Tests

```sh
./build.sh test
```

Or `build.bat test` on Windows. The build targets Java 17, enables all compiler warnings, and treats warnings as errors.

Verification includes:

- **12 classic regression groups:** original resource loading, map bounds, input, movement, collision, animation, rendering, and lifecycle.
- **24 campaign regression groups:** combat, all puzzle mechanisms, enemy behaviors, equipment, reconstruction, decrees, boss progression, death, navigation/gating, atomic saves, rendering, audio, and a prolonged combat simulation.
- **One full-input campaign playthrough:** completes the main campaign using simulated player input without granting health, coins, or progression. It rescues all three villagers, solves the main puzzles, collects the jewels, buys upgrades, and defeats the boss.
- A separate JVM renders directly from the packaged JAR, verifying resource packaging.

Images and an eight-second sound demo are written to `build/test-output/`.

## Screenshots and sound demo

These commands work without a display or an audio device:

```sh
java -Djava.awt.headless=true -jar dist/KingdomKing.jar --screenshot build/menu.png --scene menu
java -Djava.awt.headless=true -jar dist/KingdomKing.jar --screenshot build/boss.png --scene boss
java -Djava.awt.headless=true -jar dist/KingdomKing.jar --sound-demo build/sound-demo.wav
```

Screenshot scenes: `menu`, `castle`, `meadow`, `marsh`, `temple`, `keep`, `boss`, `restored`, `atlas`, and `journal`. Screenshots use the logical 480×270 resolution; the desktop app displays at 2× scale. `--classic --screenshot build/classic.png` renders the original 300×168 scene.

## Package for macOS

```sh
./package-macos.sh
```

Output: `build/app/KingdomKing.app`. This includes a Java desktop runtime for the packaging Mac's architecture. The local build is not an Apple-notarized public release.

## Code layout

- `src/game/rain/Game.java` — campaign desktop host and input routing.
- `src/game/rain/adventure/Adventure.java` — fixed-step combat, puzzles, AI, progression, and interactions.
- `src/game/rain/adventure/WorldMap.java` — seven authored regions and their objects.
- `src/game/rain/adventure/AdventureRenderer.java` — world animation and game UI.
- `src/game/rain/adventure/AudioEngine.java` — original synthesized effects and adaptive music.
- `src/game/rain/adventure/SaveStore.java` — versioned, atomic campaign saves.
- `src/game/rain/ClassicGame.java` and original packages — preserved revival mode.
- `tests/` — regression and full-input playthrough tests.
- `tools/Build.java` — offline, cross-platform build/test runner.
- `res/` — original spritesheets, maps, and Photoshop sources, unchanged.

## License

MIT — Copyright (c) 2017 Samani Mukhtar. See `LICENSE`. The new procedural artwork and synthesized music/effects are generated by the game's own code; no third-party asset dependencies were added.

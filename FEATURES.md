# The Stolen Crown — implemented feature map

This is the playable interpretation of the thirty revival ideas. Everything below is part of the campaign; the original prototype remains available through `--classic`.

| # | Idea | Implementation |
| --- | --- | --- |
| 1 | Expressive cape | Trails movement, sways when idle, reacts to a hidden draft, and lifts with the slam. |
| 2 | Dodge dash | Stamina cost, cooldown, directional burst, invulnerability, dust and afterimages. |
| 3 | Boomerang crown | Outbound/return flight, manual recall, timed catches, enemy hits, coin collection, switches and rope cutting. |
| 4 | Three-hit sword combo | Two quick cuts and a heavier finisher; Sir Bramble teaches its guard break. |
| 5 | Impact feedback | Hit stop, flash, knockback, sparks, sound and bounded screen shake. |
| 6 | Royal ground slam | Leap, landing shockwave, stamina cost, enemy stun, brittle-object destruction and hidden-floor discovery. |
| 7 | Goblin thieves | Steal loose coins, flee, throw projectiles and return stolen money when defeated. |
| 8 | Shield knights | Directional shields, charge telegraphs, wall stuns, rear vulnerability and trained combo counters. |
| 9 | Slime varieties | Hopping, splitting, sticky-trail and explosive slimes. |
| 10 | Mushroom archers | Telegraph aimed arrows; their projectiles can hit a puzzle bell or another enemy. Fire casters ignite wood. |
| 11 | Rival king boss | The Pretender uses a crown throw, charge and slam across three phases, then summons reinforcements and fire. |
| 12 | Enemy interactions | Charges knock other enemies aside, explosions damage groups, and projectiles can hit allies or scenery. |
| 13 | Crown-routing puzzle | Briarfield's three ordered bells, flight timeout and moving shutter. |
| 14 | Crown as a power source | Leave it on the keep pedestal, cross the gate without it, and reach the inner release. |
| 15 | Light and shadow | Three rotating sun statues power a central seal; the player can interrupt their beams. |
| 16 | Water-level puzzle | Low/mid/high water, an exposed cache, a floating bridge, powered wheel and island gate. |
| 17 | Enemy-powered mechanisms | A guardian arrow rings a bell; a knight breaks brittle scenery and activates a heavy pressure seal. |
| 18 | Environmental secret cues | Fireflies, a cape-tugging draft, hollow footsteps, a shadow trap and a submerged passage. |
| 19 | Castle restoration | Forge, garden and bell tower change appearance after reconstruction. |
| 20 | Rescued villagers | Blacksmith upgrades, cook's temporary meal buff, cartographer's secret map and mentor training. |
| 21 | Crown-jewel builds | Ruby fire, emerald healing, sapphire ricochets; two interchangeable sockets for three jewels. |
| 22 | Royal decrees | Fair rule, no taxes with a rebuilding tradeoff, and repeatable royal hunts with stronger enemies and richer rewards. |
| 23 | A world that remembers | Saved discoveries and villagers, a permanently defeated boss, purified marsh and changed kingdom music. |
| 24 | Surface footsteps | Distinct grass, stone, wood, splash and hollow-stone effects. |
| 25 | Crown sound identity | Throw, return whistle, catch, perfect catch and jewel-specific tones. |
| 26 | Adaptive soundtrack | A procedural melody, bass and additional instrumental/percussive layers as the kingdom is restored; a boss variation and victory cue. |
| 27 | Ambient life | Birds scatter, frogs hop near water, butterflies return to the garden, grass bends, flags wave and torches flicker. |
| 28 | Idle personality | Crown adjustment, settling cape, idle dots and sleepy bubbles. |
| 29 | Victory sequence | Boss fall, impact pause, cleared hostiles, a short musical break, returning light, victory cue and homeward portal. |
| 30 | Destructible scenery | Grass, pots, fences, signs, wood and brittle stone; coins, secrets and occasional angry chickens. |

## Additional foundations

- Seven connected regions, an atlas, a journal, context-sensitive prompts and a title screen.
- Atomic, versioned campaign saves; checkpoint respawn; a previous-save backup for new campaigns.
- Mouse and keyboard startup, native application menus, focus pause, sound toggle and reduced shake.
- A standalone Apple Silicon app with Java included, a portable Java 17+ JAR, and offline build scripts.
- 37 regression groups and one full-input campaign playthrough, retaining the classic regression coverage.

## Scope and testing limits

The artwork outside the original character is deliberately compact procedural pixel art, and the music/effects are synthesized in code. This is a small single-player campaign, with a main route and optional secrets. Saves preserve campaign progression and resume safely in the castle; ordinary enemy positions and loose loot are not serialized.

The automated full-input playthrough uses the same simulation inputs as the desktop player. It does not grant health, coins, items or solved objectives. It verifies that the main adventure can be completed, but it does not establish how every human player will experience its difficulty or pacing.

The app has been exercised on this Apple Silicon Mac, and the code/tests run on the installed Java 22 and 23 runtimes. Windows and Linux launchers are provided, but native platform testing there remains outstanding.

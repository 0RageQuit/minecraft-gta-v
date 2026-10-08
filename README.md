# Minecraft × GTA V

**Play real Minecraft inside Los Santos.**

The full source tree, setup notes and current installed mod build are preserved in [Minecraft-GTA-V-private-draft.zip](Minecraft-GTA-V-private-draft.zip). Extract the archive to view or build the project.

An experimental Windows mod that runs Minecraft Java alongside GTA V Legacy Story Mode. Build on GTA streets, play as your Minecraft character, and fly between skyscrapers with an elytra.

This version focuses on Minecraft movement: walking, strafing, sprinting, sneaking, jumping, creative flight, and elytra physics, with terrain and camera collision work to make the two games fit together.

## Current features

- Your Minecraft skin, first-person and third-person views, pose changes and vanilla movement FOV effects.
- Minecraft controls by default; switch to native GTA controls for driving.
- Building and breaking Minecraft blocks; placed blocks mirrored to GTA collision objects.
- Minecraft hotbar, held items, HUD, creative inventory/search, offhand items, chat and commands.
- Persistent builds in a dedicated Minecraft world.
- Creative flight, elytra steering and firework boosts.
- TNT/creeper explosions, bows/crossbows, melee and ender pearls with GTA effects.
- Experimental mobs versus GTA characters/police and cross-game damage.
- Floor, wall, ceiling and camera collision handling, plus experimental moving-platform support.
- Experimental survival health/damage and Nether-themed portal, mob and fire/lava interactions.
- Depth-aware rendering, ambient lighting/grade, opaque block lighting fix and extra world image around the screen edges for camera turns.

## Controls

| Key | Action |
|---|---|
| W A S D / mouse | Move / look |
| Space / Ctrl / Shift | Jump / sprint / sneak |
| E | Inventory |
| Left / right click | Attack or break / use or place |
| 1–9 / wheel | Hotbar |
| F / Q | Swap offhand / drop |
| F5 | Perspective |
| T / slash | Chat / command |
| F8 | Minecraft ↔ GTA controls |
| F7 | Passthrough on/off |
| F9 | Re-level ground |

Creative: double-tap Space to fly; Space rises, Shift descends. Elytra: equip in the chest slot, press Space while airborne, steer with the mouse and use fireworks to boost.

## Status and limitations

Private development snapshot; no public release published. Not a finished installer.

Collision on unusual surfaces, shaped blocks, moving platforms and fast camera turns needs broader testing. Recent turn snapshots confirmed filled screen edges but do not prove every transient frame. Some inherited combat/Nether systems have not been reverified in the latest build. Both games run simultaneously and require substantial resources. GTA roads/buildings cannot be mined. GTA Enhanced and GTA Online are not supported by this version.

## Setup and builds

Development versions: Minecraft Java 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3, JDK 25; GTA V Legacy build 3889 with matching ScriptHookV and ReShade 6.8.0 with add-on support.

`installed-build/` contains the exact current custom ASI, Fabric JAR, shader and ReShade configuration, with SHA256 hashes. Obtain shared loaders and Fabric API separately from their official sources. No game files, saves, skins, credentials or third-party loader binaries are bundled. Read `docs/INSTALL.md` and `docs/UPSTREAM-README.md` inside the archive. The upstream guide predates the current controls; use the table above.

## Credits and license

Derived from [rehan-remade/universal-modder](https://github.com/rehan-remade/universal-modder/tree/main/examples/minecraft-gta5-passthrough). Includes adaptations credited in `mc/src/main/resources/LICENSE-minecraft-ring` to siddoff and justbustin. MIT source license; upstream notices retained. Bundled Java-WebSocket and Gradle wrapper retain their own licenses.

Minecraft belongs to Mojang/Microsoft; GTA V belongs to Rockstar/Take-Two. Unofficial fan project.


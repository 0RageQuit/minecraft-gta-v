# Minecraft × GTA V

Run Minecraft Java inside GTA V Legacy Story Mode. Build in Los Santos, use your Minecraft skin, and travel with Minecraft movement, creative flight or an elytra. Both games run together on your PC.

## Download and install

[Download v0.1.0](https://github.com/0RageQuit/minecraft-gta-v/releases/tag/v0.1.0), the first experimental release.

- **To play:** download `Minecraft-GTA-V-v0.1.0.zip` and follow the included `INSTALL.md`.
- **For source code:** download `Minecraft-GTA-V-v0.1.0-source.zip`. The complete source is inside this archive.

Requires Windows, GTA V **Legacy** (development build 3889), Minecraft Java 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3 and Java 25. ScriptHookV with an ASI loader and ReShade 6.8.0 with add-on support must be installed separately. Back up your files before installing and use a dedicated Minecraft instance/world.

GTA Enhanced and GTA Online are unsupported. Game files, accounts, saves and personal skins are not bundled.

### Optional: skip the story

For free roam without playing through the campaign, you can use a completed GTA V Legacy Story Mode save, such as [100% Game Save on GTA5-Mods](https://www.gta5-mods.com/misc/100-save-game). This is optional; an existing free-roam save also works. Back up your own saves and follow the download page's installation instructions. This third-party save has not been tested with this mod.

## Features

- Minecraft walking, sprinting, sneaking, jumping, creative flight and elytra with firework boosts.
- Your Minecraft skin and first- or third-person views.
- Block placing/breaking, persistent builds and GTA collision for placed blocks.
- Minecraft inventory, hotbar, offhand, chat, commands and HUD.
- Switch between Minecraft controls and GTA controls for driving.
- Rendering and collision fixes for floors, walls, ceilings and camera turns.

Experimental systems include TNT/creeper explosions, bows/crossbows, melee, ender pearls, mobs interacting with GTA characters, survival damage, moving platforms and Nether-themed effects. These need more testing in the current build.

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

## Known limitations

This is an early release. Unusual terrain, shaped blocks, moving platforms and fast camera turns can still expose collision or rendering issues. Running both games requires substantial resources. GTA roads and buildings cannot be mined.

## Credits and license

Derived from [rehan-remade/universal-modder](https://github.com/rehan-remade/universal-modder/tree/main/examples/minecraft-gta5-passthrough). Minecraft Ring adaptations credit siddoff and justbustin in `mc/src/main/resources/LICENSE-minecraft-ring`. MIT source license; upstream notices and dependency licenses are retained in the source archive.

Unofficial fan project. Minecraft belongs to Mojang/Microsoft; GTA V belongs to Rockstar/Take-Two.

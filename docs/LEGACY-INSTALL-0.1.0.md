# Minecraft × GTA V v0.1.0 — experimental Windows release

Use GTA V Legacy Story Mode (tested build 3889), Minecraft Java 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3 and 64-bit Java 25. Enhanced is not supported. Own both games and use a dedicated Minecraft instance/world.

1. Close both games. Back up the GTA files you will replace and your Minecraft instance/world.
2. Install a ScriptHookV version compatible with your GTA build, including its dinput8.dll ASI loader, from https://www.dev-c.com/gtav/scripthookv/ .
3. Obtain ReShade 6.8.0 with full add-on support from https://reshade.me/ . Place its 64-bit runtime in the GTA folder as ReShade64.asi. This bridge expects that name; do not install a second runtime as dxgi.dll. Obtain ReShade.fxh and ReShadeUI.fxh from the official standard shader distribution and put both in reshade-shaders/Shaders/.
4. Copy all contents of installed-build/gta/ into your GTA folder (beside GTA5.exe). This includes MCPassthrough.asi, both ReShade configuration files and MCPassthrough.fx. The supplied ReShade configuration replaces the existing preset: back it up first. Remove/disable GrandTheftMinecraft.asi or other competing Minecraft bridges.
5. In Prism Launcher create a Minecraft 26.3 instance, install Fabric Loader 0.19.5, and add Fabric API 0.161.0+26.3. Copy installed-build/minecraft/mods/passthrough-0.1.0.jar into that instance's .minecraft/mods folder.
6. Set that instance to Java 25, 4 GB maximum memory, and Java arguments: --enable-native-access=ALL-UNNAMED -Dpassthrough.maxFps=60 -Dpassthrough.ownSkin=true
7. Start the Minecraft instance and enter its dedicated world if it does not open automatically. Start GTA Legacy in offline Story Mode with BattlEye disabled through the launcher's supported setting. Never use this mod in GTA Online.
8. In GTA press F7 to show the Minecraft view if needed. F8 switches Minecraft/native GTA controls; F9 relevels ground. WASD and mouse move/look, Space jumps, Ctrl sprints, Shift sneaks, E opens inventory, F5 changes perspective. Creative: double Space toggles flight. Elytra: equip it in the chest slot, jump while airborne, use a firework to boost.

If no Minecraft image appears, check both games are running, the ASI loader and ReShade64.asi loaded, the custom technique is enabled, and local port 25599 is available. These prerequisites are not bundled. This is an experimental release; terrain boundaries, fast camera changes and unusual GTA geometry may still reveal collision/rendering bugs.

Uninstall with both games closed: remove MCPassthrough.asi, MCPassthrough.fx and the passthrough Fabric JAR, then restore your ReShade configuration. Keep shared loaders if other mods use them. Source and MIT/upstream notices are included in the source archive. No game files, accounts, saves or personal skin are included.

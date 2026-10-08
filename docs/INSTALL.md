# Minecraft GTA V — automatic Melty setup (0.1.2-melty candidate)

Requires the player's own GTA V Legacy build 1.0.3889.0 and Minecraft Java 26.3 entitlement. Both games run on the same Windows PC. Single player, Story Mode only; Enhanced and Online are unsupported.

## What Play does

The installed recipe in melty.json places the rebuilt GTA plugin, ReShade runtime, shader and configurations beside GTA5.exe. Melty installs Ultimate ASI Loader. The plugin uses its own native adapter and no longer imports ScriptHookV.

The release bundles portable Prism, Java 25, Fabric API and the rebuilt Minecraft mod. The companion automatically creates a dedicated Minecraft 26.3/Fabric 0.19.5 instance under LocalAppData/MinecraftGTAV/prism, selects the bundled Java and installs both mod jars. Players do not configure Java or Fabric or copy mod files.

First setup asks for the player's own Microsoft account. Prism downloads Minecraft through its normal authenticated flow. The release contains no game content, accounts, skins or saves. Minecraft automatically opens its dedicated world and logs 'Minecraft companion ready'; Melty waits for this before launching GTA in Story Mode with BattlEye disabled. Rockstar connects normally for activation of the player's own copy; the recipe does not force offline mode. Subsequent Play starts both programs. Passthrough is enabled by default once connected. F7 toggles the view, F8 switches Minecraft/GTA controls and F9 relevels the ground.

Version 0.1.2-melty removes the forced-offline argument from 0.1.1-melty, which could make Rockstar close a Steam copy with an activation-required message. It changes packaging and launch arguments; the tested Minecraft/GTA gameplay components remain the same. Normal Rockstar/Steam authentication still applies. An account sign-in, if required by Rockstar, must be completed by the player.

For the Steam edition, Melty detects GTA V Legacy as Steam app 271590 and uses the player's installed Steam launcher, forwarding the recipe's Story Mode arguments. The recipe uses `launch.kind: game` so the primary game and install folder come from Melty's detection. It does not hard-code a Steam library folder or start GTA5.exe directly. Minecraft starts first; Steam/Rockstar startup can take additional time before the GTA window appears. The local Steam copy was verified at build 1.0.3889.0, with the exact current plugin, ReShade runtime and native mapping loaded after launch. This verifies launch and loading; longer gameplay testing remains separate.

## Verification status

Both mod halves compile. The ASI imports Windows libraries only. Melty's check of the submitted release passes with all 424 archive entries mapped and no manual install steps. The Minecraft companion reached its world-ready message, and both games ran locally with the revised GTA adapter and no ScriptHookV loaded. The player confirmed movement, inventory/menu controls and F8, and a real gameplay screenshot was attached to the draft. They chose to quit after screenshots; the launcher recorded an error on that exit, so exit behavior and longer sessions need further testing. The Melty listing remains a draft pending full install-and-Play verification and publication approval.

The adapter fails closed on missing signatures/natives, requires the official -nobattleye argument and disables the mashup if an online session is detected. It does not modify or bypass BattlEye.

## Building

Visual Studio C++ x64 Build Tools: gta/build.bat and packaging/build-companion.bat. Java 25: mc/gradlew.bat build. MinHook 1.3.4 and ReShade 6.8 add-on headers are under gta/third_party with notices. packaging/build_release.py builds from explicit artifacts and named official dependency downloads; never copy a game install or player data into its stage. Build a fresh stage and rerun inspect_package, validate_recipe and one_click_check.

packaging/gta-config/ holds the ReShade configuration templates used for the release. LEGACY-INSTALL-0.1.0.md is a historical 0.1.0 reference, not the new release instructions. The project README lists the actual features and experimental limits.

## Player data

The account association and dedicated Minecraft world stay under LocalAppData/MinecraftGTAV/prism. Preserve that folder to keep builds. File destinations are in melty.json; remove release files with both games closed and restore previous ReShade configurations from the installer's backup. Shared loaders may be used by other mods. See included credits/licenses.

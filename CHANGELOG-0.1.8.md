# 0.1.8 — Ender Dragon combat and GTA mob collision

## Fixes and changes in this version

- Fixed the Ender Dragon staying still or circling without engaging targets while the GTA bridge is active. It now uses local target flight, turns toward targets, and performs committed swoops.
- Added active targeting of GTA people and nearby cars. Vehicle targets expire when GTA stops reporting them, so the dragon does not keep chasing stale positions.
- Added aimed dragon fireballs with a firing cooldown. The dragon fires when facing a target within range.
- Changed dragon fireballs from lingering breath clouds to explosions on Minecraft or GTA impacts. GTA receives the rocket-and-fire boss blast used by Wither skulls.
- Added destruction of GTA vehicles within 6 metres of dragon blasts, including direct hits on long vehicles whose centres are outside that radius.
- Fixed the start of dragon projectile traces using the player's position. Traces now start from the projectile's own position.
- Kept pending GTA impact traces across updates and consumed impacts once, preventing missed hits and duplicate explosions. Expired fireballs are cleaned up.
- Added individual GTA floor, wall, and ceiling collision samples for nearby Minecraft mobs, including passive mobs, the Wither, and the Ender Dragon. Samples account for each mob's body size and movement.
- Made mob movement use Minecraft's collision solver even when a boss normally enables `noPhysics`. The previous state is restored after movement, and GTA NPC stand-ins are excluded.
- Added collision freshness checks and bounded workloads: up to 64 nearby mobs within 96 metres, with a maximum of 32 new GTA probes per frame. Stale contacts are cleared.
- Added obstacle avoidance during dragon flight and restored vanilla dragon phases when the bridge is inactive or the dragon is dying.
- Fixed Melty's Java bytecode check by compiling the mod to Java 21 bytecode against the Java 25 APIs used by the frame bridge. Minecraft 26.3 and the frame bridge still require the bundled Java 25 runtime; the Fabric requirement remains Java 25.
- Standardized the mod, package, and release version as 0.1.8. Both Melty projects accept this version as test drafts with no blocking findings.

## Earlier combat fixes included in this build

- Crossbow firework bursts destroy nearby GTA vehicles, including direct hits on long vehicles. Timed bursts and Minecraft block impacts reach GTA.
- TNT explicitly destroys GTA vehicles within 8 metres of its normal blast, with the vehicle radius scaled for larger TNT explosions and capped at 16 metres.
- Player bow and crossbow arrows forward damage through Minecraft stand-ins to GTA NPCs. Stand-ins update even when no Minecraft mobs are nearby.
- Arrow impacts preserve Minecraft mob handling, clear stale tracking, and avoid repeated damage.
- Corrected missing vehicle and pedestrian enumeration native mappings that could stop the bridge after firing a rocket. Native mapping coverage is checked during packaging.

## Validation and remaining limits

The Java and C++ production builds passed. Native-mocked regression checks cover dragon car/person impacts, pending traces, duplicate suppression, body contact expiry, and earlier rocket/arrow behavior. An isolated Minecraft 26.3 runtime test verified dragon flight, swoops, shots, explosions, and wall collision for a zombie, Wither, and dragon. After the bytecode/version change, Minecraft 26.3 started and reached `Minecraft companion ready` with mod 0.1.8.

Full GTA street gameplay with this build still needs testing. Mob collision is sampled rather than a complete imported GTA collision mesh; crowds, thin objects, and fast moving surfaces can need further work. This is an experimental pre-release.

## Downloads and updating

`Minecraft-GTA-V-0.1.8-melty.zip` is the full playable package. The source ZIP is for development, and `melty.json` is its install recipe. Requires GTA V Legacy 1.0.3889.0 Story Mode and Minecraft Java 26.3.

For a manual update, close both games and replace the GTA ASI, its native mapping file, and the Minecraft passthrough mod jar. Remove the previous passthrough jar, keeping Fabric API and your worlds/accounts. Preserve the dedicated Minecraft instance and use the bundled Java 25 runtime.

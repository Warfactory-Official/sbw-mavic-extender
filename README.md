# SBW Mavic Drone Extender

An addon for Superb Warfare that lets you fly drones far past your normal view range. Inspired by me MCHELI CE UAV chunk streaming implementation.

Normally when you send a drone away from your body, the world around it stops loading and you just see empty sky through the camera. This mod fixes that. While you are looking through a linked monitor, the terrain and entities around your drone load and render properly, no matter how far it flies.

## What it does

- Shows the real world around your drone while you pilot it, instead of void.
- Loads nearby mobs, players, and other things so they appear in the drone view.
- Puts your own surroundings back to normal the moment you stop flying.
- No fake players, no teleporting of the player, actual proper camera detachment.

## Config

A config file lets you adjust two things:

- Max distance: how far a drone can travel before the HUD shows the out of range warning and red distance marker. You can set a default value and per vehicle overrides, so extra drones from other addons can each have their own limit.
- Verbose logging: extra diagnostic messages in the log. Off by default. Only turn it on if you are troubleshooting.

## Compatibility

### SBW Drone Warfare

If you also run the SBW Drone Warfare addon (`sbwdroneconfig`), Mavic automatically disables two of its features on startup because they solve the same problem a different way and would fight Mavic's camera:

- **Player anchor** (`enableDronePlayerAnchor`, on by default in that addon) — it teleports your real body onto the drone while you fly. Mavic detaches the camera without moving you, so the anchor is redundant and causes rubber-banding.
- **Drone chunk loading** (`enableDroneChunkLoading`) - it force-loads chunks around the drone, which Mavic already does through its own streaming.

You do not need to change anything by hand. When both mods are present, Mavic turns these off and logs a line to the console (`[Mavic] Detected SBW Drone Warfare; disabled its redundant drone-view workarounds ...`).

## Versions

Works on Minecraft 1.20.1 (Forge) and 1.21.1 (NeoForge). Requires Superb Warfare.

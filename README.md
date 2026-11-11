# HitColor

**Every hit in your color.** HitColor replaces the red flash on hit entities with any color you like – from a subtle see-through tint to a bold neon glow.

![Fabric](https://img.shields.io/badge/loader-Fabric-blue) ![MC](https://img.shields.io/badge/Minecraft-1.21.11-green) ![Side](https://img.shields.io/badge/side-client-orange) [![Build](https://github.com/noelshary/hitcoco/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/noelshary/hitcoco/actions/workflows/build.yml)

## Features
- 🎨 **Color picker** with saturation field + hue bar, 40 quick colors, HEX input and random button
- 🔆 **Strength slider** – from barely visible and transparent to fully solid
- ✨ **8 animations** – Static, Pulse, Fade out, Heartbeat, Rainbow, Shimmer, Fire, Strobe (with speed slider)
- 💥 **Hit particles** in your color
- 🔔 **Hit sounds** (Chime, Ping, Orb) with volume control
- 🤖 **Test bot** in the menu – shows your hit color live, press "Hit" to see the full hit effect
- ⌨️ Open with a **keybind** (default `H`) or through **Mod Menu**
- 💾 Settings are saved automatically (`config/hitcolor.json`)

## Client-side only
HitColor changes only what **you** see. It does not touch gameplay, works on any server and needs nothing installed on the server.

## Requirements
- Minecraft 1.21.11
- Fabric Loader + [Fabric API](https://modrinth.com/mod/fabric-api)
- Optional: [Mod Menu](https://modrinth.com/mod/modmenu)

## Build
```bash
gradle build --no-daemon
```

The GitHub Actions workflow builds the mod on pushes and pull requests. Download the compiled JAR from the workflow run's **Artifacts** section. Local build output is in `build/libs/`.

## License
MIT

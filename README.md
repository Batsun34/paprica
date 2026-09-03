<div align="center">

<img src="src/main/resources/assets/paprika/icon.png" alt="Paprika" width="128">

# Paprika

**A configurable Fabric client mod for Minecraft 1.21.4 with movement, combat, ESP and HUD utilities.**

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.4-62B47A?logo=minecraft)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Loader-Fabric-DBD0B4)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-21%2B-orange?logo=openjdk)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-Non--Commercial%20%2B%20Attribution-blue)](LICENSE)

</div>

Paprika is a client-side Minecraft mod built with Fabric. It combines movement tweaks, combat helpers, player/item visualization, customizable HUD elements and first-person visual controls in a single in-game menu.

> [!NOTE]
> **License:** non-commercial use and modification are allowed, including reuse of source-code fragments. Distributed derivatives that use Paprika code must preserve visible **`By Batsun`** attribution in their UI. Commercial use requires separate written permission.

> [!WARNING]
> Some Paprika features — especially Auto Attack, Extended Reach, No Knockback and movement modifications — may be prohibited by multiplayer servers or anti-cheat systems. Check the rules of the server you play on and use the mod at your own risk.

## Features

### Movement

- **Sneak Movement Speed** — changes horizontal movement while sneaking.
- **No Knockback** — suppresses horizontal knockback while the player is hurt.
- **Jump Boost** — adds configurable vertical velocity to jumps.

### Combat

- **Auto Attack**
  - configurable attack rate;
  - configurable maximum reach;
  - optional extended reach;
  - optional line-of-sight requirement;
  - optional aim following with adjustable smoothing;
  - optional crosshair-on-point requirement;
  - several target modes:
    - Circle;
    - Circle + Mark;
    - Marked Only;
    - All Nearby.
- **Target marking** — mark or clear a specific player target.
- **Friends list** — add players by name or directly from the crosshair.
- **Hit Counter** — displays confirmed hits per second with configurable position, scale, alpha and color.

### Visuals

- **Player ESP**
  - outline thickness;
  - glow;
  - multiple animated color modes;
  - saturation and animation speed controls.
- **Player Rays**
  - rays from the screen center or bottom;
  - configurable thickness, opacity and starting height;
  - glow and animated colors.
- **Armor / held item overlays**
  - show another player's armor and held item;
  - configurable placement, scale, opacity, color and glow.
- **Distance labels** — display distance to players.
- **Item Outline**
  - highlight dropped items;
  - All / Whitelist / Blacklist filtering;
  - configurable thickness and alpha;
  - glow;
  - Nick / Gradient / Rainbow / Solid / Item Average color modes.
- **Player Trails**
  - trails for yourself and/or other players;
  - Thin Line / Floating Line / Strip;
  - configurable origin, lifetime, height, alpha and colors.
- **Custom Sky**
  - separate top and bottom RGB colors;
  - optional rainbow animation.
- **First-person hand controls**
  - hide hands while holding an item;
  - change hand FOV;
  - X/Y offsets;
  - flip item;
  - left/right/default item orientation.

### HUD

- **Target Health** with optional dynamic coloring.
- **Player List** with configurable position, scale, height and opacity.
- **Player Doll** rendered in any screen corner with configurable size and offsets.
- **Hit Counter** for attacks per second.

## Requirements

| Component | Version |
| --- | --- |
| Minecraft | **1.21.4** |
| Java | **21+** |
| Fabric Loader | **0.18.4+** according to `fabric.mod.json` |
| Fabric API | Required |
| Mod environment | Fabric client |

The project currently compiles against Fabric API `0.118.5+1.21.4`.

## Installation

1. Install **Minecraft 1.21.4**.
2. Install a compatible **Fabric Loader**.
3. Install **Fabric API** for Minecraft 1.21.4.
4. Put the Paprika `.jar` into your Minecraft `mods` directory.
5. Launch the game with the Fabric profile.
6. Press **Right Shift** to open the Paprika menu.

Paprika stores its settings in:

```text
.minecraft/config/paprika.json
```

If an old `noknockback.json` config exists and `paprika.json` does not, Paprika can migrate the legacy configuration automatically.

## Default keybinds

All Paprika binds can be changed from the mod menu. They are also registered as Minecraft keybindings.

| Action | Default key |
| --- | :---: |
| Open Paprika menu | **Right Shift** |
| Sneak Movement Speed | **V** |
| No Knockback | **N** |
| Player ESP | **H** |
| Player Rays | **J** |
| Player List | **K** |
| Player Trails | **L** |
| Item Outline | **Y** |
| Auto Attack | **R** |
| Mark target | **M** |
| Clear marked target | **U** |
| Mark friend | **F** |
| Panic | **P** |

> [!IMPORTANT]
> **Panic** disables Paprika's active features, closes the menu and clears Paprika keybindings before saving the configuration. This includes the menu and panic binds themselves, so the binds must be configured again afterwards.

## Menu

The Paprika menu is split into five groups:

| Group | Modules |
| --- | --- |
| **Movement** | No Knockback, Sneak Movement Speed, Jump Boost |
| **Combat** | Auto Attack, Friends |
| **Visuals** | Player ESP, Items, Rays, Trails, View |
| **Overlay** | Target Health, Player List, Player Doll |
| **System** | Menu / keybinds / Panic |

The UI supports toggles, sliders, dropdowns, text inputs, buttons and direct key rebinding.

Paprika includes both **English** and **Russian** localization resources.

## Building from source

Clone the repository:

```bash
git clone https://github.com/Batsun34/paprica.git
cd paprica
```

### Windows

```powershell
.\gradlew.bat build
```

### Linux / macOS

```bash
chmod +x ./gradlew
./gradlew build
```

The compiled artifacts are written to:

```text
build/libs/
```

To launch a development Minecraft client through Fabric Loom:

```bash
./gradlew runClient
```

On Windows:

```powershell
.\gradlew.bat runClient
```

## Project structure

```text
paprica/
├── .github/workflows/
│   └── build.yml
├── src/main/java/paprika/
│   ├── PaprikaClient.java
│   ├── PaprikaConfig.java
│   ├── PaprikaMenuScreen.java
│   ├── PaprikaVisualSettingsScreen.java
│   └── mixin/
├── src/main/resources/
│   ├── assets/paprika/
│   │   ├── icon.png
│   │   └── lang/
│   │       ├── en_us.json
│   │       └── ru_ru.json
│   ├── fabric.mod.json
│   └── paprika.client.mixins.json
├── build.gradle
├── settings.gradle
└── LICENSE
```

### Main classes

- `PaprikaClient` — client initialization, module state, keybindings, tick logic, HUD and world rendering.
- `PaprikaConfig` — JSON configuration loading, sanitization, migration and persistence.
- `PaprikaMenuScreen` — main in-game configuration UI.
- `PaprikaVisualSettingsScreen` — additional visual settings UI.
- `mixin/*` — integrations with Minecraft rendering, networking, controls and interaction behavior.

## Development

The project uses:

- **Java 21 toolchain**;
- **Gradle** with the included wrapper;
- **Fabric Loom**;
- **Yarn mappings** for Minecraft 1.21.4;
- **Fabric API**;
- **Mixin** for client behavior and rendering hooks.

GitHub Actions automatically runs a Gradle build on pushes and pull requests and uploads files from `build/libs/` as workflow artifacts.

## Configuration

Most settings are persisted automatically whenever a value changes. The configuration includes:

- enabled/disabled states;
- keybinds;
- visual colors and animation parameters;
- HUD positions and scales;
- friends;
- item filters;
- auto-attack settings;
- trails;
- sky and first-person view settings;
- last opened menu state.

Manual editing of `paprika.json` is possible, but using the in-game menu is recommended because values are sanitized and clamped by the mod.

## License

Paprika is **source-available**, but it is **not open source under the OSI definition**.

The project is intended to be released under the **Paprika Non-Commercial Attribution License 1.0**, a project-specific license based on the principles of the **PolyForm Noncommercial License 1.0.0**.

In practical terms:

- you may use Paprika for personal and other non-commercial purposes;
- you may study, modify and redistribute the source code;
- you may reuse parts of Paprika's source code in your own non-commercial projects;
- **commercial use of Paprika source code, modified versions, or code derived from it is not permitted without separate written permission from Batsun**;
- any distributed user-facing project that uses Paprika source code, in whole or in part, must preserve visible attribution to the original author;
- the attribution must contain **`By Batsun`** and must be reasonably visible in the application's UI, such as the main menu, settings, credits/about screen, or another user-accessible interface;
- the attribution may not be removed, hidden, obscured, or presented in a misleading way.

Example attribution:

```text
By Batsun
```

or:

```text
Contains code from Paprika — By Batsun
```

The exact terms in [`LICENSE`](LICENSE) take precedence over this README.

> [!IMPORTANT]
> Commercial licensing can be granted separately by the copyright holder. If you want to use Paprika code in a commercial product, obtain explicit permission from Batsun first.

---

<div align="center">

Made for Fabric • Minecraft 1.21.4

</div>

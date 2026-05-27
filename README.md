# TC-VeinGlow

**TC-VeinGlow** is a highly customizable VeinMiner mod for Minecraft 1.21.1 (Fabric). It allows players to mine entire veins of ores or connected blocks efficiently at once, featuring advanced configurations like custom equation strategies, spread modes, and filter modes.

## Features

- **Vein Mining:** Mine an entire vein of ores or connected blocks simultaneously.
- **Custom GUI Integration:** Fully configurable in-game using ModMenu and Cloth Config.
- **Advanced Filtering:** Configurable filter modes to specify exactly which blocks should be mined.
- **Spread Modes:** Manage how the mining algorithm spreads to adjacent blocks.
- **Custom Equations:** Advanced equation strategies for block discovery and mining behavior.

## Requirements

- **Minecraft:** 1.21.1
- **Mod Loader:** Fabric (Loader `>= 0.15.11`)
- **Java:** 21
- **Dependencies:**
  - [Fabric API](https://modrinth.com/mod/fabric-api)
  - [Cloth Config API](https://modrinth.com/mod/cloth-config) (For in-game configuration GUI)
  - [ModMenu](https://modrinth.com/mod/modmenu) (To access the configuration screen)

## Building from Source

To build this mod from source, follow these steps:

1. Clone the repository.
2. Open a terminal in the project directory.
3. Run the following command:
   ```bash
   ./gradlew build
   ```
4. The built `.jar` file will be located in the `build/libs/` directory.

## License

This project is licensed under the MIT License.

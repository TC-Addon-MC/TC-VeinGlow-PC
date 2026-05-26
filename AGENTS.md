# AGENTS: How to be productive in this codebase

Checklist for an AI coding agent working on TC_VeinGlow_Java
- Read these key files before editing: `TCVeinMinerMod.java`, `MiningEngine.java`, `MiningQueue.java`, `MiningStateMachine.java`, `FilterModeManager.java`, `StrategyRegistry.java`, `ConfigManager.java`.
- Preserve server/client separation: server-only logic is in `src/main/java` (no client imports allowed in logic classes — see `HudNotifier.java`).
- Validate changes with a local Gradle build and run the game via Loom (`gradlew.bat`).

Big-picture architecture (short)
- This is a Fabric mod (Minecraft server & client plugin) using Fabric Loom. The mod's entrypoint is `TCVeinMinerMod.onInitialize()` which registers networking payloads and event handlers.
- Per-player orchestration lives in `MiningEngine` (one instance per UUID via ENGINES map). High-level flow: break event -> scanning -> enqueue -> mining tick loop -> finalize/interrupt. See `MiningStateMachine.java` for allowed transitions.
- Block collection rules and safety are centralized in `FilterModeManager` (filters + presets). Strategy implementations (e.g. `SpreadModeManager`, `TunnelModeManager`, `CustomEquationStrategy`) call into this filter pipeline.
- Strategy registration is static in `StrategyRegistry` — add new shapes by registering a MiningStrategy with a unique id (string IDs are authoritative; `ModConfig.MiningShape` maps to those ids).

Critical patterns and conventions
- Server-first safety: always check chunk loaded before reading state. See `MiningQueue.drainForTick()` and comments — do not force-load chunks.
- Re-entry guard: `MiningEngine.isMining` prevents recursion from AFTER-block-break events. Any change to break logic must preserve this guard (`isMining` set before tryBreakBlock calls and cleared in finally).
- Tool checks run per-tick (not only at trigger): `toolOk()` is called every server tick in `onServerTick`. Don't move that check to trigger-only.
- Config is JSON in Fabric config dir: `ConfigManager` reads/writes `tc_veinminer.json` using Gson. Server-authoritative constraints (maxBlocks clamped) are enforced in the packet handler in `TCVeinMinerMod.onInitialize()`.
- Network payloads use Fabric's `PayloadTypeRegistry` + custom `PacketCodec` records in `src/main/java/com/tcveinminer/network/*.java`. Keep codec symmetry when editing fields.

Developer workflows & commands
- Build (local):
  - Windows (PowerShell): `.
    .\gradlew.bat build
    `
  - Run client for manual testing (Loom): `.
    .\gradlew.bat runClient
    `
  - Run server: `.
    .\gradlew.bat runServer
    `
- Java version: toolchain set to Java 21 (see `build.gradle` tasks.withType(JavaCompile).configureEach { it.options.release = 21 }`).
- Import into IDE via Gradle (`settings.gradle` rootProject.name = 'tc-veinminer'). Use Gradle import to get Loom run configurations.
- No unit tests present — rely on `runClient` for integration testing and inspect `run/logs/latest.log` for runtime errors.

Integration points and external deps
- Fabric Loader, Fabric API, and Yarn mappings are configured in `gradle.properties` and `build.gradle`.
- Third-party mods used at runtime: Cloth Config (cloth-config-fabric) and ModMenu (see dependencies in `build.gradle`). Their API usage appears in client layers (not in server logic).
- Inter-mod communication points: `FilterModeManager.Registry.register(...)` allows other mods to add custom filters at runtime.

When making changes, be explicit about the following
- If adding a new MiningStrategy: update `StrategyRegistry.register(...)` and ensure the string ID matches any `ModConfig` default names. Example: `StrategyRegistry` registers "FACE"/"EDGES" etc.
- If modifying packet payloads: update the corresponding `PacketCodec` tuple in both server and client payload classes and keep IDs stable (`Identifier.of("tc_veinminer", "...")`).
- If changing queue or chunk logic: preserve the non-force-load ordering (check chunk loaded before calling `world.getBlockState`) and the polling snapshot behavior in `MiningQueue.drainForTick()`.

Quick file map (most important):
- Entrypoint & wiring: `TCVeinMinerMod.java`
- Per-player flow & rules: `MiningEngine.java`
- Scanning/queue: `MiningQueue.java`
- State machine: `MiningStateMachine.java`
- Filter & safety pipeline: `FilterModeManager.java`
- Strategy registry & templates: `StrategyRegistry.java`
- Config read/write: `ConfigManager.java`, `ModConfig.java`
- Network payloads: `src/main/java/com/tcveinminer/network/*.java`

If you need to run or debug: prefer `runClient` Gradle task and attach the debugger to the spawned JVM. Inspect `run/logs/latest.log` and `run/crash-reports` for stack traces.

Contact points for further changes
- Follow existing patterns in `FilterModeManager` for any new block-safety checks — it centralizes performance and safety ordering.
- Keep server-side authoritative checks in `TCVeinMinerMod` and `MiningEngine` (sanitization, clamp values, tool validation).
## Response rules
Keep the final response under ~100 words.

Do not write large paragraphs.

Use this format:
- Changed:
- Files:
- Notes:

Use short lines only.
End of AGENTS.md


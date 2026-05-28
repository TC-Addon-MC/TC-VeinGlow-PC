# TC-VeinGlow

**TC-VeinGlow** là mod Vein Mining cực kỳ linh hoạt cho Minecraft 1.21.1 (Fabric), được phát triển bởi ToanCao. Mod cho phép người chơi khai thác toàn bộ một mạch quặng hoặc chuỗi khối liên kết cùng lúc, với hệ thống engine mạnh mẽ, giao diện GUI tuỳ chỉnh phong phú, và nhiều chế độ đào / kỹ năng nâng cao.

## Tính năng nổi bật

- **Vein Mining:** Đào toàn bộ mạch quặng hoặc chuỗi khối liền nhau trong một thao tác.
- **Nhiều hình dạng đào (Mining Shapes):**
  - `FACE` – Standard (kề mặt)
  - `EDGES` – Standard V2 (kề cạnh)
  - `CORNERS` – Standard V3 (kề góc)
  - `TUNNEL_1x2`, `TUNNEL_3x3` – Đào hầm
  - `STAIR_UP`, `STAIR_DOWN` – Đào cầu thang
  - `AREA_3x3`, `AREA_5x5` – Đào vùng diện tích
  - `TREE_CAP` – Chặt cây toàn bộ
  - Hỗ trợ hình dạng **custom** qua biểu thức toán học (custom equation)
- **Hệ thống Skill:** BucketSkill, CropHarvestSkill, TreeCapitatorSkill, InteractSkill (bóc vỏ gỗ, cày đất, v.v.), BreakSkill.
- **Radial Menu:** Chọn nhanh hình dạng đào ngay trong game.
- **GUI tuỳ chỉnh đầy đủ:** Giao diện cài đặt gồm nhiều tab (General, Color, Filter, Skills, Shapes, Dashboard).
- **HUD Overlay:** Hiển thị thông tin đào trên màn hình.
- **Block Highlighter:** Tô sáng các khối sẽ bị đào (hỗ trợ màu tuỳ chỉnh, chế độ cầu vồng).
- **Bộ lọc khối (Filter Mode):** Cấu hình chính xác những khối nào được phép đào.
- **Config Server & Client riêng biệt:** Đồng bộ cấu hình qua mạng (C2S/S2C payloads).
- **Không cần ModMenu / Cloth Config** để chạy (là dependency tuỳ chọn để mở GUI cài đặt).

## Yêu cầu

| Thành phần     | Phiên bản                   |
|----------------|-----------------------------|
| Minecraft      | 1.21.1                      |
| Mod Loader     | Fabric Loader `>= 0.15.11`  |
| Java           | 21                          |
| Fabric API     | `0.102.0+1.21.1`            |
| ModMenu        | (Tuỳ chọn – mở GUI cài đặt)|

## Cấu trúc dự án

```
TC_VeinGlow_Java/
├── build.gradle
├── gradle.properties
├── settings.gradle
├── src/
│   ├── main/
│   │   ├── java/com/tcveinminer/
│   │   │   ├── TCVeinMinerMod.java              # Server entrypoint (ModInitializer)
│   │   │   ├── config/
│   │   │   │   ├── ModConfig.java               # Cấu hình server (MiningShape enum, settings)
│   │   │   │   └── ConfigManager.java           # Đọc/ghi config server
│   │   │   ├── engine/
│   │   │   │   ├── AbstractActionEngine.java    # Base engine chứa logic chung
│   │   │   │   ├── MiningEngine.java            # Engine chính điều phối toàn bộ quá trình đào
│   │   │   │   ├── action/
│   │   │   │   │   ├── ActionContext.java       # Context dữ liệu cho mỗi action
│   │   │   │   │   ├── ActionExecutorRegistry.java
│   │   │   │   │   ├── ActionType.java          # Enum loại action
│   │   │   │   │   ├── BlockAction.java
│   │   │   │   │   └── impl/
│   │   │   │   │       ├── BreakBlockAction.java
│   │   │   │   │       ├── FluidScoopAction.java
│   │   │   │   │       ├── HarvestCropAction.java
│   │   │   │   │       ├── HoeTillAction.java
│   │   │   │   │       ├── InteractBlockAction.java
│   │   │   │   │       ├── PlantAction.java
│   │   │   │   │       └── TreeCapAction.java
│   │   │   │   ├── capability/
│   │   │   │   │   ├── CapabilityRegistry.java
│   │   │   │   │   ├── ItemActionCapability.java
│   │   │   │   │   └── impl/
│   │   │   │   │       ├── BucketCapability.java
│   │   │   │   │       ├── HarvestCapability.java
│   │   │   │   │       ├── HoeCapability.java
│   │   │   │   │       ├── InteractBlockCapability.java
│   │   │   │   │       ├── PlantCapability.java
│   │   │   │   │       └── UseItemCapability.java
│   │   │   │   ├── filter/
│   │   │   │   │   ├── LeftClickFilterPipeline.java
│   │   │   │   │   ├── RightClickFilterPipeline.java
│   │   │   │   │   └── ValidationPipeline.java
│   │   │   │   ├── left/
│   │   │   │   │   └── LeftClickEngine.java     # Xử lý kích hoạt bằng chuột trái (đào khối)
│   │   │   │   ├── preview/
│   │   │   │   │   └── PreviewManager.java
│   │   │   │   ├── queue/
│   │   │   │   │   └── BlockActionQueue.java    # Hàng đợi action theo từng tick
│   │   │   │   ├── right/
│   │   │   │   │   └── RightClickEngine.java    # Xử lý kích hoạt bằng chuột phải (interact)
│   │   │   │   ├── session/
│   │   │   │   │   ├── ActionSession.java
│   │   │   │   │   └── ActionSessionManager.java
│   │   │   │   ├── skill/
│   │   │   │   │   ├── BreakSkill.java
│   │   │   │   │   ├── BucketSkill.java
│   │   │   │   │   ├── CropHarvestSkill.java
│   │   │   │   │   ├── InteractSkill.java
│   │   │   │   │   └── TreeCapitatorSkill.java
│   │   │   │   ├── state/
│   │   │   │   │   ├── EngineState.java         # Enum trạng thái engine
│   │   │   │   │   └── EngineStateMachine.java
│   │   │   │   ├── strategy/
│   │   │   │   │   ├── BaseBfsStrategy.java     # BFS cơ sở cho các strategy dạng vein
│   │   │   │   │   ├── CustomEquationStrategy.java
│   │   │   │   │   ├── FilterModeManager.java   # Quản lý bộ lọc khối đào
│   │   │   │   │   ├── MiningStrategy.java      # Interface strategy
│   │   │   │   │   ├── RotationManager.java
│   │   │   │   │   ├── ShapeModeManager.java
│   │   │   │   │   ├── SpreadModeManager.java
│   │   │   │   │   ├── StairModeManager.java
│   │   │   │   │   ├── StrategyRegistry.java    # Đăng ký & tra cứu strategy theo ID
│   │   │   │   │   └── TunnelModeManager.java
│   │   │   │   └── traversal/
│   │   │   │       ├── OrientationContext.java
│   │   │   │       ├── Traversal.java
│   │   │   │       └── TraversalUtils.java
│   │   │   ├── hud/
│   │   │   │   └── HudNotifier.java             # Gửi thông báo HUD từ phía server
│   │   │   ├── logic/                           # (dành cho mở rộng)
│   │   │   ├── mixin/                           # (dành cho mixin server nếu cần)
│   │   │   ├── network/
│   │   │   │   ├── ActivationConfirmPayload.java
│   │   │   │   ├── ActivationRequestPayload.java
│   │   │   │   ├── ConfigSyncPayload.java
│   │   │   │   ├── FilterResultPayload.java
│   │   │   │   ├── HighlightBlockListPayload.java
│   │   │   │   ├── HighlightDeltaPayload.java
│   │   │   │   ├── HoldKeyPayload.java
│   │   │   │   ├── LookedAtBlockPayload.java
│   │   │   │   └── MiningStatePayload.java
│   │   │   └── util/
│   │   │       ├── ExpressionEvaluator.java     # Parser biểu thức custom equation
│   │   │       └── SessionStats.java
│   │   └── resources/
│   │       ├── fabric.mod.json
│   │       ├── tc_veinminer.accesswidener
│   │       ├── tc_veinminer.mixins.json
│   │       └── assets/tc_veinminer/
│   └── client/
│       ├── java/
│       │   ├── com/tcveinminer/
│       │   │   ├── TCVeinMinerClient.java           # Client entrypoint (ClientModInitializer)
│       │   │   ├── ModMenuIntegration.java          # Tích hợp ModMenu
│       │   │   ├── config/
│       │   │   │   ├── ClientConfig.java            # Cài đặt phía client (màu sắc, HUD, v.v.)
│       │   │   │   └── ClientConfigManager.java
│       │   │   ├── gui/
│       │   │   │   ├── CustomButton.java
│       │   │   │   ├── radial/                      # (dành cho radial menu nếu tách riêng)
│       │   │   │   ├── screens/
│       │   │   │   │   ├── BlockListScreen.java     # Màn hình danh sách khối trong blacklist
│       │   │   │   │   ├── CustomShapeDesignerScreen.java  # Thiết kế hình dạng custom
│       │   │   │   │   ├── MainMenuScreen.java      # Màn hình cài đặt chính
│       │   │   │   │   ├── MenuState.java
│       │   │   │   │   ├── RadialDrawingUtils.java
│       │   │   │   │   ├── RadialMenuScreen.java    # Radial menu chọn hình dạng đào
│       │   │   │   │   ├── custom/
│       │   │   │   │   │   ├── GeometryGenerator.java
│       │   │   │   │   │   ├── RenderMesh.java
│       │   │   │   │   │   ├── ShapeAnalyzer.java
│       │   │   │   │   │   ├── ShapeType.java
│       │   │   │   │   │   └── VoxelRenderer.java
│       │   │   │   │   └── tabs/
│       │   │   │   │       ├── ColorTab.java        # Tab cài đặt màu sắc & hiệu ứng
│       │   │   │   │       ├── DashTab.java         # Tab Dashboard
│       │   │   │   │       ├── FilterTab.java       # Tab bộ lọc khối
│       │   │   │   │       ├── GeneralTab.java      # Tab cài đặt chung
│       │   │   │   │       ├── MenuTab.java
│       │   │   │   │       ├── ShapesTab.java       # Tab quản lý hình dạng đào
│       │   │   │   │       └── SkillsTab.java       # Tab bật/tắt các skill
│       │   │   │   └── widgets/
│       │   │   │       ├── AlphaSlider.java
│       │   │   │       ├── AmberButton.java
│       │   │   │       ├── MaxBlockSlider.java
│       │   │   │       ├── RGBSlider.java
│       │   │   │       ├── ThicknessSlider.java
│       │   │   │       └── TransitionTimeSlider.java
│       │   │   ├── hud/
│       │   │   │   └── VeinMinerHudOverlay.java     # Render HUD overlay trên màn hình
│       │   │   ├── logic/
│       │   │   │   └── BlockHighlighter.java        # Tô sáng khối sẽ bị đào (client-side)
│       │   │   └── util/
│       │   │       ├── ButtonDrawUtil.java
│       │   │       ├── ColorManager.java
│       │   │       ├── DrawHelper.java
│       │   │       ├── LayoutUtil.java
│       │   │       ├── PanelDrawUtil.java
│       │   │       ├── ThemeColors.java
│       │   │       └── ToggleDrawUtil.java
│       │   └── com/toancao/client/
│       │       └── TemplateModClient.java           # Template placeholder
│       └── resources/
│           └── template-mod.client.mixins.json
└── docs/
    └── agents/
```

## Kiến trúc tổng quan

```
Client (Player Input)
    │  giữ phím V → HoldKeyPayload (C2S)
    │  click → ActivationRequestPayload (C2S)
    ▼
Server (MiningEngine per player)
    ├── StrategyRegistry → chọn MiningStrategy theo shapeId
    ├── LeftClickEngine  → đào khối (break)
    ├── RightClickEngine → tương tác (interact, hoe, plant, bucket...)
    ├── ActionSession    → theo dõi tiến trình, timeout
    └── BlockActionQueue → thực thi từng block/tick
    │  HighlightBlockListPayload / HighlightDeltaPayload (S2C)
    ▼
Client (Rendering)
    ├── BlockHighlighter → vẽ outline các khối sắp đào
    └── VeinMinerHudOverlay → hiển thị HUD thông tin
```

## Network Packets

| Payload | Hướng | Mục đích |
|---------|-------|----------|
| `HoldKeyPayload` | C→S | Báo trạng thái giữ phím V + cấu hình hình dạng đào |
| `ActivationRequestPayload` | C→S | Yêu cầu kích hoạt / huỷ vein mining |
| `ActivationConfirmPayload` | S→C | Server xác nhận trạng thái kích hoạt |
| `ConfigSyncPayload` | S→C | Đồng bộ config server xuống client khi join |
| `MiningStatePayload` | S→C | Cập nhật trạng thái đào (đang đào / dừng) |
| `LookedAtBlockPayload` | S→C | Block player đang nhìn vào |
| `FilterResultPayload` | S→C | Kết quả lọc khối từ server |
| `HighlightBlockListPayload` | S→C | Danh sách đầy đủ khối cần highlight |
| `HighlightDeltaPayload` | S→C | Cập nhật delta (thêm/xoá) khối highlight |

## Skills

| Skill | Mô tả |
|-------|-------|
| `BreakSkill` | Đào khối cơ bản |
| `BucketSkill` | Múc / đổ chất lỏng hàng loạt |
| `CropHarvestSkill` | Thu hoạch mùa màng hàng loạt |
| `TreeCapitatorSkill` | Chặt toàn bộ cây + tự trồng lại |
| `InteractSkill` | Bóc vỏ gỗ, cày đất, tạo đường đi |

## Build từ source

```bash
# Clone repo
git clone <repo-url>
cd TC_VeinGlow_Java

# Build
./gradlew build
```

File `.jar` đầu ra nằm tại `build/libs/`.

## Cấu hình

### Server (`ModConfig`)
- `maxBlocks` – Số khối tối đa mỗi lần đào (mặc định: 128)
- `miningSpeed` – Số khối xử lý mỗi tick (mặc định: 3)
- `requireHarvestCapability` – Yêu cầu đúng loại dụng cụ
- `consumeDurability` / `consumeHunger` – Tiêu hao độ bền / đói
- `enabledShapes` – Danh sách hình dạng cho phép trong radial menu
- `blacklistedBlocks` – Danh sách khối bị cấm đào vein

### Client (`ClientConfig`)
- Màu sắc, độ dày outline, hiệu ứng chuyển tiếp
- Bật/tắt HUD, bật/tắt từng skill
- Thời gian transition, chế độ kích hoạt

## Giấy phép

Dự án được phát hành dưới giấy phép **MIT License**.

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
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── common/                          # Mã nguồn dùng chung cho tất cả các nền tảng
│   └── src/main/java/com/tcveinminer/
│       ├── engine/                  # Core logic đào, hệ thống skill, trạng thái engine
│       ├── event/                   # EventBus dùng chung
│       ├── network/                 # Các payload packet (C2S/S2C) và handler
│       ├── platform/                # Service loader (PlatformHelper, ClientBridge)
│       └── util/                    # Tiện ích dùng chung
├── client/                          # Mã nguồn giao diện (Client-side) dùng chung
│   └── src/main/java/com/tcveinminer/client/
│       ├── config/                  # Cài đặt cấu hình phía client
│       ├── gui/                     # Giao diện màn hình cài đặt, widgets, tabs
│       ├── hud/                     # HUD overlay hiển thị thông tin
│       ├── logic/                   # Block Highlighter (tô sáng khối)
│       ├── network/                 # Xử lý mạng phía client
│       └── platform/                # Triển khai ClientBridgeImpl
├── fabric/                          # Triển khai cụ thể cho Fabric Loader
│   └── src/main/java/com/tcveinminer/fabric/
├── forge/                           # Triển khai cụ thể cho Forge
│   └── src/main/java/com/tcveinminer/forge/
└── neoforge/                        # Triển khai cụ thể cho NeoForge
    └── src/main/java/com/tcveinminer/neoforge/
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

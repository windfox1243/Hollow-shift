# Phân tích kỹ thuật bản export Construct 3 (mục đích nghiên cứu)

Tài liệu này mô tả **cách game gốc được xây dựng**: kiến trúc, thuật toán, tham số. Mục đích là học hỏi để thiết kế một game khác (HOLLOW SHIFT, xem `DESIGN.md`).
Tài liệu không chứa map, text hay asset của bản gốc. Bản dump đầy đủ do `C3Inspect` sinh ra nằm ở `build/c3-dump.txt`. File này chỉ dùng cục bộ và không phát hành.

Tạo lại bản dump:
```
java tools/Build.java inspect ../www build/c3-dump.txt
```

---

## 1. Đóng gói & định dạng dữ liệu

| Thành phần | Vai trò |
|---|---|
| `COBB CAN MOVE.exe` + `WebView2Loader.dll` | Wrapper Windows của Construct 3, mở `www/index.html` trong Edge WebView2 |
| `package.json` | Cấu hình cửa sổ 640×360, cờ Chromium (autoplay audio) |
| `www/data.json` | Toàn bộ project đã biên dịch: object type, layout, event sheet, danh sách audio |
| `www/scripts/objRefTable.js` | Bảng tên plugin/behavior/ACE. Event tham chiếu ACE bằng **chỉ số** trong bảng này |
| `www/scripts/c3runtime.js` | Runtime C3. Cuối file có `self.C3_ExpressionFuncs = [...]`: mọi biểu thức trong event được biên dịch thành closure JS, event tham chiếu bằng chỉ số |
| `www/scripts/project/javaScriptInEvents.js` | Đoạn JS viết tay nhúng trong event (sắp xếp z-order, tách quái) |
| `www/constants.json` | Số biến thể chunk, danh sách rule, tip |
| `www/images/*.webp`, `www/media/*.webm` | Spritesheet và âm thanh (Opus/WebM) |

### Cấu trúc `data.json → project[]` (mảng theo vị trí)

| Index | Nội dung |
|---|---|
| 0 / 1 | Tên project / layout đầu tiên |
| 3 | Object types: `[name, pluginIndex, ...]` |
| 4 | Families: `[familyIndex, memberIndex...]` |
| 5 | Layouts: `[name, w, h, ..., eventSheet@7, ..., layers@9]`. Layer: `[name, ..., instances@14]` |
| 6 | Event sheets: `[name, events[]]` |
| 7 | Audio: `[path, [[mime, ext, size]], ...]` |
| 10 / 11 | Kích thước viewport |

### Mã hoá event (đã giải mã trong `C3Inspect`)

| `ev[0]` | Loại | Bố cục |
|---|---|---|
| 0 | Event block | `[0, _, _, _, sid, idx, conditions@6, actions@7, children@8]` |
| 1 | Biến | `[1, name, type, initial, isStatic, isConst, sid@6, ...]` |
| 2 | Include sheet | `[2, sheetName, _]` |
| 3 | Group | `[3, [active, name], ..., conds@6, actions@7, children@8]` |
| 4 | Function | `[4, [name, retType, params[], ...], ...]`. Param có cùng dạng với biến |
| 5 | Comment/"else-if" placeholder | rất ít gặp |
| 6 | Custom action (gắn object) | `[6, [_, name, objectIndex, _, params[]], ...]` |

- Condition: `[objIndex (-1 = System), aceIndex, behavior, _, isTrigger, isInverted@5, ..., params@9]`.
- Action: `[objIndex, aceIndex, behavior, sid, _, _, params@6]`. `objIndex = -2` nghĩa là gọi function theo tên.
- Param: `[kind, value]`.
  - kind 11: tham chiếu biến theo sid.
  - kind 4: object type.
  - kind 8: toán tử so sánh (0 `=`, 1 `!=`, 2 `<`, 3 `<=`, 4 `>`, 5 `>=`).
  - kind 0/1/7: biểu thức `[exprFuncIndex, node...]`. Node `[3, sid]` là biến, `[4, n]` là literal.

## 2. Luồng màn hình (layout)

```
splash → menu ─┬─ story ──────────┐
               ├─ endless (seed) ─┼→ rules → game ─┬─ thắng → nextLevel → rules → ...
               └─ level select ───┘                └─ chết  → dead → (thử lại | menu)
level 7 (story) → "game finale" (layout riêng 1600×1600) → story end → credits
```
- Mỗi layout gắn một event sheet. `globalEV` được include ở mọi nơi: input, save, audio helper, fade.
- `game` và `game finale` dùng chung `gameEV`. Phân biệt bằng `LayoutName` và biến `finale`.
- Layout `chunks` (5880×1720) là **kho template**. Mỗi chunk là một vùng 320×320 có tilemap và các object đánh dấu (điểm spawn item/breaker). `instances` là kho prefab.

## 3. Sinh bản đồ (`genEV`)

Tham số theo `level`:
- `mapWidth = mapHeight = min(2 + level, 30)`
- `minimumRooms = w*h/2`
- `CHUNK_SIZE = 320`

Thuật toán:
1. Mảng `w×h`. Ô bắt đầu đặt ở `(floor(w/2), 0)`, đánh dấu `startRoom`.
2. Cột bên dưới ô bắt đầu được đánh dấu `conveyor`, tạo thành "xương sống" dọc.
3. **Drunk walk**: đi ngẫu nhiên 4 hướng, kẹp trong biên, với `10 + w` bước. Xuất phát từ hai bên ô start, đánh dấu `visited`.
4. Lặp: nếu số ô `visited < minimumRooms` thì drunk walk tiếp từ một hàng ngẫu nhiên trên cột giữa.
5. Mỗi ô khác 0 sinh một chunk:
   - chuỗi kết nối gồm các ký tự `N/E/S/W` theo hàng xóm khác 0;
   - template là `<conn>_<random(variants)>`, số biến thể lấy từ `constants.chunkVariants`;
   - ô conveyor luôn có `S` và dùng template `conveyor_*`.
6. Quanh ô start, phía nào trống thì đặt chunk một cửa để bịt lối. Cuối cột conveyor đặt `conveyor_N`.
7. `genItems`: chọn ngẫu nhiên các marker trong chunk (`envItemChance`, `envBreakerChance`), mỗi marker dùng một lần.
   - Số coal: `taskCoal*1.5`, với `taskCoal = min(2+level, 50)`. Chỉ đặt ở marker cách start ≥ 70px.
   - Số breaker: `clamp(level-2, 0, 15)`.
   - Carrot: `6 + 2*level`, chỉ khi có rule starve.
   - Candle: `15 + 3*level`. Rock: `10 + 3*level`.

Nhận xét: cách làm này đảm bảo liên thông nhờ drunk walk luôn đi qua ô kề. Kết nối được suy từ **hàng xóm có tồn tại**, không lưu cạnh, nên mọi ô kề nhau đều thông nhau và map thường nhiều vòng.

## 4. Vòng chơi & mục tiêu
- Story: mang coal (vật nhặt được) tới furnace cho đủ `taskCoal`, gạt đủ `taskBreaker` breaker. Đủ cả hai thì sang level tiếp và lưu `story.level`.
- Finale: cắm battery vào các port của lò lớn. Mỗi lần nhặt battery thì thêm một rule cho quái. Đủ battery thì "Survive!".
- Chết theo `deathReason`: `monster | starve | freeze | furnace`.

## 5. Quái vật (`gameEV › monster`)

Biến instance chính:
- `state`: `idle | follow | reach | grabbedPlayer | stunned`;
- `targetX/Y`;
- `aggro` (var 7), giảm dần khi mất dấu;
- `speedScale = 1 + 0.05*level`;
- `distToPlayer`.

Hành vi theo trạng thái:
- **idle**, theo chu kỳ "findInterest":
  - Nếu có rule *smell* và cách người chơi < 1000px: target = vị trí người chơi + sai số tỉ lệ với khoảng cách, phát tiếng "sniff". Chu kỳ là `clamp(d/120, 1, 8)` giây. Nếu d < 300 thì chuyển sang follow.
  - Nếu không: chọn ngẫu nhiên một chunk làm điểm đến, chu kỳ 5–10 giây.
- **Di chuyển theo bước** (tween từng bước, không chạy liên tục), xen kẽ chân trái/phải. Mỗi bước có camera shake giảm theo khoảng cách và âm stomp.
  - Bước được gọi bằng `cobb.step(dist, duration, angleTo(target))`. Vị trí được tween thẳng mà **không kiểm tra va chạm tilemap**, nên quái đi xuyên tường giữa các phòng.
  - Lúc idle: `dist = random(20,40) + min(2*level, 15)`, `duration = random(0.3,0.7)`, nhịp bước `random(0.5,1.6)/speed`.
  - Lúc follow: `dist = 40`, `duration = 0.3`, nhịp bước `0.4/(speed+bonus)`. Sau 25 bước thì `bonus += 0.01` mỗi bước, tối đa 0.3.
  - Bàn tay (handTarget) trái/phải luân phiên được tween tới trước thân `dist + 60..80`. Khi tay chạm đất thì có shake và stomp.
- **see**: `checkSee` gọi mỗi bước. Nếu `d < 370 * visibility` thì follow. Khi đang follow, nếu `d < 700 * visibility` thì cập nhật target và tăng aggro. Nếu không thì target bị nhiễu và aggro giảm. aggro ≤ 0 thì quay về idle.
  - `visibility` của người chơi (0..1) = cường độ của nguồn sáng mạnh nhất tại vị trí người chơi, suy giảm tuyến tính theo khoảng cách tới tâm đèn.
- **hear**: `createNoise(x, y, radius)`. Trong bán kính `radius` thì target = vị trí noise. Trong `radius/2` thì follow.
  - Bán kính: bước chân 175 (đi trên cỏ thì 0), nhặt đồ 500, đá rơi lớn hơn.
  - HUD có chỉ báo âm lượng bằng opacity `radius/7.5` và rung khi lớn.
- **reach**: d < 80 thì vào trạng thái vươn tay. Tay dùng **IK hai đoạn** với góc khuỷu = `acos((2L² − d²)/(2L²))`. Tay chạm người chơi thì người chơi bị kéo tới mặt và chết.
- **duplicate**: sinh nhiều quái. Mỗi frame tách chúng bằng **circle separation** bán kính 64, đẩy mỗi bên một nửa.
- Đá ném trúng quái: **stun** 1 giây (recoil, đổi màu), sau đó follow tới điểm giữa.

## 6. Người chơi
- Di chuyển 8 hướng. Animation bước có frame tag `step` để phát âm thanh và tạo noise.
- **starve**: `hunger -= dt/20`. Về 0 thì chết. Ăn carrot để hồi.
- **freeze**: nếu `visibility < 0.5` thì `cold += 0.2*dt` (tối đa 3), ngược lại `cold -= 0.6*dt`. Tốc độ giảm `30*cold`. Đạt 3 thì chết. Tức là ánh sáng vừa giữ ấm vừa làm lộ vị trí: đây là **xung đột cốt lõi** của thiết kế.
- Cầm/thả/ném: chỉ cầm 1 vật. Có glyph báo vị trí thả hợp lệ (không đè tilemap). Rock thì ném theo tween parabol.

## 7. Sinh luật (`rulesEV`)
- Level 1 chỉ có luật cơ bản.
- `ruleCount = clamp(floor(level*0.8), 0, 6)`.
- Level > 1 bảo đảm có 1 trong {see, smell, hear}. Level > 5 thêm duplicate.
- Phần còn lại bốc ngẫu nhiên: chủ thể "you" xác suất 1/3, "monster" 2/3. Không bốc trùng (thử tối đa 1000 lần).
- Thời gian chờ màn rules tăng theo số luật.

## 8. Kỹ thuật trình bày
- **Z-order theo y** có hỗ trợ phân cấp cha/con: `zValue = parentZ + childIndex / 10^depth` (JS tay).
- Camera bám bằng lerp độc lập framerate: `lerp(a, b, 1 − 0.1^dt)`.
- Camera shake có biên độ = `magnitude / distance * 10`, kẹp ≤ 15.
- Nhạc đuổi: volume = `clamp(80/d)` với quái đang follow gần nhất, lerp mượt. Ambience to dần theo độ sâu (y).
- Lighting: mỗi object thuộc family `lighting` sinh một sprite `light` con với kích thước/độ mờ theo instance var, vẽ trên layer `lighting`.

## 9. Lưu trữ & input
- LocalStorage JSON: tiến trình story, âm lượng, cài đặt.
- Keybind theo bảng `binds.kbm[action][i]` dùng `KeyboardEvent.code`. Tên phím hiển thị lấy từ `navigator.keyboard.getLayoutMap()`.

---

## 10. Bài học áp dụng cho HOLLOW SHIFT (Java)

| Ý tưởng chung của thể loại | Cách HOLLOW SHIFT làm khác |
|---|---|
| Lưới chunk + drunk walk | Spanning tree ngẫu nhiên (DFS) + thêm vòng có kiểm soát; cạnh lưu tường minh; chunk ASCII tự thiết kế 15×15 tile |
| Rule bật/tắt giác quan | Bộ luật riêng: Sight / Hearing / Smell / **Tremor** / **Echo**; Battery / Cold / Hunger |
| Quái bước theo nhịp, đi thẳng xuyên tường, hai tay IK chống đất, gần thì chộp | **Giữ nguyên cơ chế** (theo yêu cầu): sải bước tween thẳng tới mục tiêu không va chạm tường, tay trái/phải xen kẽ đặt trước thân, chạm đất thì có tiếng dậm và rung, đuổi lâu thì nhanh dần, dưới 60px thì chộp rồi kéo người chơi vào. Thân và tay là thiết kế riêng |
| Ánh sáng vừa cứu vừa hại | Giữ xung đột này qua đèn lồng hao pin + luật Cold |
| Mục tiêu: chở vật về một điểm | Lắp cầu chì vào hộp điện rải rác, rồi chạy về thang máy |
| Finale: cắm pin vào lò | Finale: phòng thang máy, giữ 3 công tắc trong khi chờ thang |
| Y-sort, camera lerp, shake theo khoảng cách | Cài lại tổng quát trong engine Java (`Renderer`, `Camera`) |

# HOLLOW SHIFT – thiết kế

Top-down survival horror, 640×360 pixel art, **100% Java SE** (Swing/Java2D + javax.sound), chạy offline.
Ý tưởng thể loại được nghiên cứu ở `RE_ANALYSIS.md`. Nội dung (map, phòng, luật, cốt truyện, text) là của riêng dự án này.

## Vòng chơi
1. **Briefing**: mở đầu ca bằng lời dẫn, sau đó công bố từng luật.
2. **Ca làm việc**:
   - tìm cầu chì và lắp vào các hộp điện (mỗi lần lắp sẽ phát tiếng ồn lớn);
   - khi đủ điện thì quay về thang máy và đi lên.
3. **Ca cuối**: map vẽ tay. Kéo 3 cầu dao chính rồi sống sót 45 giây chờ thang. Trong lúc chờ, sinh vật nhanh hơn 15%.
4. Chết thì hiện nguyên nhân và một mẹo. Có thể chơi lại ngay.

## Luật (enum `rules.Rule`)
| Luật | Chủ thể | Cài đặt |
|---|---|---|
| SIGHT | Tenant | `SightSense`: đường nhìn + độ sáng tại vị trí người chơi. Flare đang cháy làm mồi nhử |
| HEARING | Tenant | `HearingSense`: nghe noise trong bán kính; ở dưới nửa bán kính thì đuổi |
| SMELL | Tenant | `SmellSense`: đánh hơi theo chu kỳ, đoán vị trí có sai số; có âm báo |
| CONSUME | Tenant | `WallMaw`: tổ xúc tu trên vách, đứng sát là bị ghim vào tường và **gọi quái tới** (ở xa thì nó xuyên đá ra cửa phòng đó), bấm E liên tục để giật ra (5 giây là bị nuốt); `FleshNode`: cục thịt dưới sàn, rải nhiều, hay nằm ngay cạnh đồ cần nhặt và ở cửa phòng, giẫm phải là nó biết chỗ bạn |
| ECHO | Tenant | `EchoSense` + `EchoState`: tiếng động lớn kéo nó dịch chuyển tới |
| HASTE | Tenant | Tốc độ tăng 7% mỗi phút (tối đa +50%) |
| BATTERY | Bạn | `BatteryEffect`: đèn lồng hao pin, nhặt cell để sạc |
| COLD | Bạn | `ColdEffect`: ở trong tối thì lạnh dần. Đèn lồng chỉ làm chậm lại; heater/đèn/flare sưởi ấm |
| HUNGER | Bạn | `HungerEffect`: đói dần, ăn đồ hộp |

Không có luật giác quan nào thì Tenant chỉ đi lang thang, nhưng chạm vào vẫn chết.

## Tenant: khối thịt có xúc tu (giữ cơ chế di chuyển của game gốc)
- **Hình dáng:**
  - một khối thịt gần như đen, gồ ghề, có các bọc phình lên xẹp xuống, gai xương đâm ra ở lưng, gân đỏ mờ;
  - miệng hình vòng như miệng cá mút đá, có vòng răng chĩa vào trong, há rộng khi đi săn;
  - cụm mắt li ti chớp lệch nhịp nhau.

  Tất cả được vẽ bằng code mỗi khung hình (`entity.Creature`).
- **9 xúc tu** (`entity.Tentacle`): mỗi xúc tu là một chuỗi 9 khớp giải bằng FABRIK, thon dần về đầu, có ngạnh và móc xương. Thân xúc tu có gợn sóng nên không bao giờ trông cứng đơ.
- **Sải bước, xuyên tường** (cơ chế gốc). Mỗi bước, thân bị kéo một đoạn thẳng về mục tiêu (chậm lúc đầu, rồi vọt lên), không va chạm tường.
  - Hai xúc tu chính luân phiên quất ra bám nền phía trước. Mỗi lần bám là một tiếng dậm và một cú rung camera.
  - Bảy xúc tu phụ bám quanh thân và buông ra để bám chỗ khác khi bị kéo căng.
- **Hai kiểu bước** (`ai.Gait`):
  - ROAM: 15–30px cộng thêm theo ca, nhịp chậm, không đều;
  - CHASE: 30px mỗi 0.4s, nhanh dần sau 25 bước (tối đa +30%).
- **Chộp** (`ai.ReachState`): khi cách dưới 80px, nó quất xúc tu vào người chơi. Trúng thì mọi xúc tu quấn lấy người chơi và kéo vào miệng. Ném đá trúng sẽ làm nó choáng.

## Hiệu ứng sợ hãi (`game.FearOverlay`)
- Quái càng gần thì mức sợ càng tăng (bắt đầu từ 240px, tăng nhanh hơn khi nó đang săn).
- Tim đập nhanh dần. Mỗi nhịp tim:
  - viền đỏ quanh màn hình co lại;
  - camera giật nhẹ.
- Khi quái rất gần thì màn hình chập chờn tối và xuất hiện vệt rách.

## Ngôn ngữ
- Có tiếng Anh và tiếng Việt (Settings → Ngôn ngữ/Language).
- Mọi chữ hiển thị đều nằm trong `resources/lang/en.json` và `vi.json`, gồm cả cốt truyện, luật, mẹo và đoạn kết. Test đảm bảo hai file có cùng bộ khóa và cùng các biến `{0}`.
- Bản tiếng Việt được viết lại theo giọng truyện kinh dị (gọi sinh vật là "nó"), không dịch từng chữ.
- Font pixel không có dấu tiếng Việt, nên chế độ tiếng Việt dùng Tahoma (hoặc Dialog), tắt khử răng cưa cho chữ vẫn sắc.

## Cốt truyện (hé lộ dần)
- Bạn là thợ ca đêm ở Hầm 9. Tổ ca ngày xuống hầm rồi không ai lên lại. Bộ đàm chỉ còn tiếng rè và những tiếng gõ đều đều từ trong vách đá.
- Mỗi ca có một lời dẫn ngắn. Mỗi ca story có thêm **2 mẩu giấy** (`entity.Note`) nằm ở các phòng sâu nhất. Đọc thì game tạm dừng và hiện tờ giấy cũ giữa màn hình.
- Các mẩu giấy ghép dần thành câu chuyện:
  - tiếng gõ trong vách;
  - thợ mỏ cũ để đèn sáng như "trả tiền trọ" cho "khách" dưới hầm;
  - vụ ngập năm 1991: 14 người xuống, chỉ tìm thấy 13, người thứ 14 vẫn được chấm công;
  - phần cơm để thừa cho một người không có tên;
  - từ ca 4 trở đi xuất hiện những dòng viết bằng chính nét chữ của bạn.
- **Luật ngầm:** ngọn núi luôn có đúng một "khách trọ", tức một linh hồn bị nhốt trong cơ thể của nó.
  - Người thứ 14 năm 1991 đã ở trong đó suốt 30 năm. Chính hắn là tiếng gõ trong vách, là người viết những mẩu giấy cảnh báo.
  - Ai đã xuống hầm thì bị ghi tên vào ca sau.
- **Kết thúc 1 (đi lên):** bạn sống sót nhưng vẫn kẹt trong vòng lặp, vì sổ giao ca ngày mai đã ghi sẵn tên bạn.
- **Kết thúc 2 (ẩn, ở lại):**
  - Điều kiện: đọc đủ 10 mẩu giấy (lưu qua mọi lần chơi), sau đó ở ca cuối, khi thang đã tới, giữ phím ở **vết nứt** (`entity.Crack`) cuối hầm phía tây. Từ vết nứt định kỳ vọng ra ba tiếng gõ.
  - Diễn biến: bạn thế chỗ người thứ 14. Hắn được lên, tên hắn bị gạch đi. Bạn trở thành tiếng gõ và người viết giấy cho người thợ kế tiếp.
  - Chưa đọc đủ giấy thì vết nứt chỉ hiện dòng "Ba tiếng gõ, rồi im lặng."
- Các ca không có tiêu đề riêng, chỉ đánh số "Ca 1…4" và "Ca cuối".

## Không khí: tối và chưa biết
- Phần lớn đèn tường đã chết hoặc chập chờn, kể cả ở phòng thang máy. Đèn còn sáng thì nhỏ và mờ.
- Lò sưởi chỉ đặt nhiều khi có luật COLD.
- Hộp cầu chì có điện chỉ le lói, thang máy chỉ lóe một đốm, đèn lồng 88px.
- Tenant:
  - xuất hiện ở phòng xa người chơi nhất (tính theo khoảng cách thật và độ sâu);
  - **chưa có mặt trong 10 giây đầu**, chỉ báo hiệu bằng một tiếng động vọng từ xa khi nó tới;
  - gần như không tự phát sáng;
  - mắt chỉ hiện khi ở trong 190px, và khi chưa đuổi thì chỉ thỉnh thoảng lóe lên.

## Âm thanh rượt đuổi và cảnh hù
- Khi Tenant phát hiện ra bạn, nhạc `chase_found` (nhạc chủ dự án cung cấp) phát lại từ đầu và lặp suốt cuộc rượt. Mất dấu khoảng 1.5 giây thì nhạc nhỏ dần rồi tắt, nhạc nền vặn nhỏ đi trong lúc đó.
- Cảnh hù dùng `scare_main` (cũng do chủ dự án cung cấp). Tiếng đập đầu tiên rơi đúng khoảnh khắc mặt giật vào màn hình. Cảnh kéo dài 2 giây.

## Ánh sáng (giống bản gốc, xem RE_ANALYSIS §8)
- Ngoài vùng đèn là **đen hoàn toàn**. Lớp lighting của bản gốc là nền đen đặc, mỗi đèn khoét một lỗ sáng.
- Mỗi nguồn sáng có `cường độ` và `bán kính`, tỉ lệ theo bản gốc (phòng 320px của bản gốc tương ứng 240px ở đây):
  - quầng sáng của người chơi: 22px, 0.5. Quầng này không tính vào độ lộ diện;
  - đèn lồng ~ cây nến: 112px;
  - Tenant tự phát sáng mờ: 38px, 0.2.
- Độ lộ diện = cường độ của đèn sáng nhất tại vị trí người chơi × (1 − khoảng cách/bán kính). Đây cũng là công thức của bản gốc.

## Độ thông minh của Tenant so với quái gốc
| | Bản gốc | Tenant |
|---|---|---|
| Thị giác | khoảng cách < 370×độ sáng (thấy), < 700×độ sáng (giữ đuổi), **không cần đường nhìn** | 280×độ sáng / 525×độ sáng (theo tỉ lệ map), không cần đường nhìn |
| Thính giác | bước chân 175, nhặt đồ 500; trong bán kính thì đến xem, trong nửa bán kính thì đuổi | đi bộ 130, chạy 230, nhặt đồ 375, cầu chì 340; cùng quy tắc |
| Khứu giác | trong 1000px, đánh hơi mỗi d/120 giây (1–8s), dưới 300 thì đuổi | trong 750px, mỗi d/90 giây (1–8s), dưới 225 thì đuổi |
| Bỏ cuộc | "aggro" khoảng 2s sau khi mất dấu | 2.5s, sau đó lục soát quanh điểm cuối |
| Thêm | — | CONSUME, ECHO, HASTE, bị flare dụ, lục soát, ném đá trúng thì choáng |

## Jumpscare (bật/tắt trong Settings)
Một nhịp tĩnh 0.14s (mặt ở xa trong bóng tối), rồi mặt giật vào màn hình. Có chớp trắng/đỏ, hàm há dần, ngón tay bám hai góc, lệch màu RGB, nhiễu và rách dòng quét, cuối cùng vỡ hình thành màu đen. Mặt quái trong cảnh hù là miệng hình vòng khổng lồ, có nhiều vòng răng lởm chởm xoáy vào cổ họng, xúc tu quất vào từ bốn góc. Âm thanh là `scare_main`, xem phần "Âm thanh rượt đuổi và cảnh hù".

## Hiệu năng
Đo bằng `hollowshift.tests.Bench` trên map endless ca 9, tính cho mỗi khung hình:

| | Trước | Sau |
|---|---|---|
| Render | 5.3 ms | 1.5 ms |
| Update | ~0.01 ms | ~0.01 ms |

Cách làm:
- Tilemap được bake sẵn thành các khối ảnh 256px.
- Lighting tính trực tiếp trên mảng `int[]` thay vì dùng gradient và bilinear của Java2D.
- Danh sách nguồn sáng được cache.

## Kiến trúc & mẫu thiết kế OOP
- **Kế thừa**:
  - `Entity` → `Player`, `Creature`, `Item` (→ `Carryable` / `Consumable`), `Fixture`;
  - `Scene` → các màn hình;
  - `Objectives` → `FuseObjectives`, `FinaleObjectives`.
- **Interface**: `Interactable`, `LightSource`, `Sense`, `StatusEffect`.
- **Strategy**: các `Sense` và `StatusEffect` được lắp ráp theo bộ luật của ca.
- **State**: AI của Tenant gồm `WanderState`, `InvestigateState`, `ChaseState`, `ReachState`, `StunnedState`, `EchoState`.
- **Observer/Event bus**: `NoiseBus` gom tiếng ồn mỗi tick cho các giác quan và cho HUD.
- **Template Method**: `Objectives` dùng chung chuỗi thoát bằng thang máy.
- **Data-driven**:
  - phòng ở `resources/chunks/rooms.txt`;
  - ca cuối cũng được sinh ngẫu nhiên như các ca khác (cầu dao thay cho hộp cầu chì, thêm khe nứt của kết thúc ẩn);
  - chiến dịch, mẹo, đoạn kết ở `resources/levels.json`.

## Sinh map (`world.WorldGenerator`)
- Mọi phòng đều nằm trên một vòng, không có ngõ cụt (cầu chì trong ngõ cụt mà quái chặn cửa là chết chắc):
  1. đặt khung vòng, chọn ngẫu nhiên: một vòng chữ O, hai vòng chung cạnh (số 8), vòng hành lang bao quanh một gian trung tâm có 2–4 nhánh nối vào, hoặc một vòng nhỏ 2×2 để mọc tự do;
  2. mọc thêm phòng bằng các "tai" (ear): chuỗi 1–4 phòng mới nối vào bản đồ ở **cả hai đầu**, nên đồ thị luôn liên thông 2 cạnh;
  3. thêm vài cửa tắt giữa các phòng kề nhau;
  4. phòng thang máy là phòng cao nhất, gần giữa nhất.
- Kích thước lưới mỗi ca được nới/xoay ngẫu nhiên theo seed, nên cùng một ca cũng có hình dạng khác nhau.
- Mỗi phòng là một template 13×13, có thể lật X/Y và xoay (8 biến thể mỗi template). Generator bao tường và mở cửa 3 ô theo các cạnh nối.
- Sau khi dựng phòng, `WorldBuilder` bày đồ theo công dụng của phòng (`RoomTheme`, suy ra từ tên mẫu phòng): kho có thùng phuy, bao tải, rương; xưởng có đe, giá dụng cụ, cuộn cáp; khu nghỉ có tủ khóa, bàn, kệ sách; chỗ thờ có bia mộ, nến; đường hầm có xe goòng, cột chống, đống than; khu ngập nước có vũng nước, nấm.
  - Đồ lớn đứng thành dãy ngắn sát tường và chắn ô của nó (ô `PROP`); mỗi món đều được kiểm tra không chặn mất lối đi nào.
  - Đồ vụn chủ yếu nằm dọc tường và cạnh đồ lớn, giữa phòng để thoáng; mạng nhện ở góc.
- Mỗi phòng có một tấm biển trên vách bắc, gần lối vào nhất, ghi tên theo công dụng (KHO 3, XƯỞNG 2, NHÀ NGHỈ TỔ B, MIẾU 1, LÒ 4, BỂ NƯỚC 2, GIẾNG THANG 9). Biển miếu bị cào, biển bể nước bị bùn che nửa.
- Khoảng một nửa số phòng có một dòng viết vội bằng phấn hoặc than trên vách, chọn theo chủ đề phòng, không lặp trong một map. Không nhặt được, không vào sổ.
- Chữ trên biển và trên vách vẽ ở lớp độ phân giải cao (`WallWriting`), đậm nhạt theo ánh sáng tại chỗ đó: trong tối không đọc được; chữ trên vách còn phải đứng gần mới thấy. Test kiểm tra mọi template giữ cửa thông và mọi marker đều tới được.
- Cùng seed thì ra cùng map: endless dùng seed người chơi nhập, story đổi seed mỗi lần thử.

## Asset
- `engine.gfx.Art` tìm sprite theo thứ tự:
  1. `assets/sprites.json` (vùng trong spritesheet, ví dụ pack CC0);
  2. `assets/sprites/<tên>.png`;
  3. pixel art vẽ bằng code (`ProceduralArt`).
- `engine.audio.Audio` tìm `assets/sfx/<tên>.wav`, nếu không có thì tổng hợp bằng `Synth`.
- Mixer phần mềm (`Mixer`) trộn mọi voice vào một `SourceDataLine`, hỗ trợ gain/pan/pitch và loop liền mạch.
- Nếu thêm asset CC0, ghi nguồn và giấy phép vào `CREDITS.md`.

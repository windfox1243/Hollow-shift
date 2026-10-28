# HOLLOW SHIFT

Game kinh dị sinh tồn góc nhìn từ trên xuống, viết hoàn toàn bằng Java SE:
- không dùng thư viện ngoài, không có code native;
- không cần cài Maven hay Gradle.

Đây là dự án nghiên cứu, phi thương mại.

Bạn là thợ mỏ ca đêm ở Hầm 9. Tổ ca ngày xuống hầm từ sáng rồi không ai lên lại. Nhiệm vụ của bạn là tìm cầu chì, cấp điện cho các hộp điện rồi đi thang máy lên. Nhưng dưới đó còn có một thứ khác.

Đầu mỗi ca, game công bố **luật chơi**: nó cảm nhận bạn bằng cách nào (nhìn, nghe, ngửi, cảm nhận rung chấn, đi theo tiếng vọng…), và ngoài nó ra còn thứ gì có thể giết bạn (giá lạnh, cái đói, đèn hết pin).

## Có gì trong game
- **5 ca cốt truyện**. Cốt truyện hé lộ dần qua lời dẫn mỗi ca và 10 mẩu giấy rải trong hầm. Game có **hai kết thúc**, một trong số đó là kết thúc ẩn.
- **Chế độ vô tận**: nhập mã để sinh màn. Ca sau rộng hơn, quái nhanh hơn và có nhiều luật hơn. Cùng một mã thì luôn ra cùng một chuỗi màn.
- **Bản đồ sinh ngẫu nhiên** từ các mẫu phòng tự thiết kế. Ca cuối dùng bản đồ vẽ tay.
- **Quái vật**: một khối thịt có xúc tu, bò xuyên qua vách đá. Nó chỉ cảm nhận được bạn bằng những giác quan mà luật của ca đó cho phép.
- Bóng tối gần như tuyệt đối, có hiệu ứng tim đập khi quái ở gần, nhạc rượt đuổi và cảnh hù.
- Hỗ trợ **tiếng Việt và tiếng Anh**, đổi trong Cài đặt → Ngôn ngữ.

## Yêu cầu
- JDK 21 trở lên (đã thử trên JDK 27).

## Chạy game
Chạy các lệnh sau trong thư mục này:
```
java tools/Build.java run             # biên dịch và chơi ngay
java tools/Build.java test            # chạy bộ test
java tools/Build.java jar             # đóng gói ra dist/hollow-shift.jar
java -jar dist/hollow-shift.jar       # chơi bản đã đóng gói (hoặc nhấp đúp vào file .jar)
```

Lệnh dành cho người phát triển:
```
java tools/Build.java convert-audio   # chuyển lại các file OGG/MP3 trong third_party/ sang WAV
java tools/Build.java inspect ../www build/c3-dump.txt   # công cụ nghiên cứu, xem docs/RE_ANALYSIS.md
```

## Điều khiển
Có thể đổi phím trong Cài đặt → Phím điều khiển.

| Phím | Tác dụng |
|---|---|
| WASD / phím mũi tên | di chuyển |
| Shift | chạy (gây tiếng động lớn, tốn thể lực) |
| E / Space | tương tác, nhặt đồ, đọc giấy (giữ phím để kéo cầu dao) |
| Q | ném đá hoặc pháo sáng / thả cầu chì |
| F | bật/tắt đèn lồng |
| Esc | tạm dừng |

## Cấu trúc thư mục
- `src/main/java/hollowshift/`
  - `engine/`: vòng lặp game, cửa sổ, input, bộ trộn âm thanh, ánh sáng, JSON, ngôn ngữ.
  - `world/`: ô gạch, mẫu phòng, bộ sinh bản đồ, A*.
  - `entity/`, `ai/`, `rules/`: lối chơi (nhân vật, quái vật, giác quan, luật).
  - `game/`: thế giới, dựng màn, HUD, nhiệm vụ, hiệu ứng sợ hãi.
  - `scenes/`: menu và các màn hình.
  - `story/`: chiến dịch, dữ liệu lưu.
- `src/main/resources/`: mẫu phòng, bản đồ ca cuối, file ngôn ngữ (`lang/`), âm thanh, credits.
- `src/test/java/`: bộ test tự viết, công cụ chụp ảnh màn hình và đo hiệu năng.
- `tools/`: script build viết bằng Java. Thư mục `tools/lib/` chứa hai bộ giải mã âm thanh, chỉ dùng lúc build.
- `third_party/`: file âm thanh gốc kèm giấy phép.
- `docs/RE_ANALYSIS.md`: ghi chép nghiên cứu. `docs/DESIGN.md`: thiết kế game.

Cài đặt và tiến trình được lưu tại `~/.hollowshift/save.json`.

Nguồn và giấy phép của hình ảnh, âm thanh và thư viện xem trong [CREDITS.md](CREDITS.md).

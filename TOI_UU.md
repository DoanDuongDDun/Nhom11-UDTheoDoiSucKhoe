# Ghi chú tối ưu (so với bản gốc)

Giữ nguyên package, schema Room, giao diện và hành vi; không đổi AGP/Gradle/SDK.

## Hiệu năng
- **Render gộp** (`PageFragment.observeAll`): Home/Nhật ký/Báo cáo/Chu kỳ trước đây dựng lại toàn bộ view 4–5 lần khi mở màn hình (mỗi LiveData phát một lần); nay gộp thành 1 lần.
- **BodyShapeView**: cache hình học (đường viền, Path.op UNION) theo thông số bằng `LruCache`, Paint dùng lại, dựng tay/chân một bên rồi lật gương (giảm một nửa Path.op); bỏ hoạt họa khi số đo không đổi; Home không chạy lại hoạt họa mỗi lần cập nhật dữ liệu khác (thêm nước...).
- **TrendView**: tính min/max/nhãn một lần trong `setData`, dùng mảng nguyên thủy, tái sử dụng `Path`; sửa lỗi vẽ lại chữ bị nét viền do quên đặt lại `Paint.Style`.
- **Formats**: cache `SimpleDateFormat` theo luồng (trước đây tạo mới mỗi lần gọi, kể cả trong từng dòng danh sách).
- **SymptomLogic**: regex biên dịch sẵn; `advice()` chuẩn hóa mỗi tên một lần thay vì O(số loại × số bản ghi); tìm kiếm chuẩn hóa truy vấn một lần cho cả danh sách.
- **Room**: kiểm tra trùng/đang ngủ bằng truy vấn COUNT thay vì nạp mọi phiên ngủ; `refreshHabits` bỏ qua lịch đã sinh đủ (trước đây ghi DB mỗi lần onResume và mỗi 15 phút).
- **ReminderReceiver**: dọn cờ "đã nhắc" cũ (SharedPreferences không còn phình theo ngày/lần sinh hoạt), ghi gộp một lần.
- Một executor ghi dùng chung (BodyRepository dùng `WellnessRepository.IO`), một `Handler` chính dùng chung.

## Gọn mã / độ bền
- Gộp 3 bản `sleepMinutesSafe` → `HealthMath.sleepMinutesOrZero` (SleepFragment không còn crash với dữ liệu ngủ hỏng).
- Gộp helper ngày trùng lặp (`Formats.addDays`), hằng `Formats.DAY_MS`, ViewModel dùng một hàm `finish(...)`.
- `BodyShapeParameters` có `equals/hashCode`.

## Chưa đổi (gợi ý tiếp)
- `journal`/`occurrences` vẫn nạp toàn bộ bảng vào LiveData; khi dữ liệu lớn nên chuyển sang truy vấn theo khoảng ngày.
- `release { optimization { enable = false } }` đang tắt R8: bật lên sẽ giảm kích thước APK (cần chạy thử bản release).

## Gộp với bản của model khác
- Lấy phần **giao diện** của bản kia: thẻ hero (`widget_stat_hero.xml`, `PageFragment.hero/updateHero`), danh sách dạng icon (`listStyle`), ô nhập đôi (`fieldPair`) cho Giấc ngủ, Triệu chứng, Chu kỳ, So sánh, Số đo.
- Phần **hiệu năng** giữ bản của mình vì sâu hơn: `observeAll` (gộp nhiều nguồn, thay `requestRender`), cache hình học BodyShapeView (bản kia chỉ cache một gradient và sẽ bị thay liên tục khi vẽ 2 màu ở màn So sánh), cùng toàn bộ phần dữ liệu/ReminderReceiver/SymptomLogic.

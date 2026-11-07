# Kết quả kiểm thử Vitality

Môi trường: Android emulator API 34 (Android 14), Gradle 9.6.0, Java đi kèm Android Studio. Kiểm thử ngày 07/11/2026.

## Các kiểm thử đã chạy

- Unit test: 12 bài, gồm 11 bài nghiệp vụ và 1 bài mặc định của dự án. Ánh xạ eo/hông độc lập, cân nặng không đổi hình học, số đo tùy chọn có fallback, giới hạn vẽ không sửa dữ liệu, chênh lệch, ngủ qua nửa đêm, thời lượng ngủ không hợp lệ, tỷ lệ hoàn thành với mẫu số 0, khoảng ngủ giao nhau, từ chối NaN.
- Instrumentation: 5 bài. Kiểm tra context, lưu/mở lại/sửa/xóa Room, migration v1→v2 giữ số đo, nhập hai lần đo trên giao diện và mở so sánh, điều hướng Tổng quan/Cơ thể/Nhật ký.
- Ảnh màn hình so sánh lấy từ emulator, kiểm tra trực quan để sửa khe cổ do chiều winding của Path và cân vị trí mô hình.
- Android lint: không có lỗi; còn cảnh báo về cập nhật phiên bản thư viện, tài nguyên chưa dùng và chuỗi hiển thị viết trực tiếp. Giữ cấu hình SDK/AGP theo yêu cầu, không nâng để xóa cảnh báo.

## Phạm vi kiểm thử còn cần trên thiết bị đích

Chưa kiểm thử dài hạn nhắc nhở khi Doze/khởi động lại/đổi múi giờ, toàn bộ nhà cung cấp lưu CSV, mọi cỡ màn hình và toàn bộ tổ hợp số đo biên. Không coi các mục này đã được nghiệm thu chỉ dựa vào build thành công. Bản APK là debug để chạy thử và trình diễn, không phải bản ký phát hành lên Play Store.

Báo cáo gốc:
- `app/build/reports/tests/testDebugUnitTest/index.html`
- `app/build/reports/androidTests/connected/debug/index.html`
- `app/build/reports/lint-results-debug.html`

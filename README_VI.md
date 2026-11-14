# Vitality — hướng dẫn bàn giao

Ứng dụng Android ngoại tuyến, Java + XML + ViewBinding, một Activity và Navigation Fragment, ViewModel/LiveData/Repository, Room. Package, namespace và applicationId giữ nguyên `com.example.vitality`. Không thay AGP 9.4.1, Gradle 9.6.0, compile/target SDK 37 hay min SDK 24. Không sửa local.properties.

## Build và chạy

Mở thư mục dự án trong Android Studio, chờ Gradle Sync, chọn module app và emulator/điện thoại API 24 trở lên rồi Run. SDK 37 và JDK đi kèm Android Studio cần được cài trên máy. Không chép local.properties sang máy khác.

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Màn hình và dữ liệu

- Tổng quan: hình gần nhất, BMI, cân nặng/eo so lần trước, tổng nước/vận động/ngủ và lịch hôm nay.
- Cơ thể: thêm, sửa, xóa có xác nhận; các số đo bắt buộc và tùy chọn; lịch sử chọn mốc, bộ lọc tuần/tháng/90 ngày/khoảng ngày; chuyển hình bằng ValueAnimator.
- So sánh: hai bản ghi bất kỳ, tự sắp trước/sau theo thời điểm và ID; cạnh nhau hoặc chồng hình, thanh độ đậm; chênh lệch tuyệt đối và phần trăm từ dữ liệu gốc.
- Nhật ký: bữa ăn, nước (thêm nhanh 250 ml hoặc nhập), vận động; sửa/xóa. Calo nhập tay, không ước tính tự động.
- Giấc ngủ: bắt đầu, kết thúc, nhập tay, sửa/xóa; kiểm tra trùng, một phiên hoạt động; dữ liệu phiên đang ngủ nằm trong Room nên tồn tại sau khi đóng ứng dụng.
- Chu kỳ sinh hoạt: giờ, các thứ, ngày bắt đầu/kết thúc; sửa/tạm dừng/tiếp tục/xóa; lần thực hiện riêng, hoàn thành/bỏ qua/bỏ lỡ. Xóa lịch giữ lịch sử.
- Thống kê: điểm cân nặng, eo, tổng nước, vận động, ngủ và tỷ lệ hoàn thành theo ngày; khoảng ngày; CSV UTF-8 qua Storage Access Framework.
- Số đo cơ thể (bổ sung): kiểm tra trùng ngày đo, hỏi "Ghi đè hay giữ cả hai?"; thông báo "Dữ liệu không hợp lệ!", "Lưu thành công", "Lỗi lưu dữ liệu!"; cân nặng hợp lệ 2–300 kg; biểu đồ cân nặng và BMI ngay trên màn Cơ thể; cảnh báo BMI ngoài 18,5–24,9 (chỉ tham khảo); nút Hủy bỏ.
- Triệu chứng (mới): vào từ Tổng quan hoặc Nhật ký. Thêm/sửa/xóa (có xác nhận) triệu chứng kèm mức nhẹ/vừa/nặng, thời gian, ghi chú; nhật ký nhóm theo ngày; tìm kiếm không phân biệt dấu; thống kê 7/30 ngày; cảnh báo khuyến nghị đi khám khi mức nặng hoặc cùng triệu chứng 3 ngày liên tiếp; "Gợi ý chăm sóc (tham khảo)" chạy ngoại tuyến theo luật có sẵn, luôn kèm lưu ý không thay thế chẩn đoán y tế. Bảng `symptoms`, schema v3, migration 2→3 chỉ thêm bảng.
- Giao diện (theo bản demo Figma Make): font Manrope, nền gradient pastel, thẻ trắng mờ bo tròn, thanh điều hướng nổi. Trang chủ có thẻ năng lượng/tiến độ, lưới 6 chỉ số và thẻ triệu chứng (dữ liệu thật). Cơ thể, Nhật ký, Báo cáo, Cá nhân dựng lại bằng thẻ, dòng thời gian, ô chỉ số, thanh mục tiêu và công tắc nhắc nhở. Nút chính màu đỏ, dữ liệu/biểu đồ màu xanh lá.
- Cá nhân: hồ sơ, mục tiêu, mẫu hiển thị độc lập giới tính; bật/tắt nhắc đo, nước, ngủ và lịch sinh hoạt.

Nguồn nằm tại `app/src/main/java/com/example/vitality/`: `data/entity`, `data/dao`, `data/database`, `data/repository`, `viewmodel`, `ui/main`, `ui/home`, `ui/body`, `ui/diary`, `ui/sleep`, `ui/habit`, `ui/statistics`, `ui/profile`, `graphics`, `notification`, `utils`. XML dùng trong `app/src/main/res/layout`, điều hướng trong `res/navigation/main_graph.xml` và `res/menu/bottom_nav.xml`.

Entity JournalEntry gom bữa ăn/nước/vận động/ngủ bằng trường kind. UserProfile lưu hồ sơ đơn trên thiết bị. HabitSchedule là mẫu lịch, HabitOccurrence là từng lần thực hiện có khóa ngoại và chỉ mục duy nhất (scheduleId,dueAt). BodyMeasurement giữ số đo nguyên gốc, ID, timestamps, bodyTemplate và rendererVersion. Schema v1/v2 nằm trong `app/schemas`; migration 1→2 thêm bảng, không xóa hoặc sửa số đo cũ. Không có destructive migration.

## Thuật toán cơ thể để viết báo cáo

BodyShapeMapper phiên bản 1 dùng hệ số FRONT_WIDTH_FACTOR = 0.36 chuyển chu vi sang bề rộng minh họa. Hệ số này là lựa chọn đồ họa, không phải công thức giải phẫu hoặc chu vi/π. Các khoảng giới hạn bề rộng nằm trong mapper; Custom View chặn thêm theo chiều cao để giữ hình học ổn định. Giới hạn vẽ không sửa số đo đã lưu.

Ngực, eo và hông điều khiển các mốc riêng ở khoảng 25%, 37%, 48% chiều cao. Path dùng cubic/quadratic Bézier nối các mốc đối xứng. Đầu/cổ/tay/chân được dựng riêng và nối vào thân; bề rộng đùi/bắp chân bị giới hạn tương đối vùng hông để tránh tự cắt. Trường tùy chọn trống dùng chu vi cơ sở tay 28 cm, đùi 50 cm, bắp chân 34 cm và có chú thích ước lượng. Mẫu vai rộng thêm 3 đơn vị bề rộng phần trên. Cân nặng không tham gia hình học.

Hai hình so sánh dùng một scale = min(chiều rộng khung / bề rộng chuẩn, chiều cao khung / (chiều cao lớn nhất của cặp + 12)), cùng mốc bàn chân; không fit từng người độc lập. Chiều cao được giữ trong hệ tọa độ để so sánh chiều cao. ValueAnimator nội suy hình học trong 450 ms; các hình trung gian không được lưu thành bản ghi.

BMI = kg / (cm/100)^2. Chênh lệch = sau − trước, phần trăm = chênh lệch / trước ×100. Thời lượng ngủ = kết thúc − bắt đầu − thức giữa giấc, thống kê theo ngày thức dậy. Tỷ lệ duy trì = hoàn thành / tất cả lần đã đến hạn; bỏ qua và bỏ lỡ vẫn nằm trong mẫu số; chưa đến hạn và thời gian tạm dừng không tính.

## Kịch bản demo

1. Cá nhân → nhập biệt danh, mục tiêu và lưu.
2. Cơ thể → nhập 170 cm, 65 kg, ngực 90, eo 75, hông 95; để trống tay/chân.
3. Nhập lần hai: giữ các số trên, đổi eo 85 hoặc hông 105.
4. Chạm từng dòng lịch sử; quan sát vùng eo/hông thay đổi.
5. So sánh hai giai đoạn → thử cạnh nhau và chồng hình, kéo độ đậm, đọc chênh lệch.
6. Thống kê → chọn khoảng ngày → xuất CSV; đóng/mở lại app kiểm tra lịch sử.
7. Nhật ký → thêm nước, bữa ăn, vận động, ngủ qua đêm; tạo lịch sinh hoạt và đánh dấu khi đến hạn.

Không tự nạp dữ liệu mẫu vào database người dùng. Kiểm thử giao diện có bản ghi gắn ghi chú integration-ui và dọn các bản ghi đó sau bài test.

## Giới hạn vận hành

- Mô hình là minh họa 2D, không dựng cơ thể 3D hoặc chẩn đoán sức khỏe; BMI không phân loại bệnh.
- Nhắc đo 08:00, nước 10:00, ngủ 22:00; lịch sinh hoạt dùng giờ người dùng nhập. AlarmManager kiểm tra khoảng 15 phút, có thể trễ do Android/Doze. Không xin quyền exact alarm. Khi máy khởi động lại hoặc đổi giờ/múi giờ, lịch kiểm tra được đăng ký lại. Lần sinh hoạt có thời hạn thực hiện 1 giờ.
- Không OCR, cảm biến, đồng bộ, đăng nhập hoặc dữ liệu thực phẩm trực tuyến; các chức năng này nằm ngoài bản ngoại tuyến hiện tại.
- Biểu đồ nối các điểm ghi nhận theo thời gian để thể hiện xu hướng; đường nối không phải số liệu đo bổ sung và không chèn 0 cho ngày thiếu. Trung bình nhật ký tính trên ngày có dữ liệu.
- Quyền thông báo và hành vi tiết kiệm pin khác nhau theo hãng; cần kiểm tra bổ sung trên điện thoại đích.

Tài liệu Android tham khảo: https://developer.android.com/develop/background-work/services/alarms và https://developer.android.com/jetpack/androidx/releases/navigation.

## Mục tiêu cá nhân và hằng ngày

Trang chủ có thẻ Mục tiêu của bạn. Mục tiêu cơ thể gồm cân nặng hoặc vòng eo; lấy số đo gần nhất lúc tạo làm mốc ban đầu. Tiến độ = (hiện tại − ban đầu)/(đích − ban đầu), chặn 0–100%. Nếu đích trùng mốc ban đầu, chỉ đạt khi số đo hiện tại bằng đích trong sai số 0,1.

Mục tiêu daily gồm món ăn/số lần ghi bữa, hoạt động/số phút, nước/ml và ngủ/giờ. Từ khóa tùy chọn lọc tên món hoặc hoạt động (không phân biệt hoa thường/dấu). Mỗi bản ghi món phù hợp tính một lần, không tự hiểu khẩu phần hoặc thành phần dinh dưỡng. Tiến độ ngày là trung bình phần trăm của từng mục tiêu daily, mỗi mục chặn tối đa 100%. Không gộp mục tiêu cơ thể vào điểm daily. Chọn ngày để tính lại từ nhật ký ngày đó. Xóa nhật ký giảm tiến độ; xóa mục tiêu loại mục tiêu khỏi cả các ngày xem lại, không xóa nhật ký. Mục tiêu được chia thành Cơ thể, Ăn uống & nước, Vận động và Giấc ngủ. Mục tiêu nước/ngủ/vận động tổng thay thế giá trị mặc định của hồ sơ trên Trang chủ. Các màn hình Dinh dưỡng, Hoạt động và Giấc ngủ hiển thị tiến độ tính từ cùng nhật ký. Đặt lại cùng loại và từ khóa cập nhật đích đang có. Ví dụ: đặt nước 2.000 ml, ghi 1.000 ml thì cả Trang chủ và Mục tiêu hiển thị 50%.

Room phiên bản 4 thêm bảng goals bằng MIGRATION_3_4. Không xóa dữ liệu cũ. Không tự tạo mục tiêu hoặc bản ghi hoàn thành giả.

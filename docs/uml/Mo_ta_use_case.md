# Đặc tả use case — Vitality

Các sơ đồ là UML mức phân tích theo mẫu giao diện – điều khiển – thực thể. Tên CoTheUI, CoTheController, MucTieuUI và MucTieuController là tên vai trò phân tích; phần chú thích trong ảnh ánh xạ chúng sang các lớp Java thực tế. Không phải các lớp mới được thêm vào ứng dụng. Chỉ thể hiện thuộc tính và thao tác cốt lõi.

## 1. Mô phỏng cơ thể người và so sánh giai đoạn

| Tên use case | Mô phỏng cơ thể người và so sánh giai đoạn |
| --- | --- |
| Mô tả vắn tắt | Cho phép người dùng nhập và lưu số đo theo từng giai đoạn, xem mô hình cơ thể người 2D màu đỏ thay đổi theo số đo và so sánh sự thay đổi giữa hai lần đo. |
| Luồng các sự kiện | Luồng cơ bản và luồng rẽ nhánh bên dưới. |

**Luồng cơ bản:**

1. Người dùng chọn chức năng “Cơ thể”. Hệ thống hiển thị mô hình cơ thể và lịch sử số đo đã lưu.
2. Người dùng chọn thêm số đo, nhập thời điểm đo, chiều cao, cân nặng, vòng ngực, vòng eo và vòng hông; có thể bổ sung vòng tay, vòng đùi, vòng bắp chân và ghi chú. Người dùng nhấn “Lưu số đo”.
3. Hệ thống kiểm tra trường bắt buộc, định dạng, giới hạn số đo và thời điểm đo. Nếu hợp lệ, hệ thống lưu giai đoạn vào bảng body_measurements bằng Room/SQLite.
4. Hệ thống chuyển số đo thành tham số hình thể, vẽ lại mô hình cơ thể 2D màu đỏ và cập nhật lịch sử. Người dùng có thể chọn giai đoạn đã lưu để xem hình thể tương ứng.
5. Khi có ít nhất hai giai đoạn, người dùng mở so sánh, chọn hai mốc và chế độ đặt cạnh nhau hoặc chồng hình. Hệ thống sắp thứ tự trước/sau theo thời gian, hiển thị hai mô hình cùng chênh lệch số đo và BMI. Use case kết thúc.

**Luồng rẽ nhánh:**

1. Tại bước 1, nếu chưa có số đo, hệ thống hiển thị trạng thái chưa có dữ liệu và hình minh họa mặc định. Người dùng tiếp tục bước 2.
2. Tại bước 3, nếu thiếu trường bắt buộc, số liệu sai định dạng, vượt giới hạn hoặc thời điểm đo ở tương lai, hệ thống báo lỗi và yêu cầu sửa ở bước 2; không lưu dữ liệu không hợp lệ.
3. Tại bước 3, nếu đã có số đo trong cùng ngày, hệ thống cho người dùng chọn ghi đè, lưu thêm hoặc hủy. Ghi đè thay thế dữ liệu trùng ngày; lưu thêm tạo giai đoạn riêng; hủy không thay đổi dữ liệu.
4. Tại bước 3, nếu ghi dữ liệu thất bại, hệ thống hiển thị thông báo lỗi; thao tác lưu không hoàn tất. Người dùng có thể thử lại hoặc thoát.
5. Tại bước 5, nếu chưa có đủ hai giai đoạn khác nhau, người dùng cần bổ sung số đo trước khi so sánh. Nếu không cần so sánh, use case kết thúc sau bước 4.

| Các yêu cầu đặc biệt | Hình cơ thể là minh họa 2D, không phải tái dựng giải phẫu chính xác. Số đo lưu trữ không bị thay đổi bởi thuật toán vẽ. Hỗ trợ đặt cạnh nhau và chồng hình; độ đậm hình trước có thể điều chỉnh khi chồng. |
| --- | --- |
| Tiền điều kiện | Người dùng đã mở ứng dụng Vitality. Để so sánh, cần có ít nhất hai giai đoạn số đo. Phiên bản hiện tại không bắt buộc đăng nhập. |
| Hậu điều kiện | Khi thành công, số đo được lưu và mô hình/lịch sử được cập nhật. Nếu thực hiện so sánh, hệ thống hiển thị hai giai đoạn và chênh lệch tương ứng. |
| Điều kiện mở rộng | So sánh hai giai đoạn khi người dùng yêu cầu và có đủ dữ liệu. |

## 2. Đặt và theo dõi mục tiêu sức khỏe

| Tên use case | Đặt và theo dõi mục tiêu sức khỏe |
| --- | --- |
| Mô tả vắn tắt | Cho phép người dùng đặt mục tiêu cơ thể và mục tiêu hằng ngày; hệ thống tự tính phần trăm hoàn thành từ số đo và nhật ký được ghi ở các chức năng liên quan. |
| Luồng các sự kiện | Luồng cơ bản và luồng rẽ nhánh bên dưới. |

**Luồng cơ bản:**

1. Người dùng chọn ô “Mục tiêu của bạn” trên Trang chủ. Hệ thống hiển thị danh sách mục tiêu theo nhóm và tiến độ của ngày đang xem.
2. Người dùng chọn nhóm Cơ thể, Ăn uống & nước, Vận động hoặc Giấc ngủ; chọn loại mục tiêu, nhập tên và giá trị muốn đạt. Với ăn uống hoặc vận động, người dùng có thể nhập từ khóa để chỉ tính món ăn/hoạt động phù hợp.
3. Người dùng nhấn “Lưu mục tiêu”. Hệ thống kiểm tra dữ liệu. Với mục tiêu cơ thể, hệ thống lấy số đo gần nhất làm mốc ban đầu khi tạo mới; sau đó lưu mục tiêu vào bảng goals bằng Room/SQLite.
4. Người dùng ghi nhận dữ liệu thực tế ở chức năng liên quan: lượng nước đã uống, món ăn, thời gian vận động, giấc ngủ hoặc số đo mới. Hệ thống lưu dữ liệu vào nhật ký hoặc bảng số đo tương ứng.
5. Hệ thống nhận dữ liệu thay đổi và tính lại tiến độ: mục tiêu hằng ngày dựa trên dữ liệu ngày đang xem; mục tiêu cơ thể dựa trên số đo mới nhất so với mốc ban đầu và đích. Ví dụ: đích uống 2.000 ml/ngày, đã ghi 1.000 ml thì đạt 50%.
6. Hệ thống cập nhật thanh phần trăm của từng mục tiêu và tiến độ chung trong ngày; các màn hình liên quan hiển thị tiến độ tương ứng. Use case kết thúc. Bước 4–6 có thể lặp lại khi người dùng tiếp tục ghi dữ liệu.

**Luồng rẽ nhánh:**

1. Tại bước 1, nếu chưa có mục tiêu, hệ thống hiển thị trạng thái trống và hướng dẫn tạo mục tiêu ở bước 2.
2. Tại bước 3, nếu thiếu tên, giá trị sai định dạng, ngoài giới hạn hoặc số lần ăn không phải số nguyên, hệ thống báo lỗi và yêu cầu sửa ở bước 2.
3. Tại bước 3, nếu đặt mục tiêu cơ thể nhưng chưa có số đo, hệ thống yêu cầu nhập số đo trước; mục tiêu chưa được lưu.
4. Tại bước 3, nếu đã có mục tiêu cùng loại và cùng từ khóa, hệ thống cập nhật tên và đích của mục tiêu đó, giữ mốc ban đầu và ngày tạo.
5. Tại bước 3 hoặc 4, nếu ghi cơ sở dữ liệu thất bại, hệ thống báo lỗi; thao tác lưu không hoàn tất. Người dùng có thể thử lại hoặc thoát.
6. Tại bước 4–6, nếu nhật ký được sửa hoặc xóa, hệ thống tính lại tiến độ theo dữ liệu còn lại. Khi chuyển sang ngày mới, mục tiêu hằng ngày được tính từ dữ liệu ngày mới; không cộng dồn ngày trước.
7. Người dùng có thể chọn ngày trước để xem tiến độ tính từ nhật ký ngày đó theo các mục tiêu hiện còn tồn tại; đây không phải bản chụp lịch sử bất biến của mục tiêu. Mục tiêu cơ thể vẫn dùng số đo mới nhất.
8. Người dùng có thể chọn xóa mục tiêu và xác nhận. Hệ thống xóa mục tiêu, giữ nguyên nhật ký và số đo; mục tiêu đã xóa cũng không còn xuất hiện khi xem ngày trước.

| Các yêu cầu đặc biệt | Tiến độ giới hạn 0–100%. Tiến độ chung trong ngày là trung bình phần trăm các mục tiêu hằng ngày, không gồm mục tiêu cơ thể. Nước tính bằng ml, vận động bằng phút, giấc ngủ bằng giờ, món ăn bằng số bản ghi phù hợp. Từ khóa không phân biệt hoa thường và dấu tiếng Việt. |
| --- | --- |
| Tiền điều kiện | Người dùng đã mở Vitality. Mục tiêu cơ thể cần có ít nhất một lần đo để làm mốc. Không bắt buộc đăng nhập hoặc kết nối Internet. |
| Hậu điều kiện | Khi thành công, mục tiêu được lưu/cập nhật và tiến độ phản ánh dữ liệu thực tế. Việc đặt mục tiêu không tự tạo nhật ký hoàn thành. |
| Điều kiện mở rộng | Liên kết ghi nhận nước, dinh dưỡng, vận động, giấc ngủ và số đo; xem tiến độ theo ngày; xóa mục tiêu. |

## Ảnh biểu đồ

- 01_trinh_tu_co_the.png — Trình tự mô phỏng cơ thể.
- 02_trinh_tu_muc_tieu.png — Trình tự đặt và theo dõi mục tiêu.
- 03_lop_co_the.png — Lớp phân tích cho mô phỏng cơ thể.
- 04_lop_muc_tieu.png — Lớp phân tích cho mục tiêu.

Tái tạo ảnh bằng: python docs/uml/generate_diagrams.py (cần Pillow và font Arial trên Windows).

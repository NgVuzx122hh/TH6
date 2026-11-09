# Kết quả kiểm thử

## Kiểm thử tự động

Lệnh chạy: `mvn -q test`

| Nhóm | Số kiểm thử | Kết quả |
|---|---:|---|
| Laboratory Provider API | 5 | Đạt |
| Appointment Consumer API | 3 | Đạt |
| Consumer gọi Provider qua REST | 3 | Đạt |
| Điều phối nghiệp vụ và xử lý bù | 5 | Đạt |
| **Tổng** | **16** | **Đạt, không lỗi** |

Các bài kiểm thử xác minh JWT và quyền truy cập, tiếp nhận chỉ định, kiểm tra dữ liệu, truy vấn kết quả, chuyển tiếp Bearer token, xử lý timeout/HTTP lỗi từ Provider và hủy phiếu bù khi lưu liên kết thất bại. Kiểm thử DB dùng H2; kiểm thử HTTP Consumer–Provider dùng mock REST server.

## Kiểm tra tài liệu

- Postman collection parse thành công dưới định dạng JSON.
- 12 script kiểm thử trong collection được Node.js kiểm tra cú pháp thành công.
- `pom.xml` hợp lệ.
- Sơ đồ chuỗi PNG đã được sinh từ mã nguồn `RenderSequenceDiagram.java`.

## Giới hạn môi trường

Docker CLI không có trong môi trường thực thi, vì vậy chưa chạy được PostgreSQL thật, hai tiến trình Spring Boot đồng thời hoặc toàn bộ Postman collection trên dịch vụ đang chạy. Cấu hình demo PostgreSQL và các request Postman đã được chuẩn bị trong workspace.

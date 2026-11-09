# Thiết kế tích hợp — Bài thực hành 6.1

## Bối cảnh và vai trò

- **Consumer:** Appointment/Examination Service (`:8080`) nhận chỉ định từ ứng dụng khách, kiểm tra hồ sơ và lịch khám, gọi Laboratory Service, rồi lưu liên kết `examinationId`–`labOrderId`.
- **Provider:** Laboratory Service (`:8081`) quản lý danh mục xét nghiệm, tiếp nhận chỉ định và cung cấp kết quả qua API.
- **Database:** PostgreSQL dùng schema riêng cho mỗi dịch vụ. Consumer không truy vấn bảng của Provider.

## Luồng nghiệp vụ

1. Bác sĩ gửi yêu cầu chỉ định với `patientId`, `appointmentId`, `testCodes`, `notes`.
2. Consumer kiểm tra bệnh nhân tồn tại/đang hoạt động, lịch khám tồn tại, thuộc bệnh nhân đó và đang ở trạng thái `SCHEDULED`.
3. Consumer gọi `POST /api/v1/lab-orders` qua `RestClient`, chuyển tiếp JWT bác sĩ.
4. Provider chuẩn hóa mã xét nghiệm, chỉ chấp nhận `CBC` và `GLUCOSE`, lưu phiếu ở trạng thái `RECEIVED`.
5. Consumer lưu liên kết với `orderId` Provider trả về. Nếu ghi liên kết lỗi, Consumer gọi `DELETE /api/v1/lab-orders/{id}` để hủy phiếu. Nếu bước bù cũng lỗi, trả `RECONCILIATION_REQUIRED` và mã phiếu để đối soát.
6. Kỹ thuật viên ghi nhận kết quả qua API Provider. Bác sĩ tra cứu kết quả qua API Consumer; Consumer truy vấn Provider bằng REST, không truy cập DB Provider.

## Hợp đồng và lỗi

Hợp đồng request/response, JWT, status code và dữ liệu mẫu được liệt kê tại [`api-contract.md`](./api-contract.md). Timeout kết nối/đọc Provider được giới hạn cấu hình; dịch vụ không khả dụng trả `503`, lỗi Provider trả `502`, điều kiện nghiệp vụ không hợp lệ trả `422`. Phiếu chưa hoàn thành trả `409` khi truy vấn kết quả. Sequence diagram có bản [PNG](./sequence-diagram/integration-flow.png) và nguồn [PlantUML](./sequence-diagram/integration-flow.puml).

OpenAPI/Swagger UI được cung cấp riêng bởi từng dịch vụ để xem endpoint, schema và quyền Bearer JWT.

## Tính nhất quán liên dịch vụ

Hai cơ sở dữ liệu không dùng chung transaction. Nếu Provider đã tạo phiếu nhưng Consumer không lưu được liên kết, Consumer thực hiện thao tác bù bằng API hủy phiếu. Nếu thao tác bù thất bại, lỗi được trả rõ ràng để vận hành đối soát; ứng dụng không báo thành công giả.

## Dữ liệu nhạy cảm và quyền truy cập

- JWT HS256 có hạn 60 phút; secret phải giống nhau ở hai dịch vụ.
- `DOCTOR`: tạo chỉ định và xem phiếu/kết quả.
- `LAB_TECHNICIAN`: xem phiếu, cập nhật kết quả.
- Token demo được cấp tại `POST /api/v1/auth/token`; tài khoản mẫu lưu trong bộ nhớ và chỉ dùng cho bài thực hành.
- Ứng dụng mẫu chỉ lưu ID bệnh nhân, không lưu họ tên hoặc thông tin định danh cá nhân.

## Cấu hình vận hành

Các cổng mặc định là `8080` và `8081`; URL Provider, thời gian timeout, thông tin DB, mật khẩu mẫu và `JWT_SECRET` có thể cấu hình qua biến môi trường. PostgreSQL được khởi tạo schema bằng `docker/postgres-init.sql`.

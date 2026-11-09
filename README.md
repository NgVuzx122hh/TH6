# TH6 — Tích hợp dịch vụ phòng khám

Bài thực hành 6.1 triển khai quy trình chỉ định và tra cứu xét nghiệm. `Appointment/Examination Service` là Consumer; `Laboratory Service` là Provider độc lập, giao tiếp qua REST/JSON.

## Công nghệ và cấu trúc

- Java 17, Spring Boot 3.3, Maven.
- PostgreSQL 16; mỗi dịch vụ dùng một schema riêng (`appointment`, `laboratory`).
- JWT HS256, phân quyền `DOCTOR` và `LAB_TECHNICIAN`.
- Swagger/OpenAPI, Postman collection và sequence diagram trong `integration/`.

```text
appointment-service/   Consumer: kiểm tra bệnh nhân/lịch khám, lưu liên kết, truy vấn kết quả
laboratory-service/    Provider: danh mục xét nghiệm, tiếp nhận chỉ định, quản lý kết quả
docker/                Khởi tạo schema PostgreSQL
integration/           Thiết kế, hợp đồng API, sơ đồ và bộ kiểm thử
```

## Chạy ứng dụng

Cần Java 17+, Maven và Docker Compose.

1. Khởi động PostgreSQL:

   ```powershell
   docker compose up -d postgres
   ```

2. Dùng **hai cửa sổ PowerShell**. Thiết lập cùng một bí mật JWT và cùng mật khẩu demo trong cả hai cửa sổ. Giá trị mặc định chỉ dành cho máy local; thay chúng trước khi triển khai.

   Terminal 1 — Provider tại `http://localhost:8081`:

   ```powershell
   $env:JWT_SECRET = "dGg2LWRldi1vbmx5LXNlY3JldC1rZXktMzItYnl0ZXM="
   $env:TH6_DOCTOR_PASSWORD = "doctor-dev-only"
   $env:TH6_LAB_TECHNICIAN_PASSWORD = "labtech-dev-only"
   mvn -pl laboratory-service spring-boot:run
   ```

   Terminal 2 — Consumer tại `http://localhost:8080`:

   ```powershell
   $env:JWT_SECRET = "dGg2LWRldi1vbmx5LXNlY3JldC1rZXktMzItYnl0ZXM="
   $env:TH6_DOCTOR_PASSWORD = "doctor-dev-only"
   $env:TH6_LAB_TECHNICIAN_PASSWORD = "labtech-dev-only"
   $env:LABORATORY_BASE_URL = "http://localhost:8081"
   mvn -pl appointment-service spring-boot:run
   ```

   Khi có Docker Compose, dữ liệu mẫu được nạp tự động: bệnh nhân `101` đang hoạt động, bệnh nhân `102` không hoạt động; lịch `5001` hợp lệ và lịch `5002` đã hoàn tất. Mật khẩu mặc định và JWT secret trong cấu hình chỉ phục vụ demo local.

## Demo nhanh bằng PowerShell

Lấy JWT bác sĩ từ Consumer:

```powershell
$doctor = Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/v1/auth/token" `
  -ContentType "application/json" `
  -Body '{"username":"doctor","password":"doctor-dev-only"}'
$doctorHeaders = @{ Authorization = "Bearer $($doctor.accessToken)" }
```

Tạo chỉ định — Consumer xác thực bệnh nhân/lịch khám, gọi REST Provider và lưu liên kết:

```powershell
$exam = Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/v1/examinations" `
  -Headers $doctorHeaders `
  -ContentType "application/json" `
  -Body '{"patientId":101,"appointmentId":5001,"testCodes":["CBC","GLUCOSE"],"notes":"Nhịn ăn sáng"}'
$exam
```

Lấy JWT kỹ thuật viên để ghi nhận kết quả trên Provider:

```powershell
$labTech = Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8081/api/v1/auth/token" `
  -ContentType "application/json" `
  -Body '{"username":"labtech","password":"labtech-dev-only"}'
$labHeaders = @{ Authorization = "Bearer $($labTech.accessToken)" }
$labOrderId = $exam.labOrderId
Invoke-RestMethod -Method Put `
  -Uri "http://localhost:8081/api/v1/lab-orders/$labOrderId/results" `
  -Headers $labHeaders `
  -ContentType "application/json" `
  -Body '{"results":[{"testCode":"CBC","value":"Bình thường","unit":"","referenceRange":"Bình thường"},{"testCode":"GLUCOSE","value":"92","unit":"mg/dL","referenceRange":"70-99"}]}'
```

Consumer tra cứu kết quả qua Provider (không đọc trực tiếp cơ sở dữ liệu Laboratory):

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/examinations/lab-orders/$labOrderId/results" `
  -Headers $doctorHeaders
```

## Kiểm thử

```powershell
mvn test
```

Chạy collection `integration/postman/integration-tests.json` trên Postman sau khi hai dịch vụ và PostgreSQL đã chạy. Chi tiết endpoint, dữ liệu mẫu và các trường hợp lỗi nằm trong [`integration/api-contract.md`](integration/api-contract.md).

Swagger UI: `http://localhost:8080/swagger-ui/index.html` (Consumer) và `http://localhost:8081/swagger-ui/index.html` (Provider). Tài liệu JSON tương ứng ở `/v3/api-docs`.

Sơ đồ chuỗi: [`integration/sequence-diagram/integration-flow.png`](integration/sequence-diagram/integration-flow.png). Kết quả test chi tiết: [`integration/test-results.md`](integration/test-results.md).

## Lưu ý triển khai

- Tài khoản, mật khẩu và secret mặc định chỉ để demo; cấu hình mật khẩu/secret mạnh qua biến môi trường khi triển khai.
- Dữ liệu y tế cần xác thực, phân quyền và giới hạn truy cập. Ví dụ này chỉ lưu ID bệnh nhân, không lưu thông tin định danh cá nhân.
- Khi lưu liên kết chỉ định thất bại sau khi Provider đã nhận yêu cầu, Consumer thử hủy chỉ định; nếu hủy cũng lỗi, API báo `RECONCILIATION_REQUIRED` để xử lý thủ công.

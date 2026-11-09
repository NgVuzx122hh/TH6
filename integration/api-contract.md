# Hợp đồng API

Mọi API nghiệp vụ yêu cầu `Authorization: Bearer <JWT>`. Lấy token bằng `POST /api/v1/auth/token`; tài khoản demo: `doctor` / `doctor-dev-only`, `labtech` / `labtech-dev-only`. Token có hạn một giờ.

Swagger UI: `http://localhost:8080/swagger-ui/index.html` (Consumer) và `http://localhost:8081/swagger-ui/index.html` (Provider).

## Laboratory Service — Provider (`http://localhost:8081`)

| Method | Path | Quyền | Chức năng |
|---|---|---|---|
| GET | `/api/v1/lab-tests` | DOCTOR, LAB_TECHNICIAN | Danh sách xét nghiệm (`CBC`, `GLUCOSE`) |
| POST | `/api/v1/lab-orders` | DOCTOR | Tiếp nhận yêu cầu xét nghiệm |
| GET | `/api/v1/lab-orders?appointmentId={id}` | DOCTOR, LAB_TECHNICIAN | Tra cứu phiếu theo lịch khám |
| GET | `/api/v1/lab-orders/{id}` | DOCTOR, LAB_TECHNICIAN | Tra cứu phiếu |
| GET | `/api/v1/lab-orders/{id}/results` | DOCTOR, LAB_TECHNICIAN | Lấy kết quả; `409` nếu chưa hoàn thành |
| PUT | `/api/v1/lab-orders/{id}/results` | LAB_TECHNICIAN | Ghi nhận kết quả mô phỏng |
| DELETE | `/api/v1/lab-orders/{id}` | DOCTOR | Hủy phiếu `RECEIVED` để bù khi Consumer lưu lỗi |

Hai endpoint `PUT .../results` và `DELETE ...` bổ sung để mô phỏng hoàn tất xét nghiệm và xử lý bù trong bài thực hành; các API GET/POST chính giữ đúng hợp đồng đề bài.

### Tạo phiếu xét nghiệm

`POST /api/v1/lab-orders`

```json
{
  "patientId": 101,
  "appointmentId": 5001,
  "testCodes": ["CBC", "GLUCOSE"],
  "notes": "Nhịn ăn sáng"
}
```

Trả `201 Created`, `status: RECEIVED`, `orderId`, các mã xét nghiệm đã chuẩn hóa và `message: "Lab order received successfully"`.

### Ghi nhận kết quả

`PUT /api/v1/lab-orders/{id}/results`

```json
{
  "results": [
    {"testCode": "CBC", "value": "Bình thường", "unit": "", "referenceRange": "Bình thường"},
    {"testCode": "GLUCOSE", "value": "92", "unit": "mg/dL", "referenceRange": "70-99"}
  ]
}
```

Phải có đúng một kết quả cho mỗi xét nghiệm trong phiếu. Sau cập nhật, trạng thái là `COMPLETED`.

## Appointment/Examination Service — Consumer (`http://localhost:8080`)

| Method | Path | Quyền | Chức năng |
|---|---|---|---|
| POST | `/api/v1/examinations` | DOCTOR | Kiểm tra bệnh nhân/lịch khám, gọi Provider và lưu liên kết |
| GET | `/api/v1/examinations/{id}` | DOCTOR | Tra cứu chỉ định và trạng thái từ Provider |
| GET | `/api/v1/examinations?appointmentId={id}` | DOCTOR | Danh sách chỉ định của lịch khám |
| GET | `/api/v1/examinations/results?appointmentId={id}` | DOCTOR | Truy vấn kết quả qua Laboratory Service |
| GET | `/api/v1/examinations/lab-orders/{labOrderId}/results` | DOCTOR | Truy vấn kết quả theo mã phiếu qua Laboratory Service |

### Tạo chỉ định

`POST /api/v1/examinations`

```json
{
  "patientId": 101,
  "appointmentId": 5001,
  "testCodes": ["CBC", "GLUCOSE"],
  "notes": "Nhịn ăn sáng"
}
```

Trả `201 Created` với `examinationId`, `labOrderId`, trạng thái Provider và liên kết tới tài nguyên Consumer. Dữ liệu mẫu hợp lệ là bệnh nhân `101`, lịch khám `5001`; bệnh nhân `102` không hoạt động, lịch `5002` đã hoàn tất.

## Lỗi chuẩn

```json
{
  "timestamp": "2026-01-01T10:00:00Z",
  "status": 422,
  "code": "INVALID_REQUEST",
  "message": "Appointment is not valid for examination",
  "path": "/api/v1/examinations"
}
```

| Status | Ý nghĩa |
|---|---|
| 400 | JSON hoặc trường đầu vào không hợp lệ |
| 401 / 403 | Thiếu/sai JWT hoặc không đủ quyền |
| 404 | Không tìm thấy bệnh nhân, lịch khám, chỉ định hoặc phiếu |
| 409 | Kết quả chưa sẵn sàng hoặc phiếu không ở trạng thái phù hợp |
| 422 | Mã xét nghiệm/điều kiện nghiệp vụ không hợp lệ |
| 502 / 503 | Provider lỗi, timeout hoặc không khả dụng |
| 500 | Lưu Consumer lỗi; nếu hủy Provider cũng lỗi, mã lỗi là `RECONCILIATION_REQUIRED` |

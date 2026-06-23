# Traffic Detection Backend — API Document

> **Base URL:** `http://localhost:8080`
> **Version:** 1.0.0
> **Authentication:** JWT Bearer Token (hiện tại SecurityConfig đang `permitAll` — mọi request đều không cần xác thực)

---

## Mục lục

1. [Auth API](#1-auth-api)
   - [POST /auth/register](#11-post-authregister)
   - [POST /auth/login](#12-post-authlogin)
   - [POST /auth/refresh-token](#13-post-authrefresh-token)
2. [Intersection API](#2-intersection-api)
   - [GET /api/intersections](#21-get-apiintersections)
   - [PUT /api/intersections/{id}/operating-mode](#22-put-apiintersectionsiidoperating-mode)
   - [POST /api/intersections/{id}/adaptive-signals](#23-post-apiintersectionsiidadaptive-signals)
   - [GET /api/intersections/{id}/lanes](#24-get-apiintersectionsiidlanes)
   - [GET /api/intersections/{id}/signal-history](#25-get-apiintersectionsiidsignal-history)
3. [Camera API](#3-camera-api)
   - [GET /api/cameras](#31-get-apicameras)
4. [Traffic Log API](#4-traffic-log-api)
   - [POST /api/traffic-logs](#41-post-apitraffic-logs)
   - [GET /api/traffic-logs](#42-get-apitraffic-logs)
5. [Signal History API](#5-signal-history-api)
   - [GET /api/signal-history](#51-get-apisignal-history)
6. [Dashboard (Server-side View)](#6-dashboard-server-side-view)
7. [WebSocket](#7-websocket)
8. [Enums Reference](#8-enums-reference)
9. [Error Response Format](#9-error-response-format)

---

## 1. Auth API

**Base path:** `/auth`
**Controller:** `AuthController`

---

### 1.1 POST /auth/register

Đăng ký tài khoản người dùng mới.

**Request**

| Field      | Type   | Required | Validation                       |
| ---------- | ------ | -------- | -------------------------------- |
| `username` | String | ✅       | NotBlank, độ dài 3–100 ký tự     |
| `email`    | String | ✅       | NotBlank, định dạng email hợp lệ |
| `password` | String | ✅       | NotBlank, tối thiểu 6 ký tự      |
| `fullName` | String | ❌       | Không bắt buộc                   |

**Request Body Example**

```json
{
  "username": "admin01",
  "email": "admin01@example.com",
  "password": "secret123",
  "fullName": "Nguyen Van A"
}
```

**Response `200 OK`**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "admin01",
  "role": "ROLE_USER"
}
```

| Field          | Type   | Mô tả                  |
| -------------- | ------ | ---------------------- |
| `accessToken`  | String | JWT access token       |
| `refreshToken` | String | JWT refresh token      |
| `username`     | String | Tên đăng nhập          |
| `role`         | String | Vai trò của người dùng |

---

### 1.2 POST /auth/login

Đăng nhập bằng username và password.

**Request**

| Field      | Type   | Required | Validation |
| ---------- | ------ | -------- | ---------- |
| `username` | String | ✅       | NotBlank   |
| `password` | String | ✅       | NotBlank   |

**Request Body Example**

```json
{
  "username": "admin01",
  "password": "secret123"
}
```

**Response `200 OK`**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "admin01",
  "role": "ROLE_ADMIN"
}
```

---

### 1.3 POST /auth/refresh-token

Làm mới access token bằng refresh token còn hợp lệ.

**Request**

| Field          | Type   | Required | Validation |
| -------------- | ------ | -------- | ---------- |
| `refreshToken` | String | ✅       | NotBlank   |

**Request Body Example**

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Response `200 OK`**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...(new)",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...(new)",
  "username": "admin01",
  "role": "ROLE_ADMIN"
}
```

---

## 2. Intersection API

**Base path:** `/api/intersections`
**Controller:** `IntersectionController`
**Tag:** `Intersection API`

---

### 2.1 GET /api/intersections

Lấy danh sách tất cả các nút giao thông.

**Request:** Không có body, không có params.

**Response `200 OK`** — Mảng `IntersectionResponse[]`

```json
[
  {
    "id": 1,
    "name": "Ngã tư Hòa Bình",
    "operatingMode": "AI_AUTO",
    "createdAt": "2024-01-15T08:30:00"
  },
  {
    "id": 2,
    "name": "Ngã tư Lê Lợi",
    "operatingMode": "MANUAL_OVERRIDE",
    "createdAt": "2024-01-16T09:00:00"
  }
]
```

| Field           | Type          | Mô tả                     |
| --------------- | ------------- | ------------------------- |
| `id`            | Long          | ID nút giao               |
| `name`          | String        | Tên nút giao              |
| `operatingMode` | OperatingMode | Chế độ hoạt động hiện tại |
| `createdAt`     | LocalDateTime | Thời điểm tạo             |

**Response Codes**

| Code | Mô tả                    |
| ---- | ------------------------ |
| 200  | Lấy danh sách thành công |
| 500  | Lỗi server nội bộ        |

---

### 2.2 PUT /api/intersections/{id}/operating-mode

Cập nhật chế độ hoạt động của nút giao (Manual Override).

**Path Parameter**

| Param | Type | Mô tả       |
| ----- | ---- | ----------- |
| `id`  | Long | ID nút giao |

**Request Body**

| Field           | Type          | Required | Mô tả                                                  |
| --------------- | ------------- | -------- | ------------------------------------------------------ |
| `operatingMode` | OperatingMode | ✅       | Chế độ mới: `AI_AUTO`, `FIXED_TIME`, `MANUAL_OVERRIDE` |

**Request Body Example**

```json
{
  "operatingMode": "MANUAL_OVERRIDE"
}
```

**Response `200 OK`** — `IntersectionResponse`

```json
{
  "id": 1,
  "name": "Ngã tư Hòa Bình",
  "operatingMode": "MANUAL_OVERRIDE",
  "createdAt": "2024-01-15T08:30:00"
}
```

**Response Codes**

| Code | Mô tả                      |
| ---- | -------------------------- |
| 200  | Cập nhật chế độ thành công |
| 400  | Request body không hợp lệ  |
| 404  | Không tìm thấy nút giao    |
| 500  | Lỗi server nội bộ          |

---

### 2.3 POST /api/intersections/{id}/adaptive-signals

Kích hoạt thủ công quá trình tính toán tín hiệu thích ứng AI cho nút giao. Dùng cho mục đích test hoặc trigger thủ công ngoài lịch cron.

**Path Parameter**

| Param | Type | Mô tả       |
| ----- | ---- | ----------- |
| `id`  | Long | ID nút giao |

**Request:** Không có body.

**Response `200 OK`** — Body rỗng (HTTP 200 no content)

**Response Codes**

| Code | Mô tả                               |
| ---- | ----------------------------------- |
| 200  | Xử lý tín hiệu thích ứng thành công |
| 404  | Không tìm thấy nút giao             |
| 500  | Lỗi server nội bộ                   |

> **Lưu ý:** Endpoint này chỉ thực sự xử lý AI nếu nút giao đang ở chế độ `AI_AUTO`. Các chế độ khác (`FIXED_TIME`, `MANUAL_OVERRIDE`) sẽ bị bỏ qua bởi `OperatingModeGuard`.

---

### 2.4 GET /api/intersections/{id}/lanes

Lấy danh sách làn đường của một nút giao.

**Path Parameter**

| Param | Type | Mô tả       |
| ----- | ---- | ----------- |
| `id`  | Long | ID nút giao |

**Response `200 OK`** — Mảng `LaneResponse[]`

```json
[
  {
    "id": 101,
    "directionName": "NORTH_SOUTH",
    "opposingLaneId": 102
  },
  {
    "id": 102,
    "directionName": "EAST_WEST",
    "opposingLaneId": 101
  }
]
```

| Field            | Type   | Mô tả                                       |
| ---------------- | ------ | ------------------------------------------- |
| `id`             | Long   | ID làn đường                                |
| `directionName`  | String | Tên hướng làn (e.g. NORTH_SOUTH, EAST_WEST) |
| `opposingLaneId` | Long   | ID làn đối diện                             |

---

### 2.5 GET /api/intersections/{id}/signal-history

Lấy lịch sử tín hiệu đèn của một nút giao.

**Path Parameter**

| Param | Type | Mô tả       |
| ----- | ---- | ----------- |
| `id`  | Long | ID nút giao |

**Response `200 OK`** — Mảng `SignalHistoryResponse[]`

```json
[
  {
    "id": 501,
    "intersectionId": 1,
    "laneId": 101,
    "greenDuration": 45,
    "redDuration": 30,
    "appliedAt": "2024-01-15T10:00:00"
  }
]
```

| Field            | Type          | Mô tả                              |
| ---------------- | ------------- | ---------------------------------- |
| `id`             | Long          | ID bản ghi lịch sử                 |
| `intersectionId` | Long          | ID nút giao                        |
| `laneId`         | Long          | ID làn đường được áp dụng tín hiệu |
| `greenDuration`  | Integer       | Thời gian đèn xanh (giây)          |
| `redDuration`    | Integer       | Thời gian đèn đỏ (giây)            |
| `appliedAt`      | LocalDateTime | Thời điểm áp dụng tín hiệu         |

---

## 3. Camera API

**Base path:** `/api/cameras`
**Controller:** `CameraController`
**Tag:** `Camera API`

---

### 3.1 GET /api/cameras

Lấy danh sách tất cả camera trong hệ thống.

**Request:** Không có body, không có params.

**Response `200 OK`** — Mảng `CameraResponse[]`

```json
[
  {
    "id": 1,
    "ipAddress": "192.168.1.101",
    "status": "ONLINE",
    "laneId": 101
  },
  {
    "id": 2,
    "ipAddress": "192.168.1.102",
    "status": "OFFLINE",
    "laneId": 102
  }
]
```

| Field       | Type         | Mô tả                        |
| ----------- | ------------ | ---------------------------- |
| `id`        | Long         | ID camera                    |
| `ipAddress` | String       | Địa chỉ IP của camera        |
| `status`    | CameraStatus | Trạng thái camera            |
| `laneId`    | Long         | ID làn đường camera theo dõi |

**CameraStatus values:** `ONLINE` | `OFFLINE` | `MAINTENANCE`

---

## 4. Traffic Log API

**Base path:** `/api/traffic-logs`
**Controller:** `TrafficLogController`
**Tag:** `Traffic Log API`

---

### 4.1 POST /api/traffic-logs

Ghi nhận log mật độ phương tiện từ Camera AI. Sau khi nhận log, hệ thống tự động tính toán và cập nhật tín hiệu đèn thích ứng (nếu nút giao đang ở chế độ `AI_AUTO`).

**Request Body**

| Field             | JSON Key        | Type    | Required | Validation           |
| ----------------- | --------------- | ------- | -------- | -------------------- |
| `laneId`          | `lane_id`       | Long    | ✅       | NotNull              |
| `vehicleCount`    | `vehicle_count` | Integer | ✅       | NotNull, >= 0        |
| `congestionLevel` | `congestion`    | Double  | ✅       | NotNull, 0.0 – 100.0 |

> **Lưu ý:** Tên field trong JSON sử dụng `snake_case` (`lane_id`, `vehicle_count`, `congestion`).

**Request Body Example**

```json
{
  "lane_id": 101,
  "vehicle_count": 35,
  "congestion": 72.5
}
```

**Response `200 OK`** — Body rỗng

**Response Codes**

| Code | Mô tả                     |
| ---- | ------------------------- |
| 200  | Ghi log thành công        |
| 400  | Request body không hợp lệ |
| 404  | Không tìm thấy làn đường  |
| 500  | Lỗi server nội bộ         |

---

### 4.2 GET /api/traffic-logs

Lấy toàn bộ lịch sử traffic log trong hệ thống.

**Request:** Không có body, không có params.

**Response `200 OK`** — Mảng `TrafficLogResponse[]`

```json
[
  {
    "id": 1001,
    "laneId": 101,
    "vehicleCount": 35,
    "congestionLevel": 72.5,
    "recordedAt": "2024-01-15T10:05:00"
  }
]
```

| Field             | Type          | Mô tả                          |
| ----------------- | ------------- | ------------------------------ |
| `id`              | Long          | ID bản ghi log                 |
| `laneId`          | Long          | ID làn đường                   |
| `vehicleCount`    | Integer       | Số lượng phương tiện ghi nhận  |
| `congestionLevel` | Double        | Mức độ tắc nghẽn (0.0 – 100.0) |
| `recordedAt`      | LocalDateTime | Thời điểm ghi log              |

---

## 5. Signal History API

**Base path:** `/api/signal-history`
**Controller:** `SignalHistoryController`
**Tag:** `Signal History API`

---

### 5.1 GET /api/signal-history

Lấy toàn bộ lịch sử tín hiệu đèn của tất cả các nút giao.

**Request:** Không có body, không có params.

**Response `200 OK`** — Mảng `SignalHistoryResponse[]`

```json
[
  {
    "id": 501,
    "intersectionId": 1,
    "laneId": 101,
    "greenDuration": 45,
    "redDuration": 30,
    "appliedAt": "2024-01-15T10:00:00"
  },
  {
    "id": 502,
    "intersectionId": 2,
    "laneId": 201,
    "greenDuration": 20,
    "redDuration": 55,
    "appliedAt": "2024-01-15T10:00:05"
  }
]
```

| Field            | Type          | Mô tả                              |
| ---------------- | ------------- | ---------------------------------- |
| `id`             | Long          | ID bản ghi lịch sử                 |
| `intersectionId` | Long          | ID nút giao liên quan              |
| `laneId`         | Long          | ID làn đường được áp dụng tín hiệu |
| `greenDuration`  | Integer       | Thời gian đèn xanh (giây)          |
| `redDuration`    | Integer       | Thời gian đèn đỏ (giây)            |
| `appliedAt`      | LocalDateTime | Thời điểm áp dụng tín hiệu         |

---

## 6. Dashboard (Server-side View)

**Controller:** `DashboardController`

| Method | Endpoint     | Mô tả                                                        |
| ------ | ------------ | ------------------------------------------------------------ |
| GET    | `/dashboard` | Trả về Thymeleaf view `dashboard` (không phải JSON REST API) |

> Đây là server-side rendered view, không phải REST API endpoint.

---

## 7. WebSocket

Hệ thống sử dụng **STOMP over SockJS** để push tín hiệu đèn real-time đến client.

### Kết nối

| Thông số | Giá trị                          |
| -------- | -------------------------------- |
| Endpoint | `ws://localhost:8080/traffic-ws` |
| Fallback | SockJS                           |
| Protocol | STOMP                            |

### Subscribe Topic

| Topic            | Mô tả                                                    |
| ---------------- | -------------------------------------------------------- |
| `/topic/traffic` | Nhận `SignalMessage` theo thời gian thực từ AI Scheduler |

### SignalMessage (WebSocket payload)

```json
{
  "intersectionId": 1,
  "laneId": 101,
  "direction": "NORTH_SOUTH",
  "signal": "GREEN",
  "greenDuration": 45,
  "redDuration": 30,
  "remaining": 45,
  "trafficLevel": "HIGH"
}
```

| Field            | Type    | Mô tả                                             |
| ---------------- | ------- | ------------------------------------------------- |
| `intersectionId` | Long    | ID nút giao                                       |
| `laneId`         | Long    | ID làn đường                                      |
| `direction`      | String  | Hướng làn đường (e.g. `NORTH_SOUTH`, `EAST_WEST`) |
| `signal`         | String  | Tín hiệu hiện tại: `GREEN` hoặc `RED`             |
| `greenDuration`  | Integer | Thời gian đèn xanh (giây)                         |
| `redDuration`    | Integer | Thời gian đèn đỏ (giây)                           |
| `remaining`      | Integer | Thời gian còn lại (giây)                          |
| `trafficLevel`   | String  | Mức tắc nghẽn: `LOW`, `MEDIUM`, `HIGH`            |

### Scheduler Trigger

Hệ thống tự động tính toán và broadcast tín hiệu mỗi **5 giây** (`@Scheduled(fixedRate = 5000)`) thông qua `TrafficSignalScheduler`. Chỉ các nút giao ở chế độ `AI_AUTO` mới được xử lý.

---

## 8. Enums Reference

### OperatingMode

| Giá trị           | AI Processing     | Mô tả                                      |
| ----------------- | ----------------- | ------------------------------------------ |
| `AI_AUTO`         | ✅ Cho phép       | Hệ thống AI tự động điều phối tín hiệu đèn |
| `FIXED_TIME`      | ❌ Không cho phép | Đèn hoạt động theo lịch thời gian cố định  |
| `MANUAL_OVERRIDE` | ❌ Không cho phép | Điều khiển thủ công bởi người vận hành     |

### CameraStatus

| Giá trị       | Mô tả                  |
| ------------- | ---------------------- |
| `ONLINE`      | Camera đang hoạt động  |
| `OFFLINE`     | Camera ngừng hoạt động |
| `MAINTENANCE` | Camera đang bảo trì    |

---

## 9. Error Response Format

Tất cả lỗi đều trả về cùng một cấu trúc JSON do `GlobalExceptionHandler` xử lý:

```json
{
  "timestamp": "2024-01-15T10:05:30.123",
  "status": 404,
  "error": "Not Found",
  "message": "Intersection with ID 99 not found"
}
```

| Field       | Type          | Mô tả                |
| ----------- | ------------- | -------------------- |
| `timestamp` | LocalDateTime | Thời điểm xảy ra lỗi |
| `status`    | Integer       | HTTP status code     |
| `error`     | String        | Tên loại lỗi HTTP    |
| `message`   | String        | Mô tả chi tiết lỗi   |

### Bảng tổng hợp mã lỗi

| HTTP Code | Error                 | Nguyên nhân                                                                         |
| --------- | --------------------- | ----------------------------------------------------------------------------------- |
| 400       | Bad Request           | Validation thất bại, RuntimeException chưa được phân loại                           |
| 404       | Not Found             | `IntersectionNotFoundException`, `LaneNotFoundException`                            |
| 409       | Conflict              | `DuplicateIntersectionException`, `DataIntegrityViolationException` (trùng dữ liệu) |
| 500       | Internal Server Error | Lỗi hệ thống nội bộ chưa được bắt                                                   |

---

## Quick Reference

| Method | Endpoint                                   | Mô tả                                | Auth |
| ------ | ------------------------------------------ | ------------------------------------ | ---- |
| POST   | `/auth/register`                           | Đăng ký tài khoản                    | ❌   |
| POST   | `/auth/login`                              | Đăng nhập                            | ❌   |
| POST   | `/auth/refresh-token`                      | Làm mới token                        | ❌   |
| GET    | `/api/intersections`                       | Lấy danh sách nút giao               | ❌   |
| PUT    | `/api/intersections/{id}/operating-mode`   | Cập nhật chế độ hoạt động            | ❌   |
| POST   | `/api/intersections/{id}/adaptive-signals` | Trigger AI xử lý tín hiệu (test)     | ❌   |
| GET    | `/api/intersections/{id}/lanes`            | Lấy danh sách làn đường              | ❌   |
| GET    | `/api/intersections/{id}/signal-history`   | Lấy lịch sử tín hiệu của nút giao    | ❌   |
| GET    | `/api/cameras`                             | Lấy danh sách camera                 | ❌   |
| POST   | `/api/traffic-logs`                        | Ghi log mật độ phương tiện từ AI cam | ❌   |
| GET    | `/api/traffic-logs`                        | Lấy tất cả traffic log               | ❌   |
| GET    | `/api/signal-history`                      | Lấy tất cả lịch sử tín hiệu          | ❌   |
| WS     | `ws://localhost:8080/traffic-ws`           | Kết nối WebSocket STOMP              | ❌   |
| SUB    | `/topic/traffic`                           | Subscribe nhận tín hiệu real-time    | ❌   |

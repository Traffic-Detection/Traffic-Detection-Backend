# Traffic Detection Backend — API Document

> **Base URL:** `http://localhost:8080`
> **Version:** 2.0.0
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
   - [WS /topic/traffic-logs](#43-ws-topictraffic-logs-real-time-stream)
5. [Signal History API](#5-signal-history-api)
   - [GET /api/signal-history](#51-get-apisignal-history)
6. [Signal Config API](#6-signal-config-api)
   - [GET /api/signal-configs](#61-get-apisignal-configs)
   - [POST /api/signal-configs](#62-post-apisignal-configs)
   - [PUT /api/signal-configs/{id}](#63-put-apisignal-configsid)
   - [DELETE /api/signal-configs/{id}](#64-delete-apisignal-configsid)
7. [Dashboard (Server-side View)](#7-dashboard-server-side-view)
8. [WebSocket](#8-websocket)
   - [Signal Updates — /topic/signal](#81-topic-signal--tín-hiệu-đèn-real-time)
   - [Traffic Logs — /topic/traffic-logs](#82-topictraffic-logs--traffic-log-real-time)
9. [Enums Reference](#9-enums-reference)
10. [Error Response Format](#10-error-response-format)

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
    "operatingMode": "AI",
    "createdAt": "2024-01-15T08:30:00"
  },
  {
    "id": 2,
    "name": "Ngã tư Lê Lợi",
    "operatingMode": "MANUAL",
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

Cập nhật chế độ hoạt động của nút giao.

**Path Parameter**

| Param | Type | Mô tả       |
| ----- | ---- | ----------- |
| `id`  | Long | ID nút giao |

**Request Body**

| Field           | Type          | Required | Mô tả                    |
| --------------- | ------------- | -------- | ------------------------ |
| `operatingMode` | OperatingMode | ✅       | Chế độ mới: `AI`, `MANUAL` |

**Request Body Example**

```json
{
  "operatingMode": "MANUAL"
}
```

**Response `200 OK`** — `IntersectionResponse`

```json
{
  "id": 1,
  "name": "Ngã tư Hòa Bình",
  "operatingMode": "MANUAL",
  "createdAt": "2024-01-15T08:30:00"
}
```

**Business Rules**

| Chuyển mode | Hành vi |
|-------------|----------|
| Bất kỳ → `MANUAL` | Hệ thống **validate** rằng `signal_configs` đã được thiết lập đầy đủ cho tất cả lanes. Nếu thiếu → trả `400`. |
| `MANUAL` → `AI` | Hệ thống **chờ hết chu kỳ đèn** MANUAL hiện tại (green + yellow + red) trước khi bắt đầu xử lý AI. Cache MANUAL bị invalidate. |
| `AI` → `MANUAL` | Cache MANUAL bị invalidate. Pending AI activation (nếu có) bị xoá. |

**Response Codes**

| Code | Mô tả                      |
| ---- | -------------------------- |
| 200  | Cập nhật chế độ thành công |
| 400  | Request body không hợp lệ hoặc thiếu signal_configs cho MANUAL  |
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

> **Lưu ý:** Endpoint này chỉ thực sự xử lý AI nếu nút giao đang ở chế độ `AI`. Chế độ `MANUAL` sẽ bị bỏ qua bởi `OperatingModeGuard`.

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
    "yellowDuration": 3,
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
| `yellowDuration` | Integer       | Thời gian đèn vàng (giây)          |
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

Ghi nhận log mật độ phương tiện từ Camera AI.

**Luồng xử lý sau khi nhận request:**
1. Lưu `TrafficLog` vào database.
2. **Tự động broadcast** `TrafficLogResponse` đến tất cả WebSocket client đang subscribe `/topic/traffic-logs`.
3. Nếu nút giao ở chế độ `AI_AUTO`, tính toán và cập nhật tín hiệu đèn thích ứng.

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
| 200  | Ghi log thành công + broadcast WS |
| 400  | Request body không hợp lệ |
| 404  | Không tìm thấy làn đường  |
| 500  | Lỗi server nội bộ         |

---

### 4.2 GET /api/traffic-logs

Lấy toàn bộ lịch sử traffic log trong hệ thống. **Dùng cho initial load** — để nhận dữ liệu liên tục theo thời gian thực, hãy subscribe WebSocket topic `/topic/traffic-logs` (xem [4.3](#43-ws-topictraffic-logs-real-time-stream)).

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

### 4.3 WS /topic/traffic-logs — Real-time Stream

Nhận traffic log liên tục qua WebSocket (STOMP). Có **hai cơ chế** nhận dữ liệu:

#### Cơ chế 1: Push tự động (Event-driven)
Mỗi khi Camera AI gửi `POST /api/traffic-logs`, server **tự động broadcast** log mới đến tất cả client đang subscribe `/topic/traffic-logs`. Client không cần làm gì thêm.

#### Cơ chế 2: Request snapshot (On-demand)
Client gửi message đến `/app/traffic-logs` để yêu cầu toàn bộ danh sách log hiện tại. Server sẽ broadcast list đến `/topic/traffic-logs`.

**Kết nối & Subscribe (JavaScript)**

```javascript
const socket = new SockJS('http://localhost:8080/traffic-ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, () => {
  // Subscribe nhận traffic log real-time
  stompClient.subscribe('/topic/traffic-logs', (message) => {
    const data = JSON.parse(message.body);
    // data có thể là 1 object (log mới) hoặc array (snapshot)
    console.log('Traffic log update:', data);
  });

  // (Tuỳ chọn) Request snapshot toàn bộ logs ngay khi kết nối
  stompClient.send('/app/traffic-logs', {}, '');
});
```

**Payload nhận được — TrafficLogResponse (log mới đơn lẻ)**

```json
{
  "id": 1001,
  "laneId": 101,
  "vehicleCount": 35,
  "congestionLevel": 72.5,
  "recordedAt": "2024-01-15T10:05:00"
}
```

**Payload nhận được — TrafficLogResponse[] (khi request snapshot)**

```json
[
  {
    "id": 1000,
    "laneId": 102,
    "vehicleCount": 12,
    "congestionLevel": 25.0,
    "recordedAt": "2024-01-15T10:04:00"
  },
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

> **Khuyến nghị:** Dùng `GET /api/traffic-logs` để load dữ liệu ban đầu khi trang khởi động, sau đó subscribe `/topic/traffic-logs` để nhận cập nhật real-time mà không cần polling.

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
    "yellowDuration": 3,
    "redDuration": 30,
    "appliedAt": "2024-01-15T10:00:00"
  },
  {
    "id": 502,
    "intersectionId": 2,
    "laneId": 201,
    "greenDuration": 20,
    "yellowDuration": 3,
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
| `yellowDuration` | Integer       | Thời gian đèn vàng (giây)          |
| `redDuration`    | Integer       | Thời gian đèn đỏ (giây)            |
| `appliedAt`      | LocalDateTime | Thời điểm áp dụng tín hiệu         |

---

## 6. Signal Config API

**Base path:** `/api/signal-configs`
**Controller:** `SignalConfigController`
**Tag:** `Signal Config API`

> Quản lý cấu hình thời lượng đèn cho chế độ MANUAL. Mỗi lane trong một intersection cần có 1 bản ghi `signal_config` trước khi có thể chuyển sang chế độ MANUAL.

---

### 6.1 GET /api/signal-configs

Lấy danh sách cấu hình đèn theo ngã tư.

**Query Parameter**

| Param            | Type | Required | Mô tả       |
| ---------------- | ---- | -------- | ----------- |
| `intersectionId` | Long | ✅       | ID nút giao |

**Request Example**

```
GET /api/signal-configs?intersectionId=1
```

**Response `200 OK`** — Mảng `SignalConfigResponse[]`

```json
[
  {
    "id": 1,
    "intersectionId": 1,
    "laneId": 1,
    "greenDuration": 45,
    "yellowDuration": 5,
    "redDuration": 40
  },
  {
    "id": 2,
    "intersectionId": 1,
    "laneId": 2,
    "greenDuration": 40,
    "yellowDuration": 5,
    "redDuration": 45
  }
]
```

| Field            | Type    | Mô tả                     |
| ---------------- | ------- | ------------------------- |
| `id`             | Long    | ID cấu hình               |
| `intersectionId` | Long    | ID nút giao               |
| `laneId`         | Long    | ID làn đường              |
| `greenDuration`  | Integer | Thời gian đèn xanh (giây) |
| `yellowDuration` | Integer | Thời gian đèn vàng (giây) |
| `redDuration`    | Integer | Thời gian đèn đỏ (giây)   |

---

### 6.2 POST /api/signal-configs

Tạo cấu hình đèn mới cho một làn đường. Mỗi cặp (intersection, lane) chỉ được có 1 config.

**Request Body**

| Field            | Type    | Required | Validation            |
| ---------------- | ------- | -------- | --------------------- |
| `intersectionId` | Long    | ✅       | NotNull               |
| `laneId`         | Long    | ✅       | NotNull               |
| `greenDuration`  | Integer | ✅       | NotNull, Min(1)       |
| `yellowDuration` | Integer | ✅       | NotNull, Min(1)       |
| `redDuration`    | Integer | ✅       | NotNull, Min(1)       |

**Request Body Example**

```json
{
  "intersectionId": 1,
  "laneId": 1,
  "greenDuration": 45,
  "yellowDuration": 5,
  "redDuration": 40
}
```

**Response `200 OK`** — `SignalConfigResponse`

```json
{
  "id": 1,
  "intersectionId": 1,
  "laneId": 1,
  "greenDuration": 45,
  "yellowDuration": 5,
  "redDuration": 40
}
```

**Response Codes**

| Code | Mô tả                                       |
| ---- | ------------------------------------------- |
| 200  | Tạo thành công                               |
| 400  | Validation thất bại hoặc config đã tồn tại   |
| 404  | Intersection hoặc Lane không tồn tại         |
| 500  | Lỗi server nội bộ                            |

> **Lưu ý:** Khi tạo/cập nhật/xoá config, cache MANUAL của intersection tương ứng sẽ tự động bị invalidate.

---

### 6.3 PUT /api/signal-configs/{id}

Cập nhật thời lượng đèn cho một cấu hình đã tồn tại.

**Path Parameter**

| Param | Type | Mô tả       |
| ----- | ---- | ----------- |
| `id`  | Long | ID cấu hình |

**Request Body** — Giống [6.2 POST](#62-post-apisignal-configs)

**Response `200 OK`** — `SignalConfigResponse`

**Response Codes**

| Code | Mô tả                           |
| ---- | ------------------------------- |
| 200  | Cập nhật thành công              |
| 400  | Validation thất bại              |
| 404  | Không tìm thấy cấu hình         |
| 500  | Lỗi server nội bộ               |

---

### 6.4 DELETE /api/signal-configs/{id}

Xoá cấu hình đèn.

**Path Parameter**

| Param | Type | Mô tả       |
| ----- | ---- | ----------- |
| `id`  | Long | ID cấu hình |

**Response `200 OK`** — Body rỗng

**Response Codes**

| Code | Mô tả                           |
| ---- | ------------------------------- |
| 200  | Xoá thành công                   |
| 404  | Không tìm thấy cấu hình         |
| 500  | Lỗi server nội bộ               |

> ⚠️ **Cẩn trọng:** Nếu xoá config trong khi intersection đang ở mode MANUAL, cần đảm bảo vẫn còn đủ config cho tất cả lanes, nếu không sẽ không thể chuyển lại MANUAL.

---

## 7. Dashboard (Server-side View)

**Controller:** `DashboardController`

| Method | Endpoint     | Mô tả                                                        |
| ------ | ------------ | ------------------------------------------------------------ |
| GET    | `/dashboard` | Trả về Thymeleaf view `dashboard` (không phải JSON REST API) |

> Đây là server-side rendered view, không phải REST API endpoint.

---

## 8. WebSocket

Hệ thống sử dụng **STOMP over SockJS** để push dữ liệu real-time đến client.

### Thông tin kết nối

| Thông số              | Giá trị                          |
| --------------------- | -------------------------------- |
| Endpoint              | `ws://localhost:8080/traffic-ws` |
| Fallback              | SockJS                           |
| Protocol              | STOMP                            |
| App destination prefix | `/app`                          |
| Broker prefix         | `/topic`                         |

### Tổng quan các Topic & Message Mapping

| Loại    | Địa chỉ                 | Payload                    | Trigger                                        |
| ------- | ----------------------- | -------------------------- | ---------------------------------------------- |
| SUB     | `/topic/signal`         | `SignalMessage`            | AI Scheduler mỗi 5s (AI) hoặc 1 lần (MANUAL)  |
| SUB     | `/topic/traffic-logs`   | `TrafficLogResponse` hoặc `TrafficLogResponse[]` | POST log mới hoặc client gửi `/app/traffic-logs` |
| SEND    | `/app/traffic-logs`     | _(body rỗng)_              | Client yêu cầu snapshot toàn bộ logs           |

---

### 8.1 /topic/signal — Tín hiệu đèn real-time

**Trigger:** `TrafficSignalScheduler` chạy mỗi **5 giây** (`@Scheduled(fixedRate = 5000)`).
- **Mode AI:** Broadcast mỗi 5 giây với giá trị thích ứng.
- **Mode MANUAL:** Broadcast **1 lần duy nhất** với giá trị cố định từ `signal_configs`. Không gửi lại trừ khi admin cập nhật config.

**Payload — SignalMessage (mode AI)**

```json
{
  "intersectionId": 1,
  "laneId": 101,
  "direction": "NORTH_SOUTH",
  "signal": "GREEN",
  "greenDuration": 45,
  "yellowDuration": 3,
  "redDuration": 30,
  "remaining": 45,
  "trafficLevel": "HIGH"
}
```

**Payload — SignalMessage (mode MANUAL)**

```json
{
  "intersectionId": 1,
  "laneId": 101,
  "direction": "NORTH_SOUTH",
  "signal": "FIXED",
  "greenDuration": 45,
  "yellowDuration": 5,
  "redDuration": 40,
  "remaining": 45,
  "trafficLevel": "MANUAL_MODE"
}
```

| Field            | Type    | Mô tả                                             |
| ---------------- | ------- | ------------------------------------------------- |
| `intersectionId` | Long    | ID nút giao                                       |
| `laneId`         | Long    | ID làn đường                                      |
| `direction`      | String  | Hướng làn đường (e.g. `NORTH_SOUTH`, `EAST_WEST`) |
| `signal`         | String  | Tín hiệu hiện tại: `GREEN`, `RED`, hoặc `FIXED`   |
| `greenDuration`  | Integer | Thời gian đèn xanh (giây)                         |
| `yellowDuration` | Integer | Thời gian đèn vàng (giây)                         |
| `redDuration`    | Integer | Thời gian đèn đỏ (giây)                           |
| `remaining`      | Integer | Thời gian còn lại (giây)                          |
| `trafficLevel`   | String  | Mức tắc nghẽn: `LOW`, `MEDIUM`, `HIGH`, `MANUAL_MODE` |

---

### 8.2 /topic/traffic-logs — Traffic Log real-time

**Trigger:** Có hai nguồn kích hoạt:
- **Event-driven:** Mỗi khi `POST /api/traffic-logs` được gọi → server broadcast `TrafficLogResponse` (đơn lẻ) đến topic này.
- **On-demand:** Client gửi message đến `/app/traffic-logs` → server broadcast `TrafficLogResponse[]` (toàn bộ danh sách).

**Payload — TrafficLogResponse (đơn lẻ, nhận khi có log mới)**

```json
{
  "id": 1001,
  "laneId": 101,
  "vehicleCount": 35,
  "congestionLevel": 72.5,
  "recordedAt": "2024-01-15T10:05:00"
}
```

**Payload — TrafficLogResponse[] (nhận khi request snapshot)**

```json
[
  {
    "id": 1000,
    "laneId": 102,
    "vehicleCount": 12,
    "congestionLevel": 25.0,
    "recordedAt": "2024-01-15T10:04:00"
  },
  {
    "id": 1001,
    "laneId": 101,
    "vehicleCount": 35,
    "congestionLevel": 72.5,
    "recordedAt": "2024-01-15T10:05:00"
  }
]
```

**Ví dụ client (JavaScript — STOMP/SockJS)**

```javascript
const socket = new SockJS('http://localhost:8080/traffic-ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, () => {
  // Bước 1: Subscribe nhận updates liên tục
  stompClient.subscribe('/topic/traffic-logs', (message) => {
    const data = JSON.parse(message.body);
    if (Array.isArray(data)) {
      // Snapshot: render toàn bộ danh sách
      renderAllLogs(data);
    } else {
      // Log mới: thêm vào đầu danh sách
      prependLog(data);
    }
  });

  // Bước 2: (Tuỳ chọn) Request snapshot dữ liệu ban đầu
  stompClient.send('/app/traffic-logs', {}, '');
});
```

> **Pattern khuyến nghị:**
> 1. Gọi `GET /api/traffic-logs` để render dữ liệu ban đầu.
> 2. Subscribe `/topic/traffic-logs` để nhận mọi log mới mà không cần polling.

---

## 9. Enums Reference

### OperatingMode

| Giá trị   | AI Processing     | Mô tả                                                    |
| --------- | ----------------- | -------------------------------------------------------- |
| `AI`      | ✅ Cho phép       | Hệ thống AI tự động điều phối tín hiệu đèn dựa trên traffic_log |
| `MANUAL`  | ❌ Không cho phép | Đèn hoạt động theo thời lượng cố định từ bảng signal_configs     |

### CameraStatus

| Giá trị       | Mô tả                  |
| ------------- | ---------------------- |
| `ONLINE`      | Camera đang hoạt động  |
| `OFFLINE`     | Camera ngừng hoạt động |
| `MAINTENANCE` | Camera đang bảo trì    |

---

## 10. Error Response Format

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
| PUT    | `/api/intersections/{id}/operating-mode`   | Chuyển chế độ AI/MANUAL              | ❌   |
| POST   | `/api/intersections/{id}/adaptive-signals` | Trigger AI xử lý tín hiệu (test)     | ❌   |
| GET    | `/api/intersections/{id}/lanes`            | Lấy danh sách làn đường              | ❌   |
| GET    | `/api/intersections/{id}/signal-history`   | Lấy lịch sử tín hiệu của nút giao    | ❌   |
| GET    | `/api/cameras`                             | Lấy danh sách camera                 | ❌   |
| POST   | `/api/traffic-logs`                        | Ghi log mật độ phương tiện + broadcast WS | ❌   |
| GET    | `/api/traffic-logs`                        | Lấy tất cả traffic log (initial load) | ❌   |
| GET    | `/api/signal-history`                      | Lấy tất cả lịch sử tín hiệu          | ❌   |
| GET    | `/api/signal-configs?intersectionId={id}`  | Lấy cấu hình đèn theo ngã tư         | ❌   |
| POST   | `/api/signal-configs`                      | Tạo cấu hình đèn cho 1 lane          | ❌   |
| PUT    | `/api/signal-configs/{id}`                 | Cập nhật thời lượng đèn               | ❌   |
| DELETE | `/api/signal-configs/{id}`                 | Xoá cấu hình đèn                     | ❌   |
| WS     | `ws://localhost:8080/traffic-ws`           | Kết nối WebSocket STOMP               | ❌   |
| SUB    | `/topic/signal`                            | Subscribe nhận tín hiệu đèn real-time | ❌   |
| SUB    | `/topic/traffic-logs`                      | Subscribe nhận traffic log real-time  | ❌   |
| SEND   | `/app/traffic-logs`                        | Request snapshot toàn bộ traffic logs | ❌   |
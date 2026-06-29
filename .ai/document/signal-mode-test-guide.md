# Hướng dẫn Test: Chế độ MANUAL & AI

> **Base URL:** `http://localhost:8080`
> **Công cụ:** Postman, curl, hoặc bất kỳ REST client nào

---

## Mục lục

1. [Điều kiện tiên quyết](#1-điều-kiện-tiên-quyết)
2. [Test Case 1: CRUD Signal Config](#2-test-case-1-crud-signal-config)
3. [Test Case 2: Chuyển sang chế độ MANUAL](#3-test-case-2-chuyển-sang-chế-độ-manual)
4. [Test Case 3: Chế độ MANUAL — WebSocket broadcast-once](#4-test-case-3-chế-độ-manual--websocket-broadcast-once)
5. [Test Case 4: Chuyển MANUAL → AI — Chờ hết chu kỳ](#5-test-case-4-chuyển-manual--ai--chờ-hết-chu-kỳ)
6. [Test Case 5: Chế độ AI — Tính toán thích ứng](#6-test-case-5-chế-độ-ai--tính-toán-thích-ứng)
7. [Test Case 6: Edge cases & Validation](#7-test-case-6-edge-cases--validation)
8. [WebSocket Test với JavaScript](#8-websocket-test-với-javascript)

---

## 1. Điều kiện tiên quyết

Trước khi test, cần đảm bảo hệ thống có ít nhất:
- **1 Intersection** (ngã tư) với `operatingMode = AI`
- **2+ Lanes** (làn đường) thuộc intersection đó, có `opposingLane` liên kết với nhau

### Kiểm tra dữ liệu hiện có

```
GET /api/intersections
```

Response mẫu:
```json
[
  {
    "id": 1,
    "name": "Ngã tư Hòa Bình",
    "operatingMode": "AI",
    "createdAt": "2024-01-15T08:30:00"
  }
]
```

```
GET /api/intersections/1/lanes
```

Response mẫu:
```json
[
  { "id": 1, "directionName": "NORTH_SOUTH", "opposingLaneId": 2, "intersectionId": 1 },
  { "id": 2, "directionName": "EAST_WEST", "opposingLaneId": 1, "intersectionId": 1 }
]
```

> **Lưu ý ID:** Ghi lại `intersectionId` và `laneId` thực tế của hệ thống bạn để thay thế trong các bước tiếp theo.

---

## 2. Test Case 1: CRUD Signal Config

### 2.1 Tạo cấu hình đèn cho lane 1

```
POST /api/signal-configs
Content-Type: application/json

{
  "intersectionId": 1,
  "laneId": 1,
  "greenDuration": 45,
  "yellowDuration": 5,
  "redDuration": 40
}
```

**Kỳ vọng:** `200 OK`
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

### 2.2 Tạo cấu hình đèn cho lane 2

```
POST /api/signal-configs
Content-Type: application/json

{
  "intersectionId": 1,
  "laneId": 2,
  "greenDuration": 40,
  "yellowDuration": 5,
  "redDuration": 45
}
```

### 2.3 Lấy danh sách cấu hình theo ngã tư

```
GET /api/signal-configs?intersectionId=1
```

**Kỳ vọng:** `200 OK` — Mảng 2 phần tử

### 2.4 Cập nhật cấu hình

```
PUT /api/signal-configs/1
Content-Type: application/json

{
  "intersectionId": 1,
  "laneId": 1,
  "greenDuration": 50,
  "yellowDuration": 3,
  "redDuration": 37
}
```

**Kỳ vọng:** `200 OK` — `greenDuration` = 50

### 2.5 Xoá cấu hình

```
DELETE /api/signal-configs/1
```

**Kỳ vọng:** `200 OK`

> ⚠️ **Sau khi test xong xoá, tạo lại config cho cả 2 lanes trước khi test tiếp!**

---

## 3. Test Case 2: Chuyển sang chế độ MANUAL

### 3.1 Chuyển mode sang MANUAL (thành công)

**Điều kiện:** Đã tạo đủ `signal_configs` cho tất cả lanes của intersection.

```
PUT /api/intersections/1/operating-mode
Content-Type: application/json

{
  "operatingMode": "MANUAL"
}
```

**Kỳ vọng:** `200 OK`
```json
{
  "id": 1,
  "name": "Ngã tư Hòa Bình",
  "operatingMode": "MANUAL",
  "createdAt": "2024-01-15T08:30:00"
}
```

### 3.2 Chuyển MANUAL khi CHƯA có đủ config (thất bại)

**Điều kiện:** Xoá hết hoặc chỉ có 1 config cho intersection có 2 lanes.

```
PUT /api/intersections/1/operating-mode
Content-Type: application/json

{
  "operatingMode": "MANUAL"
}
```

**Kỳ vọng:** `400 Bad Request`
```json
{
  "timestamp": "...",
  "status": 400,
  "error": "Bad Request",
  "message": "Lỗi: Chưa thiết lập đủ cấu hình thời lượng đèn (signal_configs) cho tất cả các làn. Không thể chuyển sang chế độ MANUAL!"
}
```

---

## 4. Test Case 3: Chế độ MANUAL — WebSocket broadcast-once

### Mục tiêu
Xác nhận rằng ở chế độ MANUAL, scheduler chỉ gửi WebSocket **1 lần duy nhất**, không gửi liên tục mỗi 5s.

### Bước thực hiện

1. **Kết nối WebSocket** (xem [phần 8](#8-websocket-test-với-javascript))

2. **Subscribe** `/topic/signal`

3. **Chuyển mode sang MANUAL:**
   ```
   PUT /api/intersections/1/operating-mode
   {"operatingMode": "MANUAL"}
   ```

4. **Quan sát WebSocket:** Sẽ nhận được **1 batch SignalMessage** với:
   ```json
   {
     "intersectionId": 1,
     "laneId": 1,
     "direction": "NORTH_SOUTH",
     "signal": "FIXED",
     "greenDuration": 45,
     "yellowDuration": 5,
     "redDuration": 40,
     "remaining": 45,
     "trafficLevel": "MANUAL_MODE"
   }
   ```

5. **Chờ 15-20 giây:** Không nhận thêm bất kỳ message nào cho intersection này.

6. **Kiểm tra log server:** Tìm dòng log:
   ```
   [MANUAL] Cấp phép phát sóng tín hiệu cho Nút giao: 1
   ```
   Dòng này chỉ xuất hiện **1 lần duy nhất**.

### Kỳ vọng
- ✅ Nhận 1 batch message (1 message/lane)
- ✅ Không nhận thêm message sau đó
- ✅ `signal` = `"FIXED"`, `trafficLevel` = `"MANUAL_MODE"`
- ✅ Không có bản ghi mới trong `signal_history`

---

## 5. Test Case 4: Chuyển MANUAL → AI — Chờ hết chu kỳ

### Mục tiêu
Xác nhận khi chuyển từ MANUAL sang AI, hệ thống chờ hết chu kỳ đèn hiện tại trước khi bắt đầu xử lý AI.

### Bước thực hiện

1. **Đảm bảo đang ở mode MANUAL** với config: `green=45s`, `yellow=5s`, `red=40s` → tổng chu kỳ = 90s.

2. **Chuyển sang AI:**
   ```
   PUT /api/intersections/1/operating-mode
   {"operatingMode": "AI"}
   ```

3. **Kiểm tra log server ngay lập tức:** Tìm dòng:
   ```
   [ModeSwitchManager] Intersection 1 sẽ chuyển sang AI sau 90000ms (tại epoch=...)
   ```

4. **Trong 90 giây tiếp theo:** Quan sát log scheduler:
   ```
   [Scheduler] Intersection 1 đang chờ hết chu kỳ MANUAL. Bỏ qua.
   [Scheduler] Finished - AI: 0, MANUAL: 0, Waiting: 1, duration: ...ms
   ```

5. **Sau 90 giây:** Scheduler bắt đầu xử lý AI:
   ```
   [ModeSwitchManager] Intersection 1 đã hết thời gian chờ. AI bắt đầu xử lý.
   [AI] Processing intersection: Ngã tư Hòa Bình
   ```

6. **WebSocket sẽ bắt đầu nhận signal messages** với `signal` = `"GREEN"` hoặc `"RED"`.

### Kỳ vọng
- ✅ Trong khoảng thời gian chờ: scheduler KHÔNG xử lý AI, counter `Waiting` > 0
- ✅ Sau khoảng thời gian = `cycleDurationMs`: AI bắt đầu xử lý bình thường
- ✅ WebSocket bắt đầu nhận `SignalMessage` với `trafficLevel` = `LOW`/`MEDIUM`/`HIGH`
- ✅ Bảng `signal_history` có bản ghi mới

---

## 6. Test Case 5: Chế độ AI — Tính toán thích ứng

### Mục tiêu
Xác nhận AI mode đọc `traffic_log`, tính toán, ghi `signal_history`, và gửi WebSocket.

### Bước thực hiện

1. **Đảm bảo mode = AI:**
   ```
   PUT /api/intersections/1/operating-mode
   {"operatingMode": "AI"}
   ```

2. **Gửi traffic log với mật độ CAO cho lane 1:**
   ```
   POST /api/traffic-logs
   {"lane_id": 1, "vehicle_count": 80, "congestion": 85.0}
   ```

3. **Gửi traffic log với mật độ THẤP cho lane 2:**
   ```
   POST /api/traffic-logs
   {"lane_id": 2, "vehicle_count": 10, "congestion": 15.0}
   ```

4. **Chờ scheduler tick (tối đa 5s)**

5. **Kiểm tra WebSocket:** Nhận 2 SignalMessage:
   - Lane 1 (congestion cao): `signal` = `"GREEN"`, `greenDuration` = 60
   - Lane 2 (congestion thấp): `signal` = `"RED"`, `redDuration` > 0

6. **Kiểm tra signal history:**
   ```
   GET /api/intersections/1/signal-history
   ```

   **Kỳ vọng:** Có bản ghi mới với `yellowDuration` = 3

### Kỳ vọng
- ✅ Lane có congestion cao nhận GREEN
- ✅ Lane có congestion thấp nhận RED
- ✅ `signal_history` có bản ghi mới (bao gồm `yellowDuration`)
- ✅ WebSocket message có `yellowDuration` = 3

---

## 7. Test Case 6: Edge cases & Validation

### 7.1 Tạo config trùng lane

```
POST /api/signal-configs
{"intersectionId": 1, "laneId": 1, "greenDuration": 30, "yellowDuration": 3, "redDuration": 30}
```
_(Khi đã tồn tại config cho lane 1)_

**Kỳ vọng:** `400 Bad Request` — "Cấu hình đèn cho làn đường này đã tồn tại!"

### 7.2 Tạo config với duration = 0

```
POST /api/signal-configs
{"intersectionId": 1, "laneId": 3, "greenDuration": 0, "yellowDuration": 0, "redDuration": 0}
```

**Kỳ vọng:** `400 Bad Request` — Validation error (min = 1)

### 7.3 Chuyển AI → MANUAL → AI nhanh

```
PUT /api/intersections/1/operating-mode
{"operatingMode": "MANUAL"}

PUT /api/intersections/1/operating-mode
{"operatingMode": "AI"}

PUT /api/intersections/1/operating-mode
{"operatingMode": "MANUAL"}
```

**Kỳ vọng:** Mỗi lần chuyển mode:
- Cache MANUAL bị invalidate
- Pending AI bị clear khi chuyển lại MANUAL
- Không có lỗi runtime

### 7.4 Cập nhật config khi đang MANUAL

1. Đang ở mode MANUAL
2. Cập nhật signal config:
   ```
   PUT /api/signal-configs/1
   {"intersectionId": 1, "laneId": 1, "greenDuration": 60, "yellowDuration": 3, "redDuration": 27}
   ```
3. **Kỳ vọng:** Cache bị invalidate → scheduler sẽ broadcast lại giá trị mới 1 lần

---

## 8. WebSocket Test với JavaScript

### Cách kết nối và subscribe

Mở browser console hoặc tạo file HTML để test:

```html
<!DOCTYPE html>
<html>
<head>
  <title>WebSocket Test</title>
  <script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
  <script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js"></script>
</head>
<body>
  <h2>Signal Messages</h2>
  <div id="messages" style="font-family: monospace; white-space: pre;"></div>

  <script>
    const socket = new SockJS('http://localhost:8080/traffic-ws');
    const stompClient = Stomp.over(socket);

    stompClient.connect({}, () => {
      console.log('✅ Connected to WebSocket');

      // Subscribe tín hiệu đèn
      stompClient.subscribe('/topic/signal', (message) => {
        const data = JSON.parse(message.body);
        const time = new Date().toLocaleTimeString();
        const div = document.getElementById('messages');
        div.innerHTML = `[${time}] Lane ${data.laneId}: ${data.signal} (G=${data.greenDuration}s Y=${data.yellowDuration}s R=${data.redDuration}s) - ${data.trafficLevel}\n` + div.innerHTML;
      });

      // Subscribe traffic logs
      stompClient.subscribe('/topic/traffic-logs', (message) => {
        console.log('📊 Traffic log:', JSON.parse(message.body));
      });
    });
  </script>
</body>
</html>
```

### Kiểm tra nhanh

| Hành động | Kỳ vọng trên WebSocket |
|-----------|------------------------|
| Mode = AI, có traffic_log | Nhận `SignalMessage` mỗi 5s với `signal`=GREEN/RED, `trafficLevel`=LOW/MEDIUM/HIGH |
| Mode = MANUAL | Nhận `SignalMessage` **1 lần** với `signal`=FIXED, `trafficLevel`=MANUAL_MODE |
| MANUAL → AI | Không nhận gì trong thời gian chờ, sau đó nhận AI signals |
| Cập nhật signal_configs khi MANUAL | Nhận lại 1 batch message mới |

---

## Checklist tổng hợp

| # | Test Case | Trạng thái |
|---|-----------|------------|
| 1 | CRUD Signal Config — Create | ☐ |
| 2 | CRUD Signal Config — Read | ☐ |
| 3 | CRUD Signal Config — Update | ☐ |
| 4 | CRUD Signal Config — Delete | ☐ |
| 5 | Chuyển MANUAL thành công | ☐ |
| 6 | Chuyển MANUAL thất bại (thiếu config) | ☐ |
| 7 | MANUAL broadcast-once qua WebSocket | ☐ |
| 8 | MANUAL không ghi signal_history | ☐ |
| 9 | MANUAL → AI chờ hết chu kỳ | ☐ |
| 10 | AI đọc traffic_log + tính toán | ☐ |
| 11 | AI ghi signal_history (có yellowDuration) | ☐ |
| 12 | AI gửi WebSocket (có yellowDuration) | ☐ |
| 13 | Edge case: config trùng lane | ☐ |
| 14 | Edge case: validation duration min=1 | ☐ |
| 15 | Edge case: chuyển mode nhanh liên tục | ☐ |
| 16 | Edge case: update config invalidate cache | ☐ |

# Kế hoạch thiết kế: Chế độ điều khiển đèn tín hiệu MANUAL & AI

## 1. Tổng quan

Hệ thống điều khiển đèn giao thông hỗ trợ **2 chế độ vận hành** cho mỗi ngã tư (`Intersection`):

| Chế độ | Nguồn dữ liệu thời lượng đèn | Có dùng `traffic_log`? | Ghi `signal_history`? |
|--------|-------------------------------|------------------------|-----------------------|
| **MANUAL** | Đọc từ bảng `signal_configs` (cố định) | ❌ Không | ❌ Không (tuỳ chọn) |
| **AI** | Thuật toán tính toán dựa trên `traffic_log` | ✅ Có | ✅ Có (mỗi chu kỳ) |

---

## 2. Phân tích hiện trạng code

### 2.1. Các thành phần hiện có

```
TrafficSignalScheduler (Scheduler - chạy mỗi 5s)
  ├── IntersectionRepository.findAllWithLanes()     → Lấy tất cả ngã tư có lane
  ├── OperatingModeGuard.isAiProcessingAllowed()     → Kiểm tra mode
  ├── TrafficAlgorithmServiceImpl.calculateAdaptiveSignal() → Thuật toán AI
  ├── SignalHistoryService.saveAll()                 → Lưu lịch sử
  └── WebSocketServiceImpl.sendSignalUpdates()       → Gửi real-time cho frontend
```

### 2.2. Vấn đề hiện tại

1. **Scheduler hiện tại CHỈ xử lý mode AI_AUTO**: Khi `OperatingModeGuard` trả `false` (tức mode MANUAL/FIXED_TIME), scheduler **skip hoàn toàn** intersection đó → Frontend không nhận được tín hiệu đèn nào khi ở mode MANUAL.

2. **Không có `SignalConfigRepository`**: Entity `SignalConfig` đã tạo nhưng chưa có Repository tương ứng để đọc dữ liệu thời lượng đèn cố định.

3. **Enum `OperatingMode` hiện có 3 giá trị**: `AI_AUTO`, `FIXED_TIME`, `MANUAL_OVERRIDE` — nhưng theo yêu cầu mới chỉ cần 2 mode chính: `MANUAL` và `AI`. Cần xác nhận lại enum.

4. **`TrafficAlgorithmServiceImpl`** hiện tại đã hoạt động đúng logic AI (đọc `traffic_log`, tính `green/red`, trả về `SignalHistory` + `SignalMessage`).

---

## 3. Luồng hoạt động chi tiết

### 3.1. Chế độ MANUAL

```
┌─────────────────────────────────────────────────────────────────┐
│                     MANUAL MODE FLOW                            │
└─────────────────────────────────────────────────────────────────┘

 [1] Scheduler chạy (mỗi 5s)
      │
 [2] Query: intersectionRepository.findAllWithLanes()
      │
 [3] Kiểm tra: intersection.getOperatingMode() == MANUAL?
      │                                                  
      ├── YES ────────────────────────────────────────────┐
      │                                                    │
 [4] Query bảng signal_configs:                            │
     SELECT * FROM signal_configs                          │
     WHERE intersection_id = ? AND lane_id = ?             │
      │                                                    │
 [5] Lấy green_duration, yellow_duration, red_duration     │
     (giá trị CỐ ĐỊNH, đã được admin cấu hình sẵn)       │
      │                                                    │
 [6] Đóng gói thành SignalMessage                          │
      │                                                    │
 [7] Gửi qua WebSocket: /topic/signal                     │
      │                                                    │
 [8] KHÔNG ghi signal_history (vì không thay đổi gì)      │
     (Tuỳ chọn: có thể ghi nếu muốn tracking)             │
      │                                                    │
      └─── Kết thúc chu kỳ cho intersection này ───────────┘
```

**Đặc điểm chính:**
- Chỉ đọc DB bảng `signal_configs` 1 lần khi bật mode (hoặc cache lại)
- Hoàn toàn lờ đi dữ liệu `traffic_log`
- Giá trị đèn là cố định cho đến khi admin thay đổi

**Tối ưu hoá (Cache):**
- Khi chuyển sang MANUAL, đọc `signal_configs` 1 lần và lưu vào memory (ConcurrentHashMap)
- Các lần scheduler chạy tiếp theo → đọc từ cache, KHÔNG query DB
- Cache bị invalidate khi: (a) admin đổi mode, hoặc (b) admin cập nhật signal_configs

---

### 3.2. Chế độ AI

```
┌─────────────────────────────────────────────────────────────────┐
│                       AI MODE FLOW                              │
└─────────────────────────────────────────────────────────────────┘

 [1] Scheduler chạy (mỗi 5s — tức mỗi "cuối chu kỳ đèn")
      │
 [2] Query: intersectionRepository.findAllWithLanes()
      │
 [3] Kiểm tra: intersection.getOperatingMode() == AI?
      │
      ├── YES ────────────────────────────────────────────┐
      │                                                    │
 [4] Với mỗi lane thuộc intersection:                      │
     Query bảng traffic_log:                               │
     SELECT * FROM traffic_logs                            │
     WHERE lane_id = ?                                     │
     ORDER BY recorded_at DESC LIMIT 1                     │
      │                                                    │
 [5] Lấy congestion_level (mật độ kẹt xe mới nhất)        │
      │                                                    │
 [6] Thuật toán AI tính toán:                              │
     - Tìm cặp lane (lane + opposing_lane) có tổng         │
       congestion cao nhất                                  │
     - Cặp thắng → GREEN, các cặp còn lại → RED            │
     - greenDuration = f(congestion):                       │
         congestion ≤ 30% → 20s                             │
         congestion ≤ 60% → 40s                             │
         congestion > 60% → 60s                             │
     - redDuration = TOTAL_CYCLE - greenDuration            │
      │                                                    │
 [7] Đóng gói thành List<SignalMessage>                    │
      │                                                    │
 [8] INSERT vào bảng signal_history (ghi sổ lịch sử)      │
      │                                                    │
 [9] Gửi qua WebSocket: /topic/signal                     │
      │                                                    │
      └─── Kết thúc chu kỳ, chờ scheduler tick tiếp ──────┘
```

**Đặc điểm chính:**
- Mỗi chu kỳ đều query `traffic_log` mới nhất
- Thuật toán nhả ra green/red duration **khác nhau** mỗi chu kỳ
- **Luôn ghi `signal_history`** để có lịch sử audit

---

## 4. Thiết kế kỹ thuật — Các thay đổi cần thực hiện

### 4.1. Tầng Repository

| File | Hành động | Chi tiết |
|------|-----------|----------|
| `SignalConfigRepository.java` | **TẠO MỚI** | `findByIntersectionIdAndLaneId(Long, Long)`, `findByIntersectionId(Long)` |

### 4.2. Tầng Service

| File | Hành động | Chi tiết |
|------|-----------|----------|
| `ManualSignalService.java` (interface) | **TẠO MỚI** | Định nghĩa method `getFixedSignals(Intersection)` |
| `ManualSignalServiceImpl.java` | **TẠO MỚI** | Đọc `signal_configs`, build `SignalMessage`, có cơ chế cache |
| `TrafficAlgorithmServiceImpl.java` | **GIỮ NGUYÊN** | Logic AI hiện tại đã đúng |

### 4.3. Tầng Scheduler

| File | Hành động | Chi tiết |
|------|-----------|----------|
| `TrafficSignalScheduler.java` | **SỬA** | Bỏ logic skip khi mode không phải AI → Thêm nhánh xử lý MANUAL |

**Logic mới của Scheduler:**

```java
for (Intersection intersection : intersections) {
    OperatingMode mode = intersection.getOperatingMode();

    if (mode == OperatingMode.AI_AUTO) {
        // === NHÁNH AI ===
        // 1. Gọi TrafficAlgorithmService.calculateAdaptiveSignal()
        // 2. Lưu signal_history
        // 3. Gửi WebSocket
    } 
    else if (mode == OperatingMode.MANUAL_OVERRIDE || mode == OperatingMode.FIXED_TIME) {
        // === NHÁNH MANUAL ===
        // 1. Gọi ManualSignalService.getFixedSignals()
        //    (đọc signal_configs, có cache)
        // 2. KHÔNG lưu signal_history
        // 3. Gửi WebSocket (để frontend vẫn nhận được tín hiệu)
    }
}
```

### 4.4. Tầng Guard

| File | Hành động | Chi tiết |
|------|-----------|----------|
| `OperatingModeGuard.java` | **SỬA** hoặc **BỎ** | Không còn cần guard để skip — thay bằng switch/if trong scheduler |

### 4.5. Cơ chế Cache cho MANUAL mode

```
ConcurrentHashMap<Long, List<SignalMessage>> manualSignalCache
    Key   = intersectionId
    Value = List<SignalMessage> (đã build sẵn từ signal_configs)
```

**Cache invalidation xảy ra khi:**
1. Admin gọi API `PUT /api/intersections/{id}/operating-mode` → clear cache cho intersection đó
2. Admin cập nhật `signal_configs` → clear cache cho intersection tương ứng
3. Application restart → cache tự rỗng

---

## 5. Sơ đồ quan hệ giữa các bảng

```
intersections
    │
    ├── 1:N ──── lanes
    │               │
    │               ├── 1:N ──── traffic_logs      (AI đọc mật độ từ đây)
    │               │
    │               ├── 1:N ──── signal_configs    (MANUAL đọc thời lượng từ đây)
    │               │
    │               └── 1:N ──── signal_history    (AI ghi lịch sử vào đây)
    │
    ├── 1:N ──── signal_configs  (cũng FK tới intersection)
    │
    └── 1:N ──── signal_history  (cũng FK tới intersection)
```

---

## 6. API cần bổ sung

### 6.1. CRUD cho `signal_configs`

| Method | Endpoint | Mô tả |
|--------|----------|--------|
| `GET` | `/api/signal-configs?intersectionId={id}` | Lấy cấu hình đèn theo ngã tư |
| `POST` | `/api/signal-configs` | Tạo cấu hình đèn mới cho 1 lane |
| `PUT` | `/api/signal-configs/{id}` | Cập nhật thời lượng đèn |
| `DELETE` | `/api/signal-configs/{id}` | Xoá cấu hình |

**Request body mẫu (POST/PUT):**
```json
{
  "intersectionId": 1,
  "laneId": 2,
  "greenDuration": 45,
  "yellowDuration": 5,
  "redDuration": 40
}
```

### 6.2. Cập nhật API chuyển mode

API hiện có `PUT /api/intersections/{id}/operating-mode` — cần bổ sung logic:
- Khi chuyển sang MANUAL → Validate rằng `signal_configs` đã tồn tại cho tất cả lanes của intersection đó
- Khi chuyển sang AI → Clear cache MANUAL (nếu có)

---

## 7. Checklist thực hiện (theo thứ tự)

### Phase 1: Tầng Data
- [ ] Tạo `SignalConfigRepository.java`

### Phase 2: Tầng Service
- [ ] Tạo interface `ManualSignalService.java`
- [ ] Tạo `ManualSignalServiceImpl.java` (đọc signal_configs + cache)
- [ ] Cập nhật `OperatingModeGuard.java` (hoặc bỏ đi, tuỳ thiết kế)

### Phase 3: Tầng Scheduler
- [ ] Sửa `TrafficSignalScheduler.java` — thêm nhánh MANUAL

### Phase 4: Tầng Controller + DTO
- [ ] Tạo `SignalConfigController.java`
- [ ] Tạo `SignalConfigRequest.java` (DTO)
- [ ] Tạo `SignalConfigResponse.java` (DTO)
- [ ] Cập nhật `UpdateOperatingModeRequest` nếu cần validate

### Phase 5: Testing & Verification
- [ ] Test mode MANUAL: chuyển mode → scheduler gửi WebSocket với giá trị cố định
- [ ] Test mode AI: chuyển mode → scheduler đọc traffic_log → tính toán → ghi signal_history
- [ ] Test chuyển đổi mode: MANUAL ↔ AI không bị lỗi, cache invalidate đúng
- [ ] Test edge case: không có signal_configs khi bật MANUAL → trả lỗi rõ ràng

---

## 8. Câu hỏi cần xác nhận trước khi code

> **Q1**: Enum `OperatingMode` hiện có 3 giá trị (`AI_AUTO`, `FIXED_TIME`, `MANUAL_OVERRIDE`). Có nên gộp `FIXED_TIME` và `MANUAL_OVERRIDE` lại thành 1 mode `MANUAL` không? Hay giữ nguyên 3 mode và cả `FIXED_TIME` lẫn `MANUAL_OVERRIDE` đều đọc từ `signal_configs`?

> **Q2**: Ở mode MANUAL, scheduler có cần gửi WebSocket liên tục (mỗi 5s) không? Hay chỉ gửi 1 lần khi bật mode, rồi frontend tự lặp lại theo thời lượng nhận được?

> **Q3**: Có cần thêm `yellowDuration` vào `SignalMessage` gửi qua WebSocket không? Hiện tại `SignalMessage` chỉ có `greenDuration` và `redDuration`.

> **Q4**: Khi chuyển từ MANUAL sang AI, có cần chờ hết chu kỳ đèn hiện tại hay chuyển ngay lập tức?

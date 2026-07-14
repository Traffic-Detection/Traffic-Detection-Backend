# Route Recommendation Backend — Context Log

## Date: 2026-07-08

## Overview
Tích hợp OpenStreetMap + OSRM Route Recommendation vào hệ thống Traffic Detection Backend.

---

## Existing Files (Already Present Before This Sprint)

### Entities
- `RoadSegment.java` — Entity đại diện road segment kết nối 2 intersection
- `RouteHistory.java` — Entity lưu lịch sử route recommendation
- `Intersection.java` — Entity giao lộ (không thay đổi)
- `CameraDevice.java` — Entity camera (không thay đổi)
- `TrafficLog.java` — Entity traffic log (không thay đổi)
- `Lane.java` — Entity làn xe (không thay đổi)

### Enums
- `TrafficLevel.java` — LOW, MEDIUM, HIGH
- `RoadSegmentStatus.java` — ACTIVE, INACTIVE

### Services
- `RouteService.java` (Interface) + `RouteServiceImpl.java` (470 lines)
  - OSRM integration with alternatives=3
  - Scoring: IntersectionPenalty + RoadSegmentPenalty + Distance + Duration
  - Intersection penalty: HIGH=1000, MEDIUM=100, LOW=10
  - Road segment penalty: HIGH=500, MEDIUM=50, LOW=5
  - Haversine distance matching (200m radius)

- `RoadSegmentService.java` (Interface) + `RoadSegmentServiceImpl.java`
  - CRUD for road segments
  - Traffic level updates

- `TrafficSimulationService.java` (Interface) + `TrafficSimulationServiceImpl.java`
  - Random vehicle count generation every 5 seconds
  - VehicleCount > 60 → HIGH, > 30 → MEDIUM, ≤ 30 → LOW
  - Updates intersection (in-memory) and road segment (database) traffic

### Repositories
- `RoadSegmentRepository.java` — findByStatus, findByOsmWayId, findByTrafficLevel
- `RouteHistoryRepository.java` — findTop20ByOrderByCreatedAtDesc
- `IntersectionRepository.java` — findAll, findByOperatingModeWithLanes

### DTOs
- Request: `RouteRecommendRequest.java` (startLat, startLng, endLat, endLng)
- Response: `RouteRecommendResponse.java`, `RouteCandidate.java`, `RouteHistoryResponse.java`
- Response: `RoadSegmentResponse.java`, `CurrentTrafficResponse.java`
- Response: `IntersectionTrafficResponse.java`, `SimulationStatusResponse.java`

### Exceptions
- `OsrmServiceException.java` — OSRM unreachable or error
- `RouteNotFoundException.java` — No routes found
- `RoadSegmentNotFoundException.java` — Road segment not found

### Liquibase v4.0
- `018-create-road-segments-table.yaml` — road_segments table
- `019-create-route-histories-table.yaml` — route_histories table
- `020-seed-road-segments.yaml` — 10 mock road segments

### Configuration
- `RestTemplateConfig.java` — RestTemplate bean for OSRM calls

---

## Changes Made in This Sprint

### Bug Fix
- `RouteServiceImpl.java` — Fixed SLF4J log format: `{:.0f}` → `{}`

### Configuration Changes
- `db.changelog-master.yaml` — Registered v4.0 changelogs (018, 019, 020)
- `application.yaml` — Added `osrm.base-url` property with default `http://localhost:5000`

### New Controllers
- `RouteController.java` — POST /api/routes/recommend, GET /api/routes/history
- `SimulationController.java` — POST /api/simulation/start, POST /api/simulation/stop, GET /api/traffic/current
- `RoadSegmentController.java` — GET /api/roads, GET /api/roads/intersection/{id}

### Security Updates
- `SecurityConfig.java` — Whitelisted:
  - POST /api/routes/** → authenticated
  - POST /api/simulation/** → ROLE_ADMIN, ROLE_OPERATOR

### Exception Handler Updates
- `GlobalExceptionHandler.java` — Added:
  - RouteNotFoundException, RoadSegmentNotFoundException → 404
  - OsrmServiceException → 502 Bad Gateway

---

## REST API Summary

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | /api/routes/recommend | Authenticated | Route recommendation |
| GET | /api/routes/history | Authenticated | Route history (top 20) |
| POST | /api/simulation/start | ADMIN/OPERATOR | Start simulation |
| POST | /api/simulation/stop | ADMIN/OPERATOR | Stop simulation |
| GET | /api/traffic/current | Authenticated | Current traffic status |
| GET | /api/roads | Authenticated | All active road segments |
| GET | /api/roads/intersection/{id} | Authenticated | Road segments by intersection |

---

## Architecture Notes
- Clean Architecture: Controller → Service Interface → Service Implementation → Repository
- No business logic in controllers
- TrafficLevel from simulation (mock) — replaceable with YOLO data later
- OSRM called only from backend, never from frontend

package com.gwuy.sba301.trafficdetectionbackend.service.impls;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gwuy.sba301.trafficdetectionbackend.dto.request.RouteRecommendRequest;
import com.gwuy.sba301.trafficdetectionbackend.dto.response.*;
import com.gwuy.sba301.trafficdetectionbackend.entity.Intersection;
import com.gwuy.sba301.trafficdetectionbackend.entity.Lane;
import com.gwuy.sba301.trafficdetectionbackend.entity.RoadSegment;
import com.gwuy.sba301.trafficdetectionbackend.entity.RouteHistory;
import com.gwuy.sba301.trafficdetectionbackend.entity.TrafficLog;
import com.gwuy.sba301.trafficdetectionbackend.enums.RoadSegmentStatus;
import com.gwuy.sba301.trafficdetectionbackend.enums.TrafficLevel;
import com.gwuy.sba301.trafficdetectionbackend.exception.OsrmServiceException;
import com.gwuy.sba301.trafficdetectionbackend.exception.RouteNotFoundException;
import com.gwuy.sba301.trafficdetectionbackend.repository.IntersectionRepository;
import com.gwuy.sba301.trafficdetectionbackend.repository.LaneRepository;
import com.gwuy.sba301.trafficdetectionbackend.repository.RoadSegmentRepository;
import com.gwuy.sba301.trafficdetectionbackend.repository.RouteHistoryRepository;
import com.gwuy.sba301.trafficdetectionbackend.repository.TrafficLogRepository;
import com.gwuy.sba301.trafficdetectionbackend.service.interfaces.RouteService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class RouteServiceImpl implements RouteService {

    final RestTemplate restTemplate;
    final ObjectMapper objectMapper;
    final IntersectionRepository intersectionRepository;
    final RoadSegmentRepository roadSegmentRepository;
    final RouteHistoryRepository routeHistoryRepository;

    // Đã thay thế Simulator bằng Repository thật của YOLOv8
    final TrafficLogRepository trafficLogRepository;
    final LaneRepository laneRepository;

    @Value("${osrm.base-url:http://localhost:5000}")
    private String osrmBaseUrl;

    private static final double INTERSECTION_MATCH_RADIUS_METERS = 200.0;

    private static final double INTERSECTION_PENALTY_HIGH = 1000.0;
    private static final double INTERSECTION_PENALTY_MEDIUM = 100.0;
    private static final double INTERSECTION_PENALTY_LOW = 10.0;

    private static final double ROAD_SEGMENT_PENALTY_HIGH = 500.0;
    private static final double ROAD_SEGMENT_PENALTY_MEDIUM = 50.0;
    private static final double ROAD_SEGMENT_PENALTY_LOW = 5.0;

    @Override
    @Transactional
    public RouteRecommendResponse recommendRoute(RouteRecommendRequest request) {
        log.info("Route recommendation requested: ({},{}) → ({},{})",
                request.getStartLat(), request.getStartLng(),
                request.getEndLat(), request.getEndLng());

        JsonNode osrmResponse = callOsrm(request);
        JsonNode routesNode = osrmResponse.get("routes");

        if (routesNode == null || !routesNode.isArray() || routesNode.isEmpty()) {
            throw new RouteNotFoundException("No routes found between the given coordinates");
        }

        List<Intersection> allIntersections = intersectionRepository.findAll();
        List<RoadSegment> activeRoadSegments = roadSegmentRepository.findByStatus(RoadSegmentStatus.ACTIVE);

        List<RouteCandidate> candidates = new ArrayList<>();
        for (int i = 0; i < routesNode.size(); i++) {
            JsonNode routeNode = routesNode.get(i);
            RouteCandidate candidate = scoreRoute(i, routeNode, allIntersections, activeRoadSegments);
            candidates.add(candidate);
        }

        int selectedIndex = 0;
        double minScore = Double.MAX_VALUE;
        for (int i = 0; i < candidates.size(); i++) {
            if (candidates.get(i).getScore() < minScore) {
                minScore = candidates.get(i).getScore();
                selectedIndex = i;
            }
        }

        RouteCandidate selectedRoute = candidates.get(selectedIndex);
        String message = buildRecommendationMessage(candidates, selectedIndex);

        saveRouteHistory(request, selectedRoute, selectedIndex, candidates.size());

        return RouteRecommendResponse.builder()
                .selectedRouteIndex(selectedIndex)
                .totalRoutes(candidates.size())
                .message(message)
                .candidates(candidates)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RouteHistoryResponse> getRouteHistory() {
        return routeHistoryRepository.findTop20ByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToHistoryResponse)
                .collect(Collectors.toList());
    }

    private JsonNode callOsrm(RouteRecommendRequest request) {
        String url = String.format("%s/route/v1/driving/%f,%f;%f,%f?alternatives=3&geometries=geojson&overview=full",
                osrmBaseUrl,
                request.getStartLng(), request.getStartLat(),
                request.getEndLng(), request.getEndLat());

        try {
            String responseBody = restTemplate.getForObject(url, String.class);
            JsonNode jsonNode = objectMapper.readTree(responseBody);

            String code = jsonNode.has("code") ? jsonNode.get("code").asText() : "";
            if (!"Ok".equals(code)) {
                throw new OsrmServiceException("OSRM returned error code: " + code);
            }
            return jsonNode;
        } catch (RestClientException e) {
            throw new OsrmServiceException("Cannot connect to OSRM service at " + osrmBaseUrl, e);
        } catch (Exception e) {
            throw new OsrmServiceException("Failed to parse OSRM response", e);
        }
    }

    /**
     * TÍNH TOÁN MỨC ĐỘ KẸT XE THỰC TẾ TỪ YOLOv8
     * Gom tất cả các làn của ngã tư lại, lấy làn kẹt nặng nhất làm chuẩn (%)
     */
    private TrafficLevel getRealTimeTrafficLevel(Long intersectionId) {
        List<Lane> lanes = laneRepository.findByIntersectionId(intersectionId);
        if (lanes.isEmpty()) return TrafficLevel.LOW;

        double maxCongestion = 0.0;
        for (Lane lane : lanes) {
            double congestion = trafficLogRepository.findFirstByLaneIdOrderByRecordedAtDesc(lane.getId())
                    .map(TrafficLog::getCongestionLevel)
                    .orElse(0.0);
            if (congestion > maxCongestion) {
                maxCongestion = congestion;
            }
        }

        // Đánh giá mức độ kẹt xe dựa trên tỷ lệ phần trăm (Congestion %)
        if (maxCongestion > 60.0) return TrafficLevel.HIGH;
        if (maxCongestion > 30.0) return TrafficLevel.MEDIUM;
        return TrafficLevel.LOW;
    }

    private RouteCandidate scoreRoute(int routeIndex, JsonNode routeNode,
                                      List<Intersection> allIntersections,
                                      List<RoadSegment> activeRoadSegments) {
        double distance = routeNode.get("distance").asDouble();
        double duration = routeNode.get("duration").asDouble();
        JsonNode geometry = routeNode.get("geometry");

        List<double[]> routeCoords = extractRouteCoordinates(geometry);
        List<Intersection> matchedIntersections = findIntersectionsOnRoute(routeCoords, allIntersections);
        List<RoadSegment> matchedRoadSegments = findRoadSegmentsOnRoute(matchedIntersections, activeRoadSegments);

        double intersectionPenalty = 0.0;
        for (Intersection intersection : matchedIntersections) {
            // DÙNG DATA YOLO THẬT THAY VÌ GIẢ LẬP
            TrafficLevel level = getRealTimeTrafficLevel(intersection.getId());
            intersectionPenalty += getIntersectionPenalty(level);
        }

        double roadSegmentPenalty = 0.0;
        for (RoadSegment segment : matchedRoadSegments) {
            roadSegmentPenalty += getRoadSegmentPenalty(segment.getTrafficLevel());
        }

        double trafficPenalty = intersectionPenalty + roadSegmentPenalty;
        double score = trafficPenalty + distance + duration;

        String overallTrafficLevel = determineOverallTrafficLevel(matchedIntersections, matchedRoadSegments);

        return RouteCandidate.builder()
                .routeIndex(routeIndex)
                .distance(distance)
                .duration(duration)
                .score(score)
                .trafficPenalty(trafficPenalty)
                .trafficLevel(overallTrafficLevel)
                .geometry(geometry)
                .intersections(matchedIntersections.stream()
                        .map(Intersection::getName)
                        .collect(Collectors.toList()))
                .roadSegments(matchedRoadSegments.stream()
                        .map(RoadSegment::getRoadName)
                        .collect(Collectors.toList()))
                .build();
    }

    private List<double[]> extractRouteCoordinates(JsonNode geometry) {
        List<double[]> coords = new ArrayList<>();
        if (geometry != null && geometry.has("coordinates")) {
            JsonNode coordinates = geometry.get("coordinates");
            for (JsonNode coord : coordinates) {
                if (coord.isArray() && coord.size() >= 2) {
                    coords.add(new double[]{coord.get(0).asDouble(), coord.get(1).asDouble()});
                }
            }
        }
        return coords;
    }

    private List<Intersection> findIntersectionsOnRoute(List<double[]> routeCoords,
                                                        List<Intersection> allIntersections) {
        List<Intersection> matched = new ArrayList<>();
        for (Intersection intersection : allIntersections) {
            double[] intersectionCoord = parseIntersectionCoordinates(intersection);
            if (intersectionCoord == null) continue;

            for (double[] routeCoord : routeCoords) {
                double dist = haversineDistance(
                        intersectionCoord[1], intersectionCoord[0],
                        routeCoord[1], routeCoord[0]
                );
                if (dist <= INTERSECTION_MATCH_RADIUS_METERS) {
                    matched.add(intersection);
                    break;
                }
            }
        }
        return matched;
    }

    private double[] parseIntersectionCoordinates(Intersection intersection) {
        if (intersection.getCoordinates() == null || intersection.getCoordinates().isBlank()) {
            return null;
        }
        try {
            JsonNode coordNode = objectMapper.readTree(intersection.getCoordinates());
            double lat = coordNode.has("lat") ? coordNode.get("lat").asDouble() :
                    coordNode.has("latitude") ? coordNode.get("latitude").asDouble() : 0;
            double lng = coordNode.has("lng") ? coordNode.get("lng").asDouble() :
                    coordNode.has("longitude") ? coordNode.get("longitude").asDouble() : 0;
            if (lat == 0 && lng == 0) return null;
            return new double[]{lng, lat};
        } catch (Exception e) {
            return null;
        }
    }

    private List<RoadSegment> findRoadSegmentsOnRoute(List<Intersection> matchedIntersections,
                                                      List<RoadSegment> activeRoadSegments) {
        Set<Long> matchedIds = matchedIntersections.stream()
                .map(Intersection::getId)
                .collect(Collectors.toSet());

        return activeRoadSegments.stream()
                .filter(seg -> matchedIds.contains(seg.getFromIntersection().getId())
                        || matchedIds.contains(seg.getToIntersection().getId()))
                .collect(Collectors.toList());
    }

    private double getIntersectionPenalty(TrafficLevel level) {
        return switch (level) {
            case HIGH -> INTERSECTION_PENALTY_HIGH;
            case MEDIUM -> INTERSECTION_PENALTY_MEDIUM;
            case LOW -> INTERSECTION_PENALTY_LOW;
        };
    }

    private double getRoadSegmentPenalty(TrafficLevel level) {
        return switch (level) {
            case HIGH -> ROAD_SEGMENT_PENALTY_HIGH;
            case MEDIUM -> ROAD_SEGMENT_PENALTY_MEDIUM;
            case LOW -> ROAD_SEGMENT_PENALTY_LOW;
        };
    }

    /**
     * Xác định mức độ kẹt xe tổng thể của cả 1 tuyến đường.
     * ĐÃ FIX LỖI: Kiểm tra ĐỘC LẬP cả Ngã tư lẫn Đoạn đường. Cứ có 1 điểm chạm ĐỎ là cả tuyến bị ĐỎ.
     */
    private String determineOverallTrafficLevel(List<Intersection> matchedIntersections,
                                                List<RoadSegment> matchedRoadSegments) {

        // 1. Kiểm tra xem có đi xuyên qua NGÃ TƯ nào đang KẸT (HIGH) không?
        for (Intersection intersection : matchedIntersections) {
            TrafficLevel level = getRealTimeTrafficLevel(intersection.getId());
            if (level == TrafficLevel.HIGH) {
                return "HIGH";
            }
        }

        // 2. Kiểm tra xem có đi đè lên ĐOẠN ĐƯỜNG nào đang KẸT (HIGH) không?
        boolean hasHighRoad = matchedRoadSegments.stream()
                .anyMatch(seg -> seg.getTrafficLevel() == TrafficLevel.HIGH);
        if (hasHighRoad) {
            return "HIGH";
        }

        // 3. Tương tự, kiểm tra mức độ TRUNG BÌNH (MEDIUM)
        for (Intersection intersection : matchedIntersections) {
            TrafficLevel level = getRealTimeTrafficLevel(intersection.getId());
            if (level == TrafficLevel.MEDIUM) {
                return "MEDIUM";
            }
        }
        boolean hasMediumRoad = matchedRoadSegments.stream()
                .anyMatch(seg -> seg.getTrafficLevel() == TrafficLevel.MEDIUM);
        if (hasMediumRoad) {
            return "MEDIUM";
        }

        // 4. Nếu qua hết các vòng gửi xe trên mà không kẹt -> Chắc chắn là Thông thoáng
        return "LOW";
    }

    private double haversineDistance(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Xây dựng thông báo gợi ý tuyến đường bằng Tiếng Việt.
     */
    private String buildRecommendationMessage(List<RouteCandidate> candidates, int selectedIndex) {
        boolean allHigh = candidates.stream()
                .allMatch(c -> "HIGH".equals(c.getTrafficLevel()));

        if (allHigh) {
            return "Tất cả các tuyến đường đều đang kẹt xe nghiêm trọng. Đã chọn tuyến đường có điểm phạt thấp nhất.";
        }

        RouteCandidate selected = candidates.get(selectedIndex);
        if ("LOW".equals(selected.getTrafficLevel())) {
            return "Tuyến đường " + (selectedIndex + 1) + " được khuyến nghị — tình trạng giao thông thông thoáng.";
        } else if ("MEDIUM".equals(selected.getTrafficLevel())) {
            return "Tuyến đường " + (selectedIndex + 1) + " được khuyến nghị — giao thông đông đúc nhẹ, là lựa chọn tốt nhất hiện tại.";
        } else {
            return "Tuyến đường " + (selectedIndex + 1) + " được chọn — giao thông kẹt xe nhưng có điểm phạt thấp nhất.";
        }
    }

    private void saveRouteHistory(RouteRecommendRequest request, RouteCandidate selectedRoute,
                                  int selectedIndex, int totalRoutes) {
        RouteHistory history = RouteHistory.builder()
                .startLat(request.getStartLat())
                .startLng(request.getStartLng())
                .endLat(request.getEndLat())
                .endLng(request.getEndLng())
                .selectedRouteIndex(selectedIndex)
                .totalRoutes(totalRoutes)
                .totalScore(selectedRoute.getScore())
                .totalDistance(selectedRoute.getDistance())
                .totalDuration(selectedRoute.getDuration())
                .routeGeometry(selectedRoute.getGeometry() != null
                        ? selectedRoute.getGeometry().toString() : null)
                .build();
        routeHistoryRepository.save(history);
    }

    private RouteHistoryResponse mapToHistoryResponse(RouteHistory entity) {
        return RouteHistoryResponse.builder()
                .id(entity.getId())
                .startLat(entity.getStartLat())
                .startLng(entity.getStartLng())
                .endLat(entity.getEndLat())
                .endLng(entity.getEndLng())
                .selectedRouteIndex(entity.getSelectedRouteIndex())
                .totalRoutes(entity.getTotalRoutes())
                .totalScore(entity.getTotalScore())
                .totalDistance(entity.getTotalDistance())
                .totalDuration(entity.getTotalDuration())
                .routeGeometry(entity.getRouteGeometry())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
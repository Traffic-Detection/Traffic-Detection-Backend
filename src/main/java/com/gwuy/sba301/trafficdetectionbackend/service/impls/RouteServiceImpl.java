package com.gwuy.sba301.trafficdetectionbackend.service.impls;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gwuy.sba301.trafficdetectionbackend.dto.request.RouteRecommendRequest;
import com.gwuy.sba301.trafficdetectionbackend.dto.response.*;
import com.gwuy.sba301.trafficdetectionbackend.entity.Intersection;
import com.gwuy.sba301.trafficdetectionbackend.entity.RoadSegment;
import com.gwuy.sba301.trafficdetectionbackend.entity.RouteHistory;
import com.gwuy.sba301.trafficdetectionbackend.enums.RoadSegmentStatus;
import com.gwuy.sba301.trafficdetectionbackend.enums.TrafficLevel;
import com.gwuy.sba301.trafficdetectionbackend.exception.OsrmServiceException;
import com.gwuy.sba301.trafficdetectionbackend.exception.RouteNotFoundException;
import com.gwuy.sba301.trafficdetectionbackend.repository.IntersectionRepository;
import com.gwuy.sba301.trafficdetectionbackend.repository.RoadSegmentRepository;
import com.gwuy.sba301.trafficdetectionbackend.repository.RouteHistoryRepository;
import com.gwuy.sba301.trafficdetectionbackend.service.interfaces.RouteService;
import com.gwuy.sba301.trafficdetectionbackend.service.interfaces.TrafficSimulationService;
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

/**
 * Implementation of {@link RouteService}.
 *
 * <p>Integrates with OSRM to fetch alternative routes, then applies a
 * traffic-aware scoring algorithm using intersection and road segment
 * traffic levels from the mock database.</p>
 *
 * <h3>Scoring Algorithm</h3>
 * <ol>
 *   <li>Call OSRM with alternatives=3</li>
 *   <li>For each route, identify intersections and road segments it passes through</li>
 *   <li>Apply intersection penalties: HIGH=+1000, MEDIUM=+100, LOW=+10</li>
 *   <li>Apply road segment penalties: HIGH=+500, MEDIUM=+50, LOW=+5</li>
 *   <li>Score = trafficPenalty + distance + duration</li>
 *   <li>Select the route with the lowest score</li>
 * </ol>
 */
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
    final TrafficSimulationService trafficSimulationService;

    @Value("${osrm.base-url:http://localhost:5000}")
    private String osrmBaseUrl;

    /** Radius in meters to consider an intersection "on" a route */
    private static final double INTERSECTION_MATCH_RADIUS_METERS = 200.0;

    // ─── Penalty Constants ───────────────────────────────────────────
    private static final double INTERSECTION_PENALTY_HIGH = 1000.0;
    private static final double INTERSECTION_PENALTY_MEDIUM = 100.0;
    private static final double INTERSECTION_PENALTY_LOW = 10.0;

    private static final double ROAD_SEGMENT_PENALTY_HIGH = 500.0;
    private static final double ROAD_SEGMENT_PENALTY_MEDIUM = 50.0;
    private static final double ROAD_SEGMENT_PENALTY_LOW = 5.0;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public RouteRecommendResponse recommendRoute(RouteRecommendRequest request) {
        log.info("Route recommendation requested: ({},{}) → ({},{})",
                request.getStartLat(), request.getStartLng(),
                request.getEndLat(), request.getEndLng());

        // Step 1: Call OSRM
        JsonNode osrmResponse = callOsrm(request);
        JsonNode routesNode = osrmResponse.get("routes");

        if (routesNode == null || !routesNode.isArray() || routesNode.isEmpty()) {
            throw new RouteNotFoundException("No routes found between the given coordinates");
        }

        // Step 2 & 3: Load intersections and road segments from DB
        List<Intersection> allIntersections = intersectionRepository.findAll();
        List<RoadSegment> activeRoadSegments = roadSegmentRepository.findByStatus(RoadSegmentStatus.ACTIVE);

        log.info("Loaded {} intersections and {} active road segments for scoring",
                allIntersections.size(), activeRoadSegments.size());

        // Step 4-6: Score each route
        List<RouteCandidate> candidates = new ArrayList<>();
        for (int i = 0; i < routesNode.size(); i++) {
            JsonNode routeNode = routesNode.get(i);
            RouteCandidate candidate = scoreRoute(i, routeNode, allIntersections, activeRoadSegments);
            candidates.add(candidate);
            log.info("Route {} — distance={}m, duration={}s, penalty={}, score={}",
                    i, candidate.getDistance(), candidate.getDuration(),
                    candidate.getTrafficPenalty(), candidate.getScore());
        }

        // Step 7: Select route with lowest score
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

        log.info("Selected route {} with score {} — {}", selectedIndex, minScore, message);

        // Step 8: Save to history
        saveRouteHistory(request, selectedRoute, selectedIndex, candidates.size());

        // Step 9: Build response
        return RouteRecommendResponse.builder()
                .selectedRouteIndex(selectedIndex)
                .totalRoutes(candidates.size())
                .message(message)
                .candidates(candidates)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<RouteHistoryResponse> getRouteHistory() {
        log.info("Fetching route recommendation history");
        return routeHistoryRepository.findTop20ByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToHistoryResponse)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════
    //  OSRM Integration
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Calls the OSRM route API with alternatives=3 and GeoJSON geometry.
     *
     * @param request the route request with coordinates
     * @return the OSRM JSON response
     * @throws OsrmServiceException if OSRM is unreachable or returns an error
     */
    private JsonNode callOsrm(RouteRecommendRequest request) {
        // OSRM uses lng,lat order
        String url = String.format("%s/route/v1/driving/%f,%f;%f,%f?alternatives=3&geometries=geojson&overview=full",
                osrmBaseUrl,
                request.getStartLng(), request.getStartLat(),
                request.getEndLng(), request.getEndLat());

        log.debug("Calling OSRM: {}", url);

        try {
            String responseBody = restTemplate.getForObject(url, String.class);
            JsonNode jsonNode = objectMapper.readTree(responseBody);

            String code = jsonNode.has("code") ? jsonNode.get("code").asText() : "";
            if (!"Ok".equals(code)) {
                throw new OsrmServiceException("OSRM returned error code: " + code);
            }

            return jsonNode;
        } catch (RestClientException e) {
            log.error("Failed to connect to OSRM at {}: {}", osrmBaseUrl, e.getMessage());
            throw new OsrmServiceException("Cannot connect to OSRM service at " + osrmBaseUrl, e);
        } catch (OsrmServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error parsing OSRM response: {}", e.getMessage());
            throw new OsrmServiceException("Failed to parse OSRM response", e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Scoring Algorithm
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Scores a single OSRM route by matching it against known intersections
     * and road segments, then applying traffic penalties.
     */
    private RouteCandidate scoreRoute(int routeIndex, JsonNode routeNode,
                                       List<Intersection> allIntersections,
                                       List<RoadSegment> activeRoadSegments) {
        double distance = routeNode.get("distance").asDouble();
        double duration = routeNode.get("duration").asDouble();
        JsonNode geometry = routeNode.get("geometry");

        // Extract route coordinates for proximity matching
        List<double[]> routeCoords = extractRouteCoordinates(geometry);

        // Step 3: Find intersections on this route
        List<Intersection> matchedIntersections = findIntersectionsOnRoute(routeCoords, allIntersections);

        // Step 3: Find road segments on this route
        List<RoadSegment> matchedRoadSegments = findRoadSegmentsOnRoute(matchedIntersections, activeRoadSegments);

        // Step 4: Calculate intersection penalties
        double intersectionPenalty = 0.0;
        for (Intersection intersection : matchedIntersections) {
            TrafficLevel level = trafficSimulationService.getIntersectionTrafficLevel(intersection.getId());
            intersectionPenalty += getIntersectionPenalty(level);
        }

        // Step 5: Calculate road segment penalties
        double roadSegmentPenalty = 0.0;
        for (RoadSegment segment : matchedRoadSegments) {
            roadSegmentPenalty += getRoadSegmentPenalty(segment.getTrafficLevel());
        }

        // Step 6: Calculate total score
        double trafficPenalty = intersectionPenalty + roadSegmentPenalty;
        double score = trafficPenalty + distance + duration;

        // Determine overall traffic level for this route
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

    /**
     * Extracts the coordinate array from a GeoJSON geometry node.
     * Each coordinate is [lng, lat].
     */
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

    /**
     * Finds intersections whose coordinates are within the match radius
     * of any point on the route.
     */
    private List<Intersection> findIntersectionsOnRoute(List<double[]> routeCoords,
                                                         List<Intersection> allIntersections) {
        List<Intersection> matched = new ArrayList<>();

        for (Intersection intersection : allIntersections) {
            double[] intersectionCoord = parseIntersectionCoordinates(intersection);
            if (intersectionCoord == null) continue;

            for (double[] routeCoord : routeCoords) {
                double dist = haversineDistance(
                        intersectionCoord[1], intersectionCoord[0],  // lat, lng
                        routeCoord[1], routeCoord[0]                  // lat, lng (OSRM: lng,lat)
                );
                if (dist <= INTERSECTION_MATCH_RADIUS_METERS) {
                    matched.add(intersection);
                    break;
                }
            }
        }

        log.debug("Matched {} intersections on route", matched.size());
        return matched;
    }

    /**
     * Parses the JSON coordinates string from an Intersection entity.
     * Expected format: {"lat": 10.xxx, "lng": 106.xxx} or similar.
     *
     * @return [lng, lat] array or null if parsing fails
     */
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
            log.warn("Failed to parse coordinates for intersection {}: {}", intersection.getId(), e.getMessage());
            return null;
        }
    }

    /**
     * Finds road segments whose from/to intersections are in the matched set.
     * A road segment is considered on the route if both its from and to
     * intersections are matched.
     */
    private List<RoadSegment> findRoadSegmentsOnRoute(List<Intersection> matchedIntersections,
                                                       List<RoadSegment> activeRoadSegments) {
        Set<Long> matchedIds = matchedIntersections.stream()
                .map(Intersection::getId)
                .collect(Collectors.toSet());

        List<RoadSegment> matched = activeRoadSegments.stream()
                .filter(seg -> matchedIds.contains(seg.getFromIntersection().getId())
                        || matchedIds.contains(seg.getToIntersection().getId()))
                .collect(Collectors.toList());

        log.debug("Matched {} road segments on route", matched.size());
        return matched;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Penalty Calculation
    // ═══════════════════════════════════════════════════════════════════

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
     * Determines the overall traffic level for a route based on its
     * matched intersections and road segments.
     */
    private String determineOverallTrafficLevel(List<Intersection> matchedIntersections,
                                                 List<RoadSegment> matchedRoadSegments) {
        boolean hasHigh = matchedRoadSegments.stream()
                .anyMatch(seg -> seg.getTrafficLevel() == TrafficLevel.HIGH);

        if (hasHigh) {
            // Also check if any intersection is HIGH
            for (Intersection intersection : matchedIntersections) {
                TrafficLevel level = trafficSimulationService.getIntersectionTrafficLevel(intersection.getId());
                if (level == TrafficLevel.HIGH) return "HIGH";
            }
            return "HIGH";
        }

        boolean hasMedium = matchedRoadSegments.stream()
                .anyMatch(seg -> seg.getTrafficLevel() == TrafficLevel.MEDIUM);
        if (hasMedium) return "MEDIUM";

        return "LOW";
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Helpers
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Haversine distance between two points in meters.
     */
    private double haversineDistance(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371000; // Earth radius in meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Builds a human-readable recommendation message.
     */
    private String buildRecommendationMessage(List<RouteCandidate> candidates, int selectedIndex) {
        boolean allHigh = candidates.stream()
                .allMatch(c -> "HIGH".equals(c.getTrafficLevel()));

        if (allHigh) {
            return "All routes have heavy traffic. Selected the route with the lowest overall score.";
        }

        RouteCandidate selected = candidates.get(selectedIndex);
        if ("LOW".equals(selected.getTrafficLevel())) {
            return "Route " + (selectedIndex + 1) + " is recommended — light traffic conditions.";
        } else if ("MEDIUM".equals(selected.getTrafficLevel())) {
            return "Route " + (selectedIndex + 1) + " is recommended — moderate traffic, best available option.";
        } else {
            return "Route " + (selectedIndex + 1) + " selected — heavy traffic but lowest penalty score.";
        }
    }

    /**
     * Persists the route recommendation result to the history table.
     */
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
        log.info("Route history saved with score {}", selectedRoute.getScore());
    }

    /**
     * Maps a RouteHistory entity to its response DTO.
     */
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

//package com.gwuy.sba301.trafficdetectionbackend.service.impls;
//
//import com.gwuy.sba301.trafficdetectionbackend.dto.response.*;
//import com.gwuy.sba301.trafficdetectionbackend.entity.Intersection;
//import com.gwuy.sba301.trafficdetectionbackend.entity.RoadSegment;
//import com.gwuy.sba301.trafficdetectionbackend.enums.RoadSegmentStatus;
//import com.gwuy.sba301.trafficdetectionbackend.enums.TrafficLevel;
//import com.gwuy.sba301.trafficdetectionbackend.repository.IntersectionRepository;
//import com.gwuy.sba301.trafficdetectionbackend.repository.RoadSegmentRepository;
//import com.gwuy.sba301.trafficdetectionbackend.service.interfaces.RoadSegmentService;
//import com.gwuy.sba301.trafficdetectionbackend.service.interfaces.TrafficSimulationService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
//import java.util.*;
//import java.util.concurrent.*;
//import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.stream.Collectors;
//
///**
// * Implementation of {@link TrafficSimulationService}.
// *
// * <p>Simulates traffic by generating random vehicle counts every 5 seconds,
// * deriving traffic levels from the vehicle count, and updating both
// * intersection (in-memory) and road segment (database) traffic data.</p>
// *
// * <h3>Vehicle Count → Traffic Level Mapping</h3>
// * <ul>
// *   <li>vehicleCount &gt; 60 → HIGH</li>
// *   <li>vehicleCount &gt; 30 → MEDIUM</li>
// *   <li>vehicleCount ≤ 30 → LOW</li>
// * </ul>
// */
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class TrafficSimulationServiceImpl implements TrafficSimulationService {
//
//    private final IntersectionRepository intersectionRepository;
//    private final RoadSegmentRepository roadSegmentRepository;
//    private final RoadSegmentService roadSegmentService;
//
//    /** In-memory cache: intersectionId → current traffic level */
//    private final ConcurrentHashMap<Long, TrafficLevel> intersectionTrafficMap = new ConcurrentHashMap<>();
//
//    /** In-memory cache: intersectionId → current vehicle count */
//    private final ConcurrentHashMap<Long, Integer> intersectionVehicleCountMap = new ConcurrentHashMap<>();
//
//    private final AtomicBoolean running = new AtomicBoolean(false);
//    private ScheduledExecutorService scheduler;
//    private final Random random = new Random();
//
//    private static final long SIMULATION_INTERVAL_MS = 5000;
//
//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    public SimulationStatusResponse startSimulation() {
//        if (running.get()) {
//            log.warn("Simulation is already running");
//            return SimulationStatusResponse.builder()
//                    .running(true)
//                    .message("Traffic simulation is already running")
//                    .build();
//        }
//
//        running.set(true);
//        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
//            Thread t = new Thread(r, "traffic-simulation");
//            t.setDaemon(true);
//            return t;
//        });
//
//        scheduler.scheduleAtFixedRate(this::simulateTrafficCycle,
//                0, SIMULATION_INTERVAL_MS, TimeUnit.MILLISECONDS);
//
//        log.info("Traffic simulation STARTED — generating data every {}ms", SIMULATION_INTERVAL_MS);
//
//        return SimulationStatusResponse.builder()
//                .running(true)
//                .message("Traffic simulation started — generating random traffic data every 5 seconds")
//                .build();
//    }
//
//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    public SimulationStatusResponse stopSimulation() {
//        if (!running.get()) {
//            log.warn("Simulation is not running");
//            return SimulationStatusResponse.builder()
//                    .running(false)
//                    .message("Traffic simulation is not running")
//                    .build();
//        }
//
//        running.set(false);
//        if (scheduler != null && !scheduler.isShutdown()) {
//            scheduler.shutdown();
//            try {
//                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
//                    scheduler.shutdownNow();
//                }
//            } catch (InterruptedException e) {
//                scheduler.shutdownNow();
//                Thread.currentThread().interrupt();
//            }
//        }
//
//        log.info("Traffic simulation STOPPED");
//
//        return SimulationStatusResponse.builder()
//                .running(false)
//                .message("Traffic simulation stopped")
//                .build();
//    }
//
//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    public CurrentTrafficResponse getCurrentTraffic() {
//        // Build intersection traffic responses
//        List<Intersection> allIntersections = intersectionRepository.findAll();
//        List<IntersectionTrafficResponse> intersectionResponses = allIntersections.stream()
//                .map(this::mapToIntersectionTraffic)
//                .collect(Collectors.toList());
//
//        // Build road segment responses
//        List<RoadSegmentResponse> roadSegmentResponses = roadSegmentService.getAllRoadSegments();
//
//        return CurrentTrafficResponse.builder()
//                .intersections(intersectionResponses)
//                .roadSegments(roadSegmentResponses)
//                .simulationRunning(running.get())
//                .build();
//    }
//
//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    public boolean isRunning() {
//        return running.get();
//    }
//
//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    public TrafficLevel getIntersectionTrafficLevel(Long intersectionId) {
//        return intersectionTrafficMap.getOrDefault(intersectionId, TrafficLevel.LOW);
//    }
//
//    // ═══════════════════════════════════════════════════════════════════
//    //  Simulation Logic
//    // ═══════════════════════════════════════════════════════════════════
//
//    /**
//     * Executes one cycle of the traffic simulation:
//     * <ol>
//     *   <li>Generate random vehicle count for each intersection</li>
//     *   <li>Derive traffic level from vehicle count</li>
//     *   <li>Update in-memory intersection traffic map</li>
//     *   <li>Update road segment traffic levels in database</li>
//     * </ol>
//     */
//    private void simulateTrafficCycle() {
//        try {
//            log.debug("Running traffic simulation cycle...");
//
//            // Update intersections (in-memory — we don't modify the intersections table)
//            List<Intersection> intersections = intersectionRepository.findAll();
//            for (Intersection intersection : intersections) {
//                int vehicleCount = random.nextInt(101); // 0-100
//                TrafficLevel level = deriveTrafficLevel(vehicleCount);
//
//                intersectionTrafficMap.put(intersection.getId(), level);
//                intersectionVehicleCountMap.put(intersection.getId(), vehicleCount);
//
//                log.debug("Intersection {} [{}]: vehicleCount={}, level={}",
//                        intersection.getId(), intersection.getName(), vehicleCount, level);
//            }
//
//            // Update road segments (database)
//            List<RoadSegment> roadSegments = roadSegmentRepository.findByStatus(RoadSegmentStatus.ACTIVE);
//            for (RoadSegment segment : roadSegments) {
//                // Derive road segment traffic from the traffic levels of its connected intersections
//                TrafficLevel fromLevel = intersectionTrafficMap
//                        .getOrDefault(segment.getFromIntersection().getId(), TrafficLevel.LOW);
//                TrafficLevel toLevel = intersectionTrafficMap
//                        .getOrDefault(segment.getToIntersection().getId(), TrafficLevel.LOW);
//
//                // Use the worse traffic level between the two connected intersections
//                TrafficLevel segmentLevel = getWorseTrafficLevel(fromLevel, toLevel);
//                roadSegmentService.updateTrafficLevel(segment.getId(), segmentLevel);
//            }
//
//            log.info("Simulation cycle complete — updated {} intersections and {} road segments",
//                    intersections.size(), roadSegments.size());
//
//        } catch (Exception e) {
//            log.error("Error during traffic simulation cycle: {}", e.getMessage(), e);
//        }
//    }
//
//    /**
//     * Derives a traffic level from the simulated vehicle count.
//     *
//     * @param vehicleCount random count 0-100
//     * @return the corresponding traffic level
//     */
//    private TrafficLevel deriveTrafficLevel(int vehicleCount) {
//        if (vehicleCount > 60) return TrafficLevel.HIGH;
//        if (vehicleCount > 30) return TrafficLevel.MEDIUM;
//        return TrafficLevel.LOW;
//    }
//
//    /**
//     * Returns the worse (higher congestion) of two traffic levels.
//     */
//    private TrafficLevel getWorseTrafficLevel(TrafficLevel a, TrafficLevel b) {
//        if (a == TrafficLevel.HIGH || b == TrafficLevel.HIGH) return TrafficLevel.HIGH;
//        if (a == TrafficLevel.MEDIUM || b == TrafficLevel.MEDIUM) return TrafficLevel.MEDIUM;
//        return TrafficLevel.LOW;
//    }
//
//    /**
//     * Maps an Intersection entity to an IntersectionTrafficResponse DTO
//     * using the current in-memory simulation data.
//     */
//    private IntersectionTrafficResponse mapToIntersectionTraffic(Intersection intersection) {
//        TrafficLevel level = intersectionTrafficMap.getOrDefault(intersection.getId(), TrafficLevel.LOW);
//        Integer vehicleCount = intersectionVehicleCountMap.getOrDefault(intersection.getId(), 0);
//
//        return IntersectionTrafficResponse.builder()
//                .id(intersection.getId())
//                .name(intersection.getName())
//                .address(intersection.getAddress())
//                .coordinates(intersection.getCoordinates())
//                .trafficLevel(level.name())
//                .vehicleCount(vehicleCount)
//                .build();
//    }
//}

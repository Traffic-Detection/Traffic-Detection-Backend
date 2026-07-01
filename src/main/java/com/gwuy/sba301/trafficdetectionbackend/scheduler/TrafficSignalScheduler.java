package com.gwuy.sba301.trafficdetectionbackend.scheduler;

import com.gwuy.sba301.trafficdetectionbackend.dto.response.SignalMessage;
import com.gwuy.sba301.trafficdetectionbackend.entity.Intersection;
import com.gwuy.sba301.trafficdetectionbackend.entity.SignalHistory;
import com.gwuy.sba301.trafficdetectionbackend.enums.OperatingMode;
import com.gwuy.sba301.trafficdetectionbackend.repository.IntersectionRepository;
import com.gwuy.sba301.trafficdetectionbackend.service.impls.ModeSwitchManager;
import com.gwuy.sba301.trafficdetectionbackend.service.interfaces.ManualSignalService;
import com.gwuy.sba301.trafficdetectionbackend.service.interfaces.SignalHistoryService;
import com.gwuy.sba301.trafficdetectionbackend.service.impls.WebSocketServiceImpl;
import com.gwuy.sba301.trafficdetectionbackend.service.interfaces.TrafficAlgorithmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrafficSignalScheduler {

    private final IntersectionRepository intersectionRepository;
    private final TrafficAlgorithmService trafficAlgorithmServiceImpl;
    private final SignalHistoryService signalHistoryService;
    private final WebSocketServiceImpl webSocketServiceImpl;
    private final ManualSignalService manualSignalService;
    private final ModeSwitchManager modeSwitchManager;

    private final Map<Long, Instant> aiCooldownMap = new ConcurrentHashMap<>();
    private final Map<Long, Instant> cycleStartMap = new ConcurrentHashMap<>();
    private final Map<Long, List<SignalMessage>> currentCycleMessagesMap = new ConcurrentHashMap<>();

    @Scheduled(fixedRate = 5000)
    @Transactional
    public void runTrafficEngine() {
        long startTime = System.currentTimeMillis();
        int processedAi = 0;
        int processedManual = 0;
        int waitingCycle = 0;

        try {
            List<Intersection> intersections = intersectionRepository.findAllWithLanes();
            if (intersections.isEmpty()) return;

            Instant now = Instant.now();

            for (Intersection intersection : intersections) {
                OperatingMode mode = intersection.getOperatingMode();

                if (mode == OperatingMode.AI) {
                    if (!modeSwitchManager.isAiReady(intersection.getId())) {
                        waitingCycle++;
                        continue;
                    }

                    Instant nextAllowedRun = aiCooldownMap.getOrDefault(intersection.getId(), Instant.MIN);

                    // ==========================================
                    // HEARTBEAT 5S: TỰ NHẨM THỜI GIAN ĐỂ GỬI FRONTEND
                    // ==========================================
                    if (now.isBefore(nextAllowedRun)) {
                        List<SignalMessage> cachedMessages = currentCycleMessagesMap.get(intersection.getId());
                        Instant cycleStart = cycleStartMap.get(intersection.getId());

                        if (cachedMessages != null && cycleStart != null) {
                            long elapsedSec = Duration.between(cycleStart, now).getSeconds();
                            List<SignalMessage> syncMessages = new ArrayList<>();

                            for (SignalMessage msg : cachedMessages) {
                                // [ĐÃ FIX 1]: Lấy chính Remaining làm thời gian của Pha 1
                                long phase1Duration = msg.getRemaining();
                                long totalDuration = msg.getGreenDuration() + msg.getRedDuration();

                                String currentSignal;
                                long currentRemaining;

                                if (elapsedSec < phase1Duration) {
                                    currentSignal = msg.getSignal(); // Vẫn đang ở Pha 1
                                    currentRemaining = phase1Duration - elapsedSec;
                                } else if (elapsedSec < totalDuration) {
                                    currentSignal = msg.getSignal().equals("GREEN") ? "RED" : "GREEN"; // Đã sang Pha 2
                                    currentRemaining = totalDuration - elapsedSec;
                                } else {
                                    currentSignal = msg.getSignal().equals("GREEN") ? "RED" : "GREEN";
                                    currentRemaining = 0;
                                }

                                syncMessages.add(SignalMessage.builder()
                                        .intersectionId(msg.getIntersectionId())
                                        .laneId(msg.getLaneId())
                                        .direction(msg.getDirection())
                                        .signal(currentSignal)
                                        .greenDuration(msg.getGreenDuration())
                                        .yellowDuration(msg.getYellowDuration())
                                        .redDuration(msg.getRedDuration())
                                        .remaining((int) currentRemaining)
                                        .trafficLevel(msg.getTrafficLevel())
                                        .build());
                            }
                            webSocketServiceImpl.sendSignalUpdates(syncMessages);
                        }
                        continue; // Đang trong chu kỳ, đi ngủ tiếp, không gọi thuật toán AI!
                    }

                    // ==========================================
                    // GỌI THUẬT TOÁN AI (Chỉ gọi khi hết chu kỳ)
                    // ==========================================
                    Map<String, Object> result = trafficAlgorithmServiceImpl.calculateAdaptiveSignal(intersection);
                    if (result.isEmpty()) continue;

                    @SuppressWarnings("unchecked")
                    List<SignalHistory> histories = (List<SignalHistory>) result.get("histories");
                    @SuppressWarnings("unchecked")
                    List<SignalMessage> messages = (List<SignalMessage>) result.get("messages");

                    signalHistoryService.saveAll(histories);
                    webSocketServiceImpl.sendSignalUpdates(messages);

                    // TÍNH TỔNG CHU KỲ (Thường là 80s)
                    long totalCycle = messages.get(0).getGreenDuration() + messages.get(0).getRedDuration();

                    // [ĐÃ FIX 2]: AI ngủ đông 100% bằng ĐÚNG TỔNG CHU KỲ. Không trừ đi 5s nữa để Đèn Vàng diễn ra trọn vẹn!
                    aiCooldownMap.put(intersection.getId(), now.plusSeconds(totalCycle));
                    cycleStartMap.put(intersection.getId(), now);
                    currentCycleMessagesMap.put(intersection.getId(), messages);

                    log.info("[Scheduler] Đã kích hoạt chu kỳ mới cho Giao lộ: {}. AI sẽ ngủ đông {}s.", intersection.getName(), totalCycle);
                    processedAi++;

                } else if (mode == OperatingMode.MANUAL) {
                    List<SignalMessage> manualMessages = manualSignalService.getFixedSignals(intersection);
                    if (!manualMessages.isEmpty()) {
                        webSocketServiceImpl.sendSignalUpdates(manualMessages);
                        processedManual++;
                    }
                }
            }

        } catch (Exception e) {
            log.error("[Scheduler] Error in TrafficSignalScheduler: ", e);
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("[Scheduler] Finished - AI: {}, MANUAL: {}, Waiting: {}, duration: {}ms",
                processedAi, processedManual, waitingCycle, duration);
    }
}
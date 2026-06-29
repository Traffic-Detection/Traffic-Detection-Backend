package com.gwuy.sba301.trafficdetectionbackend.service.impls;

import com.gwuy.sba301.trafficdetectionbackend.dto.response.SignalMessage;
import com.gwuy.sba301.trafficdetectionbackend.entity.Intersection;
import com.gwuy.sba301.trafficdetectionbackend.entity.SignalConfig;
import com.gwuy.sba301.trafficdetectionbackend.repository.SignalConfigRepository;
import com.gwuy.sba301.trafficdetectionbackend.service.interfaces.ManualSignalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManualSignalServiceImpl implements ManualSignalService {

    private final SignalConfigRepository signalConfigRepository;

    // Bộ đệm lưu trữ cấu hình đèn cố định
    private final ConcurrentHashMap<Long, List<SignalMessage>> manualSignalCache = new ConcurrentHashMap<>();

    // Đánh dấu các ngã tư đã được phát sóng qua WebSocket để chống Spam
    private final Set<Long> broadcastedIntersections = ConcurrentHashMap.newKeySet();

    @Override
    public List<SignalMessage> getFixedSignals(Intersection intersection) {
        Long intersectionId = intersection.getId();

        // 1. Đọc và lưu vào bộ đệm (chỉ query DB nếu cache trống)
        List<SignalMessage> cachedMessages = manualSignalCache.computeIfAbsent(intersectionId, id -> {
            log.info("[MANUAL] Cache miss. Đang đọc cấu hình từ bảng signal_configs cho Nút giao: {}", id);
            List<SignalConfig> configs = signalConfigRepository.findByIntersectionId(id);

            if (configs.isEmpty()) {
                log.warn("[MANUAL] Nút giao {} chưa có dữ liệu trong bảng signal_configs!", id);
                return Collections.emptyList();
            }

            return configs.stream().map(config -> SignalMessage.builder()
                    .intersectionId(config.getIntersection().getId())
                    .laneId(config.getLane().getId())
                    .direction(config.getLane().getDirectionName())
                    .signal("FIXED")
                    .greenDuration(config.getGreenDuration())
                    .yellowDuration(config.getYellowDuration()) // Ánh xạ dữ liệu đèn vàng
                    .redDuration(config.getRedDuration())
                    .remaining(config.getGreenDuration())
                    .trafficLevel("MANUAL_MODE")
                    .build()
            ).collect(Collectors.toList());
        });

        // 2. Chống Spam: Chỉ trả về dữ liệu nếu ngã tư này chưa được broadcast lần nào
        if (!cachedMessages.isEmpty() && !broadcastedIntersections.contains(intersectionId)) {
            broadcastedIntersections.add(intersectionId);
            log.info("[MANUAL] Cấp phép phát sóng tín hiệu cho Nút giao: {}", intersectionId);
            return cachedMessages; // Trả về để Scheduler gửi qua WebSocket
        }

        // Đã gửi rồi thì trả về danh sách rỗng để Scheduler im lặng
        return Collections.emptyList();
    }

    @Override
    public void invalidateCache(Long intersectionId) {
        manualSignalCache.remove(intersectionId);
        broadcastedIntersections.remove(intersectionId); // Xoá cờ đánh dấu để được broadcast lại
        log.info("[MANUAL] Đã xoá cache và cờ phát sóng cho Nút giao: {}", intersectionId);
    }

    @Override
    public void invalidateAllCache() {
        manualSignalCache.clear();
        broadcastedIntersections.clear();
        log.info("[MANUAL] Đã làm sạch toàn bộ bộ đệm tín hiệu MANUAL.");
    }
}
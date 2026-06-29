package com.gwuy.sba301.trafficdetectionbackend.service.impls;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý trạng thái chuyển đổi mode giữa MANUAL và AI.
 * Khi chuyển từ MANUAL → AI, hệ thống phải chờ hết chu kỳ đèn hiện tại
 * trước khi bắt đầu xử lý thuật toán AI.
 *
 * Key = intersectionId
 * Value = thời điểm (epoch millis) mà AI được phép bắt đầu xử lý
 */
@Slf4j
@Component
public class ModeSwitchManager {

    // intersectionId → epoch millis khi AI được phép bắt đầu
    private final ConcurrentHashMap<Long, Long> pendingAiActivation = new ConcurrentHashMap<>();

    /**
     * Đặt lịch cho AI bắt đầu xử lý sau khi chu kỳ đèn MANUAL hiện tại kết thúc.
     *
     * @param intersectionId ID của ngã tư
     * @param cycleDurationMs tổng thời gian 1 chu kỳ MANUAL (green + yellow + red) tính bằng milliseconds
     */
    public void scheduleAiActivation(Long intersectionId, long cycleDurationMs) {
        long activationTime = System.currentTimeMillis() + cycleDurationMs;
        pendingAiActivation.put(intersectionId, activationTime);
        log.info("[ModeSwitchManager] Intersection {} sẽ chuyển sang AI sau {}ms (tại epoch={})",
                intersectionId, cycleDurationMs, activationTime);
    }

    /**
     * Kiểm tra xem ngã tư có đang trong giai đoạn chờ chuyển mode không.
     *
     * @param intersectionId ID của ngã tư
     * @return true nếu AI đã sẵn sàng xử lý (đã hết thời gian chờ hoặc không có pending)
     */
    public boolean isAiReady(Long intersectionId) {
        Long activationTime = pendingAiActivation.get(intersectionId);
        if (activationTime == null) {
            return true; // Không có pending → AI sẵn sàng
        }

        if (System.currentTimeMillis() >= activationTime) {
            pendingAiActivation.remove(intersectionId);
            log.info("[ModeSwitchManager] Intersection {} đã hết thời gian chờ. AI bắt đầu xử lý.", intersectionId);
            return true;
        }

        log.debug("[ModeSwitchManager] Intersection {} đang chờ hết chu kỳ MANUAL. Còn {}ms",
                intersectionId, activationTime - System.currentTimeMillis());
        return false;
    }

    /**
     * Xoá trạng thái pending (khi chuyển sang MANUAL hoặc restart).
     */
    public void clearPending(Long intersectionId) {
        pendingAiActivation.remove(intersectionId);
    }
}

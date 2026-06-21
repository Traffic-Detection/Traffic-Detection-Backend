package com.gwuy.sba301.trafficdetectionbackend.service;

import com.gwuy.sba301.trafficdetectionbackend.dto.response.SignalHistoryResponse;
import com.gwuy.sba301.trafficdetectionbackend.entity.SignalHistory;
import com.gwuy.sba301.trafficdetectionbackend.repository.SignalHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignalHistoryService {

    private final SignalHistoryRepository signalHistoryRepository;

    @Transactional
    public void saveAll(List<SignalHistory> histories) {
        signalHistoryRepository.saveAll(histories);
        log.info("[DB] {} SignalHistory saved", histories.size());
    }

    @Transactional(readOnly = true)
    public List<SignalHistoryResponse> getAllSignalHistory() {
        return signalHistoryRepository.findAll().stream()
                .map(history -> SignalHistoryResponse.builder()
                        .id(history.getId())
                        .intersectionId(history.getIntersection().getId())
                        .laneId(history.getLane().getId())
                        .greenDuration(history.getGreenDuration())
                        .redDuration(history.getRedDuration())
                        .appliedAt(history.getAppliedAt())
                        .build())
                .collect(Collectors.toList());
    }
}

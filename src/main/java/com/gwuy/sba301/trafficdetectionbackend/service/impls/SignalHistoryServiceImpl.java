package com.gwuy.sba301.trafficdetectionbackend.service.impls;

import com.gwuy.sba301.trafficdetectionbackend.dto.response.SignalHistoryResponse;
import com.gwuy.sba301.trafficdetectionbackend.entity.SignalHistory;
import com.gwuy.sba301.trafficdetectionbackend.repository.SignalHistoryRepository;
import com.gwuy.sba301.trafficdetectionbackend.service.interfaces.SignalHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignalHistoryServiceImpl implements SignalHistoryService {

    private final SignalHistoryRepository signalHistoryRepository;

    @Transactional
    @Override
    public void saveAll(List<SignalHistory> histories) {
        signalHistoryRepository.saveAll(histories);
        log.info("[DB] {} SignalHistory saved", histories.size());
    }
    @Override
    @Transactional(readOnly = true)
    public List<SignalHistoryResponse> getAllSignalHistory() {
        return signalHistoryRepository.findAll().stream()
                .map(history -> SignalHistoryResponse.builder()
                        .id(history.getId())
                        .intersectionId(history.getIntersection().getId())
                        .laneId(history.getLane().getId())
                        .greenDuration(history.getGreenDuration())
                        .yellowDuration(history.getYellowDuration())
                        .redDuration(history.getRedDuration())
                        .appliedAt(history.getAppliedAt())
                        .build())
                .collect(Collectors.toList());
    }
}

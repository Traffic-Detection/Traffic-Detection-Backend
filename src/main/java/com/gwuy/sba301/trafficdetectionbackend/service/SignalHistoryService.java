package com.gwuy.sba301.trafficdetectionbackend.service;

import com.gwuy.sba301.trafficdetectionbackend.entity.SignalHistory;
import com.gwuy.sba301.trafficdetectionbackend.repository.SignalHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
}

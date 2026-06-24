package com.gwuy.sba301.trafficdetectionbackend.service.interfaces;

import com.gwuy.sba301.trafficdetectionbackend.dto.response.SignalHistoryResponse;
import com.gwuy.sba301.trafficdetectionbackend.entity.SignalHistory;

import java.util.List;

public interface ISignalHistoryService {
    void saveAll(List<SignalHistory> histories);
    List<SignalHistoryResponse> getAllSignalHistory();
}

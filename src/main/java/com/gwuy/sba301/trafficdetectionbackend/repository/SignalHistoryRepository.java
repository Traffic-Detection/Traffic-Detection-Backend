package com.gwuy.sba301.trafficdetectionbackend.repository;

import com.gwuy.sba301.trafficdetectionbackend.entity.SignalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SignalHistoryRepository extends JpaRepository<SignalHistory, Long> {

    List<SignalHistory> findByIntersectionIdOrderByAppliedAtDesc(Long intersectionId);
}

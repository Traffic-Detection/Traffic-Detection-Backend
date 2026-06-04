package com.gwuy.sba301.trafficdetectionbackend.repository;

import com.gwuy.sba301.trafficdetectionbackend.entity.TrafficLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TrafficLogRepository extends JpaRepository<TrafficLog, Long> {
    Optional<TrafficLog> findFirstByLaneIdOrderByRecordedAtDesc(Long laneId);
}
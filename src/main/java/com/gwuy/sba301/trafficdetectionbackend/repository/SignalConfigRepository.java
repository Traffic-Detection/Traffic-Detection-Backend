package com.gwuy.sba301.trafficdetectionbackend.repository;

import com.gwuy.sba301.trafficdetectionbackend.entity.SignalConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SignalConfigRepository extends JpaRepository<SignalConfig, Long> {

    List<SignalConfig> findByIntersectionId(Long intersectionId);

    Optional<SignalConfig> findByIntersectionIdAndLaneId(Long intersectionId, Long laneId);
}
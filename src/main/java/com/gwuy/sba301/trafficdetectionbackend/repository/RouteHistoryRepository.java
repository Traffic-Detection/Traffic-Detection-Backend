package com.gwuy.sba301.trafficdetectionbackend.repository;

import com.gwuy.sba301.trafficdetectionbackend.entity.RouteHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link RouteHistory} entity.
 * Provides queries for route recommendation history retrieval.
 */
@Repository
public interface RouteHistoryRepository extends JpaRepository<RouteHistory, Long> {

    /**
     * Get the 20 most recent route recommendations.
     */
    List<RouteHistory> findTop20ByOrderByCreatedAtDesc();
}

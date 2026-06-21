package com.gwuy.sba301.trafficdetectionbackend.repository;

import com.gwuy.sba301.trafficdetectionbackend.entity.Intersection;
import com.gwuy.sba301.trafficdetectionbackend.enums.OperatingMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IntersectionRepository extends JpaRepository<Intersection, Long> {

    @Query("SELECT DISTINCT i FROM Intersection i JOIN Lane l ON l.intersection.id = i.id WHERE i.operatingMode = :operatingMode")
    List<Intersection> findByOperatingModeWithLanes(@Param("operatingMode") OperatingMode operatingMode);
}
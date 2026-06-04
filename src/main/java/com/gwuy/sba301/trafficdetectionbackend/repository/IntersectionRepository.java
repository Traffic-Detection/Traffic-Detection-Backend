package com.gwuy.sba301.trafficdetectionbackend.repository;

import com.gwuy.sba301.trafficdetectionbackend.entity.Intersection;
import com.gwuy.sba301.trafficdetectionbackend.enums.OperatingMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IntersectionRepository extends JpaRepository<Intersection, Long> {

    List<Intersection> findByOperatingMode(OperatingMode operatingMode);
}
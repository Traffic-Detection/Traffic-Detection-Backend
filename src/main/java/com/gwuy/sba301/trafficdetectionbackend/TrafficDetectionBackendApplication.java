package com.gwuy.sba301.trafficdetectionbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.gwuy.sba301.trafficdetectionbackend", "com.atcs"})
@EnableScheduling
public class TrafficDetectionBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrafficDetectionBackendApplication.class, args);
    }

}

package com.gwuy.sba301.trafficdetectionbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication(scanBasePackages = {
        "com.gwuy.sba301.trafficdetectionbackend",
        "com.atcs"
})
@EnableScheduling
public class TrafficDetectionBackendApplication {

    public static void main(String[] args) {

        TimeZone.setDefault(
                TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
        );

        SpringApplication.run(
                TrafficDetectionBackendApplication.class,
                args
        );
    }

}

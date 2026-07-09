package com.gwuy.sba301.trafficdetectionbackend.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "r2")
public class R2Properties {

    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucketName;
    private String publicBaseUrl;
}

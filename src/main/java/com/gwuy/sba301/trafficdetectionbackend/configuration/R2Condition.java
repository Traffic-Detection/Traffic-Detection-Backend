package com.gwuy.sba301.trafficdetectionbackend.configuration;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

public class R2Condition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String endpoint = context.getEnvironment().getProperty("r2.endpoint");
        return StringUtils.hasText(endpoint);
    }
}

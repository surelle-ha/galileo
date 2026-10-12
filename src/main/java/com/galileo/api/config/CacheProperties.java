package com.galileo.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties("app.cache")
public record CacheProperties(
        @DefaultValue("PT10M") Duration departmentTtl,
        @DefaultValue("PT5M") Duration teamTtl,
        @DefaultValue("PT2M") Duration userTtl,
        @DefaultValue("PT30M") Duration roleTtl
) {
}

package com.galileo.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties("app.security.jwt")
public record JwtProperties(
        @DefaultValue("galileo-api") String issuer,
        String secret,
        @DefaultValue("PT1H") Duration accessTokenTtl
) {
    public JwtProperties {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("JWT_SECRET must contain at least 32 characters");
        }
    }
}

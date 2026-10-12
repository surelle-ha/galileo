package com.galileo.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app.bootstrap")
public record BootstrapProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("admin@galileo.local") String adminEmail,
        @DefaultValue("Admin123!") String adminPassword
) {
}

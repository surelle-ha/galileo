package com.galileo.api.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Optional Tomcat connector for environments where the Windows NIO selector is unavailable.
 * The default remains Tomcat NIO in normal local and production runs.
 */
@Configuration
@ConditionalOnProperty(prefix = "app.server", name = "tomcat-nio2", havingValue = "true")
public class TomcatNio2Config {

    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatNio2Customizer() {
        return factory -> factory.setProtocol("org.apache.coyote.http11.Http11Nio2Protocol");
    }
}

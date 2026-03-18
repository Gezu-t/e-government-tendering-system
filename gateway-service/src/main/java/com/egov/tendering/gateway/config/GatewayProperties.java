package com.egov.tendering.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Externalized gateway configuration bound from the {@code app.gateway.*} namespace.
 *
 * <p>Security-relevant lists:
 * <ul>
 *   <li>{@code public-paths}     – paths accessible without authentication (any HTTP method)</li>
 *   <li>{@code public-get-paths} – paths accessible without authentication for GET only</li>
 * </ul>
 * To add a new public endpoint, update {@code application.yml} — no code change required.
 */
@Configuration
@ConfigurationProperties(prefix = "app.gateway")
@Data
public class GatewayProperties {

    private int defaultTimeout = 10000;
    private int connectTimeout = 2000;

    /** Paths that are publicly accessible for any HTTP method (except infrastructure paths). */
    private List<String> publicPaths = new ArrayList<>();

    /** Paths that are publicly accessible for GET requests only. */
    private List<String> publicGetPaths = new ArrayList<>();
}

package com.egov.tendering.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter;
import org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.security.web.server.util.matcher.OrServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Security configuration for the API Gateway.
 *
 * <p>Public path lists are injected from {@code GatewayProperties} (app.gateway.public-paths and
 * app.gateway.public-get-paths in application.yml).  Adding a new unauthenticated endpoint
 * requires only a config change — no modification to this class.
 *
 * <p>Infrastructure paths (/actuator/**, OPTIONS preflight) are always permitted and are not
 * moved to config as they are not operational concerns.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${app.security.jwt.secret}")
    private String jwtSecret;

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:5173,http://localhost:4200}")
    private String allowedOrigins;

    @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${cors.allowed-headers:*}")
    private String allowedHeaders;

    @Value("${cors.max-age:3600}")
    private long corsMaxAge;

    private final GatewayProperties gatewayProps;

    public SecurityConfig(GatewayProperties gatewayProps) {
        this.gatewayProps = gatewayProps;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        List<ServerWebExchangeMatcher> permitMatchers = new ArrayList<>();

        // Infrastructure — always open
        permitMatchers.add(ServerWebExchangeMatchers.pathMatchers("/actuator/**"));
        permitMatchers.add(ServerWebExchangeMatchers.pathMatchers(HttpMethod.OPTIONS, "/**"));

        // Operationally configured public paths (any method)
        if (!gatewayProps.getPublicPaths().isEmpty()) {
            permitMatchers.add(ServerWebExchangeMatchers.pathMatchers(
                    gatewayProps.getPublicPaths().toArray(new String[0])));
        }

        // Operationally configured GET-only public paths
        if (!gatewayProps.getPublicGetPaths().isEmpty()) {
            permitMatchers.add(ServerWebExchangeMatchers.pathMatchers(
                    HttpMethod.GET, gatewayProps.getPublicGetPaths().toArray(new String[0])));
        }

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .headers(headers -> headers
                        // Prevent clickjacking
                        .frameOptions(fo -> fo.mode(XFrameOptionsServerHttpHeadersWriter.Mode.DENY))
                        // Prevent MIME-type sniffing
                        .contentTypeOptions(co -> {})
                        // Enforce HTTPS for 1 year, include subdomains
                        .hsts(hsts -> hsts
                                .maxAge(Duration.ofDays(365))
                                .includeSubdomains(true)
                                .preload(false))
                        // Restrict resource loading — API gateway only serves JSON
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'none'; frame-ancestors 'none'; form-action 'none'"))
                        // Don't leak referrer to other origins
                        .referrerPolicy(rp -> rp.policy(
                                ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        // Restrict browser features
                        .permissionsPolicy(pp -> pp.policy(
                                "camera=(), microphone=(), geolocation=(), payment=()"))
                        // Don't cache sensitive API responses
                        .cache(ServerHttpSecurity.HeaderSpec.CacheSpec::disable)
                )
                .authorizeExchange(exchanges -> exchanges
                        .matchers(new OrServerWebExchangeMatcher(permitMatchers)).permitAll()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
                .build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        SecretKeySpec secretKey = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        return NimbusReactiveJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS512).build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOrigins(splitProperty(allowedOrigins));
        corsConfig.setAllowedMethods(splitProperty(allowedMethods));
        corsConfig.setAllowedHeaders(splitProperty(allowedHeaders));
        corsConfig.setMaxAge(corsMaxAge);
        corsConfig.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        return source;
    }

    private List<String> splitProperty(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(entry -> !entry.isEmpty())
                .toList();
    }
}

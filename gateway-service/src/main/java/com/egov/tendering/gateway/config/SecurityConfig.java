package com.egov.tendering.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoders;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security configuration for the API Gateway
 * Handles authentication, authorization, and security concerns
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:5173,http://localhost:4200}")
    private String allowedOrigins;

    @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${cors.allowed-headers:*}")
    private String allowedHeaders;

    @Value("${cors.max-age:3600}")
    private long corsMaxAge;

    /**
     * Configures security rules for the gateway
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeExchange(exchanges -> exchanges
                        // Public endpoints that don't require authentication
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers("/api/auth/**").permitAll()

                        // Swagger/OpenAPI endpoints
                        .pathMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // Service-specific public endpoints
                        .pathMatchers("/api/users/register").permitAll()

                        // Public tender browsing (read-only, no auth required)
                        .pathMatchers(org.springframework.http.HttpMethod.GET, "/api/tenders", "/api/tenders/*").permitAll()
                        .pathMatchers(org.springframework.http.HttpMethod.GET, "/api/tenders/*/clarifications/public").permitAll()

                        // OPTIONS preflight requests
                        .pathMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()

                        // Secured endpoints requiring authentication
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
                .build();
    }

    /**
     * JWT decoder for validating tokens
     */
    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        return ReactiveJwtDecoders.fromIssuerLocation(issuerUri);
    }

    /**
     * CORS configuration
     */
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

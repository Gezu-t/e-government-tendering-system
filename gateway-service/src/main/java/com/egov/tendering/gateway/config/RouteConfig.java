package com.egov.tendering.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Route configuration for the API Gateway
 * Defines routing rules for incoming requests to appropriate microservices
 */
@Configuration
public class RouteConfig {

    /**
     * Configures routes to various microservices in the system
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // User Service Routes
                .route("user-service", r -> r.path("/api/users/**")
                        .uri("lb://user-service"))

                // Tender Service Routes
                .route("tender-service", r -> r.path("/api/tenders/**")
                        .uri("lb://tender-service"))

                // Bidding Service Routes
                .route("bidding-service", r -> r.path("/api/bids/**")
                        .uri("lb://bidding-service"))

                // Contract Service Routes
                .route("contract-service", r -> r.path("/api/contracts/**")
                        .uri("lb://contract-service"))

                // Document Service Routes
                .route("document-service", r -> r.path("/api/documents/**")
                        .uri("lb://document-service"))

                // Notification Service Routes
                .route("notification-service", r -> r.path("/api/notifications/**")
                        .uri("lb://notification-service"))

                // Evaluation Service Routes
                .route("evaluation-service", r -> r.path("/api/evaluations/**")
                        .uri("lb://evaluation-service"))

                // Audit Service Routes
                .route("audit-service", r -> r.path("/api/audit/**")
                        .uri("lb://audit-service"))
                .build();
    }
}

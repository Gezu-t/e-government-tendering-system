package com.egov.tendering.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Route configuration for the API Gateway.
 * All endpoints use the /api/ prefix (no versioning in path).
 */
@Configuration
public class RouteConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()

                // ============================================================
                // USER SERVICE (port 8081)
                // ============================================================
                .route("user-auth", r -> r.path("/api/auth/**")
                        .uri("lb://user-service"))
                .route("user-service", r -> r.path("/api/users/**")
                        .uri("lb://user-service"))
                .route("user-organizations", r -> r.path("/api/organizations/**")
                        .uri("lb://user-service"))
                .route("user-vendor-qualifications", r -> r.path("/api/vendor-qualifications/**")
                        .uri("lb://user-service"))
                .route("user-blacklist", r -> r.path("/api/blacklist/**")
                        .uri("lb://user-service"))

                // ============================================================
                // TENDER SERVICE (port 8082)
                // ============================================================
                .route("tender-service", r -> r.path("/api/tenders/**")
                        .uri("lb://tender-service"))
                .route("tender-categories", r -> r.path("/api/tenderCategory/**")
                        .uri("lb://tender-service"))

                // ============================================================
                // BIDDING SERVICE (port 8083)
                // ============================================================
                .route("bidding-service", r -> r.path("/api/bids/**")
                        .uri("lb://bidding-service"))
                .route("bidding-signatures", r -> r.path("/api/signatures/**")
                        .uri("lb://bidding-service"))
                .route("bidding-anti-collusion", r -> r.path("/api/anti-collusion/**")
                        .uri("lb://bidding-service"))
                .route("bidding-files", r -> r.path("/api/files/**")
                        .uri("lb://bidding-service"))
                .route("bidding-compliance", r -> r.path("/api/compliance/**")
                        .uri("lb://bidding-service"))
                .route("bidding-debug", r -> r.path("/api/debug/**")
                        .uri("lb://bidding-service"))

                // ============================================================
                // CONTRACT SERVICE (port 8084)
                // ============================================================
                .route("contract-service", r -> r.path("/api/contracts/**")
                        .uri("lb://contract-service"))
                .route("contract-vendor-performance", r -> r.path("/api/vendor-performance/**")
                        .uri("lb://contract-service"))

                // ============================================================
                // DOCUMENT SERVICE (port 8085)
                // ============================================================
                .route("document-service", r -> r.path("/api/documents/**")
                        .uri("lb://document-service"))

                // ============================================================
                // NOTIFICATION SERVICE (port 8086)
                // ============================================================
                .route("notification-service", r -> r.path("/api/notifications/**")
                        .uri("lb://notification-service"))

                // ============================================================
                // EVALUATION SERVICE (port 8087)
                // ============================================================
                .route("evaluation-service", r -> r.path("/api/evaluations/**")
                        .uri("lb://evaluation-service"))
                .route("evaluation-reviews", r -> r.path("/api/reviews/**")
                        .uri("lb://evaluation-service"))
                .route("evaluation-rankings", r -> r.path("/api/rankings/**")
                        .uri("lb://evaluation-service"))
                .route("evaluation-allocations", r -> r.path("/api/allocations/**")
                        .uri("lb://evaluation-service"))
                .route("evaluation-multi-criteria", r -> r.path("/api/multi-criteria/**")
                        .uri("lb://evaluation-service"))
                .route("evaluation-conflict-of-interest", r -> r.path("/api/conflict-of-interest/**")
                        .uri("lb://evaluation-service"))

                // ============================================================
                // AUDIT SERVICE (port 8088)
                // ============================================================
                .route("audit-service", r -> r.path("/api/audit/**")
                        .uri("lb://audit-service"))
                .route("audit-reports", r -> r.path("/api/reports/**")
                        .uri("lb://audit-service"))

                .build();
    }
}

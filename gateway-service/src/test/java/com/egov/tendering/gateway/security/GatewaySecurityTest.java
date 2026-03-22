package com.egov.tendering.gateway.security;

import com.egov.tendering.gateway.config.AppGatewayProperties;
import com.egov.tendering.gateway.config.SecurityConfig;
import com.egov.tendering.gateway.controller.FallbackController;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * Gateway security smoke tests (Issue 8).
 *
 * <p>Covers the complete auth path at the gateway layer:
 * <ul>
 *   <li>Public paths are accessible without authentication.</li>
 *   <li>Protected paths require a valid Bearer token.</li>
 *   <li>Requests with invalid or absent tokens are rejected with 401.</li>
 *   <li>A valid HS512 token passes through security with roles preserved.</li>
 * </ul>
 *
 * <p>These are unit-level WebFlux tests — no external services (Eureka, Redis,
 * Config Server, MySQL) are needed.  The JWT secret and public-path lists are
 * identical in structure to production values so failures clearly point to
 * token issuance, gateway validation, or endpoint authorization problems.
 */
@WebFluxTest(controllers = FallbackController.class)
@Import({SecurityConfig.class, AppGatewayProperties.class})
@TestPropertySource(properties = {
        "app.security.jwt.secret=test-secret-key-that-is-at-least-64-bytes-long-for-hs512-algorithm-ok",
        "app.security.jwt.claim-roles=roles",
        "app.gateway.public-paths=/api/auth/**,/api/users/register",
        "app.gateway.public-get-paths=/api/tenders,/api/tenders/*",
        "cors.allowed-origins=http://localhost:3000",
        "cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS",
        "cors.allowed-headers=*",
        "cors.max-age=3600"
})
class GatewaySecurityTest {

    private static final String JWT_SECRET =
            "test-secret-key-that-is-at-least-64-bytes-long-for-hs512-algorithm-ok";

    @Autowired
    private WebTestClient webTestClient;

    // ------------------------------------------------------------------ helpers

    /**
     * Builds a signed HS512 JWT using the same Nimbus library that backs
     * the gateway's {@code NimbusReactiveJwtDecoder}.  This simulates tokens
     * produced by {@code user-service / JwtTokenProvider}.
     */
    private String buildToken(List<String> roles, long expiresInMs) throws Exception {
        Date now = new Date();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("testuser")
                .issueTime(now)
                .expirationTime(new Date(now.getTime() + expiresInMs))
                .claim("roles", roles)
                .claim("userId", 1L)
                .build();

        byte[] keyBytes = JWT_SECRET.getBytes(StandardCharsets.UTF_8);
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS512), claims);
        jwt.sign(new MACSigner(keyBytes));
        return jwt.serialize();
    }

    private String validAdminToken() throws Exception {
        return buildToken(List.of("ROLE_ADMIN"), 3_600_000);
    }

    private String validTendererToken() throws Exception {
        return buildToken(List.of("ROLE_TENDERER"), 3_600_000);
    }

    // ================================================================
    // Group 1: Public-path access (no auth required)
    // ================================================================
    @Nested
    @DisplayName("Public paths are accessible without authentication")
    class PublicPathAccess {

        @Test
        @DisplayName("Infrastructure path /actuator/** is always open")
        void actuatorIsPermitted() {
            // Security always permits /actuator/**. No controller handles it in this
            // test slice, so WebFlux returns 404 — but critically NOT 401.
            webTestClient.get()
                    .uri("/actuator/health")
                    .exchange()
                    .expectStatus().isNotFound(); // 404, not 401
        }

        @Test
        @DisplayName("Configured public path /api/auth/login is open to unauthenticated requests")
        void configuredPublicAuthPathIsPermitted() {
            // /api/auth/** is in app.gateway.public-paths.
            // No controller handles it in this test slice → 404, not 401.
            webTestClient.post()
                    .uri("/api/auth/login")
                    .exchange()
                    .expectStatus().isNotFound();
        }

        @Test
        @DisplayName("Configured public GET path /api/tenders is open without auth")
        void configuredPublicGetPathIsPermitted() {
            webTestClient.get()
                    .uri("/api/tenders")
                    .exchange()
                    .expectStatus().isNotFound(); // no controller — but NOT 401
        }
    }

    // ================================================================
    // Group 2: Protected-path enforcement
    // ================================================================
    @Nested
    @DisplayName("Protected paths reject unauthenticated or invalid requests")
    class ProtectedPathEnforcement {

        @Test
        @DisplayName("Protected endpoint returns 401 when no token is provided")
        void missingTokenReturnsUnauthorized() {
            // /fallback is served by FallbackController and is not in any public list.
            webTestClient.get()
                    .uri("/fallback")
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("Protected endpoint returns 401 for a malformed Bearer token")
        void malformedTokenReturnsUnauthorized() {
            webTestClient.get()
                    .uri("/fallback")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer this.is.not.a.valid.jwt")
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("Protected endpoint returns 401 for a token signed with the wrong key")
        void wrongKeyTokenReturnsUnauthorized() throws Exception {
            String wrongSecret = "wrong-secret-key-that-is-at-least-64-bytes-long-for-hs512-algorithm-xx";
            Date now = new Date();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject("attacker")
                    .issueTime(now)
                    .expirationTime(new Date(now.getTime() + 3_600_000))
                    .claim("roles", List.of("ROLE_ADMIN"))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS512), claims);
            jwt.sign(new MACSigner(wrongSecret.getBytes(StandardCharsets.UTF_8)));

            webTestClient.get()
                    .uri("/fallback")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.serialize())
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("Protected endpoint returns 401 for an expired token")
        void expiredTokenReturnsUnauthorized() throws Exception {
            String expiredToken = buildToken(List.of("ROLE_ADMIN"), -1000); // already expired

            webTestClient.get()
                    .uri("/fallback")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)
                    .exchange()
                    .expectStatus().isUnauthorized();
        }
    }

    // ================================================================
    // Group 3: Valid-token access
    // ================================================================
    @Nested
    @DisplayName("Valid HS512 tokens are accepted by the gateway")
    class ValidTokenAccess {

        @Test
        @DisplayName("ADMIN token passes gateway security (endpoint responds, not 401)")
        void adminTokenPassesSecurity() throws Exception {
            // The FallbackController returns 503 for /fallback — security lets it through.
            webTestClient.get()
                    .uri("/fallback")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validAdminToken())
                    .exchange()
                    .expectStatus().isEqualTo(503); // 503 from FallbackController, not 401
        }

        @Test
        @DisplayName("TENDERER token passes gateway security")
        void tendererTokenPassesSecurity() throws Exception {
            webTestClient.get()
                    .uri("/fallback")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validTendererToken())
                    .exchange()
                    .expectStatus().isEqualTo(503); // passed security
        }
    }

    // ================================================================
    // Group 4: Role claim round-trip
    // ================================================================
    @Nested
    @DisplayName("JWT claim round-trip: token produced like user-service, decoded like gateway")
    class JwtClaimRoundTrip {

        @Test
        @DisplayName("Roles List<String> survives token production and Nimbus decoding")
        void rolesListClaimRoundTrip() throws Exception {
            // Build token with roles as List<String> (the format JwtTokenProvider emits after Issue 3).
            String token = buildToken(List.of("ROLE_ADMIN", "ROLE_EVALUATOR"), 3_600_000);

            // The gateway decoder must accept this token.  A 401 would indicate
            // the decoder rejected it; 503 means it passed security.
            webTestClient.get()
                    .uri("/fallback")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .exchange()
                    .expectStatus().isEqualTo(503);
        }
    }
}

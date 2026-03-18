package com.egov.tendering.tender.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class JwtUserIdExtractor {

    @Value("${app.security.jwt.claim-user-id:userId}")
    private String userIdClaimName;

    public Long requireUserId(Jwt jwt) {
        if (jwt == null) {
            throw new IllegalArgumentException("Authenticated JWT is required");
        }

        Object userIdClaim = jwt.getClaim(userIdClaimName);
        if (userIdClaim instanceof Number number) {
            return number.longValue();
        }
        if (userIdClaim != null && !userIdClaim.toString().isBlank()) {
            return Long.parseLong(userIdClaim.toString());
        }
        throw new IllegalStateException("JWT is missing required " + userIdClaimName + " claim");
    }
}

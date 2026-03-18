package com.egov.tendering.document.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class JwtUserIdExtractor {

    @Value("${app.security.jwt.claim-user-id:userId}")
    private String userIdClaimName;

    public String getUserIdAsString(Jwt jwt) {
        if (jwt == null) {
            throw new IllegalArgumentException("Authenticated JWT is required");
        }

        Object userIdClaim = jwt.getClaim(userIdClaimName);
        if (userIdClaim == null || userIdClaim.toString().isBlank()) {
            throw new IllegalStateException("JWT is missing required " + userIdClaimName + " claim");
        }
        return userIdClaim.toString();
    }
}

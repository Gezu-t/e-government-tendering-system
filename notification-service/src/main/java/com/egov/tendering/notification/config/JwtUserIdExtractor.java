package com.egov.tendering.notification.config;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class JwtUserIdExtractor {

    public String getUserIdAsString(Jwt jwt) {
        if (jwt == null) {
            throw new IllegalArgumentException("Authenticated JWT is required");
        }

        Object userIdClaim = jwt.getClaim("userId");
        if (userIdClaim == null || userIdClaim.toString().isBlank()) {
            throw new IllegalStateException("JWT is missing required userId claim");
        }
        return userIdClaim.toString();
    }
}

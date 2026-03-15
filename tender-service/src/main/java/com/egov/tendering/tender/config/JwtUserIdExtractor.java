package com.egov.tendering.tender.config;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class JwtUserIdExtractor {

    public Long requireUserId(Jwt jwt) {
        if (jwt == null) {
            throw new IllegalArgumentException("Authenticated JWT is required");
        }

        Object userIdClaim = jwt.getClaim("userId");
        if (userIdClaim instanceof Number number) {
            return number.longValue();
        }
        if (userIdClaim != null && !userIdClaim.toString().isBlank()) {
            return Long.parseLong(userIdClaim.toString());
        }
        throw new IllegalStateException("JWT is missing required userId claim");
    }
}

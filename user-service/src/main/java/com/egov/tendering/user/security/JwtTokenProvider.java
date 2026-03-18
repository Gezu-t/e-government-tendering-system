package com.egov.tendering.user.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtTokenProvider {

    private final Key key;
    private final long jwtExpirationInMs;
    private final String rolesClaimName;
    private final String permissionsClaimName;
    private final String userIdClaimName;

    public JwtTokenProvider(
            @Value("${app.security.jwt.secret}") String jwtSecret,
            @Value("${app.security.jwt.expiration}") long jwtExpirationInMs,
            @Value("${app.security.jwt.claim-roles:roles}") String rolesClaimName,
            @Value("${app.security.jwt.claim-permissions:permissions}") String permissionsClaimName,
            @Value("${app.security.jwt.claim-user-id:userId}") String userIdClaimName) {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.jwtExpirationInMs = jwtExpirationInMs;
        this.rolesClaimName = rolesClaimName;
        this.permissionsClaimName = permissionsClaimName;
        this.userIdClaimName = userIdClaimName;
    }

    public String generateToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        List<String> allAuthorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        List<String> permissions = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> !a.startsWith("ROLE_"))
                .collect(Collectors.toList());

        return Jwts.builder()
                .setSubject(userPrincipal.getUsername())
                .claim(rolesClaimName, allAuthorities)
                .claim(permissionsClaimName, permissions)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    public String generateToken(String username) {
        return generateToken(username, null, Collections.emptyList());
    }

    public String generateToken(String username, Collection<String> authorities) {
        return generateToken(username, null, authorities);
    }

    public String generateToken(String username, Long userId, Collection<String> authorities) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        List<String> permissionsList = authorities.stream()
                .filter(a -> !a.startsWith("ROLE_"))
                .collect(Collectors.toList());

        JwtBuilder builder = Jwts.builder()
                .setSubject(username)
                .claim(rolesClaimName, new ArrayList<>(authorities))
                .claim(permissionsClaimName, permissionsList)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512);

        if (userId != null) {
            builder.claim(userIdClaimName, userId);
        }

        return builder.compact();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    public Collection<? extends GrantedAuthority> getAuthoritiesFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        Object rolesClaim = claims.get(rolesClaimName);
        if (rolesClaim == null) {
            return Collections.emptyList();
        }
        if (rolesClaim instanceof List<?> list) {
            return list.stream()
                    .map(Object::toString)
                    .filter(r -> !r.isBlank())
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }
        // Fallback: comma-separated string (legacy tokens)
        String rolesStr = rolesClaim.toString();
        if (rolesStr.isBlank()) return Collections.emptyList();
        return Arrays.stream(rolesStr.split(","))
                .map(String::trim)
                .filter(r -> !r.isEmpty())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    public UsernamePasswordAuthenticationToken getAuthentication(String token, UserDetails userDetails) {
        Collection<? extends GrantedAuthority> authorities = getAuthoritiesFromToken(token);

        return new UsernamePasswordAuthenticationToken(userDetails, "", authorities);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (SecurityException ex) {
            log.error("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty");
        }
        return false;
    }
}

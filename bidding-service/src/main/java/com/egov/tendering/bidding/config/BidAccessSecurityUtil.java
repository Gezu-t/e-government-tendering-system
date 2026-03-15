package com.egov.tendering.bidding.config;

import com.egov.tendering.bidding.dal.model.Bid;
import com.egov.tendering.bidding.dal.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BidAccessSecurityUtil {

    private final BidRepository bidRepository;
    private final JwtUserIdExtractor jwtUserIdExtractor;

    public boolean canAccessBid(Long bidId) {
        if (hasPrivilegedRole()) {
            return true;
        }
        return isBidOwner(bidId);
    }

    public boolean isBidOwner(Long bidId) {
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            return false;
        }

        Bid bid = bidRepository.findById(bidId).orElse(null);
        return bid != null && currentUserId.equals(bid.getTendererId());
    }

    public boolean canAccessTenderer(Long tendererId) {
        if (hasPrivilegedRole()) {
            return true;
        }

        Long currentUserId = getCurrentUserId();
        return currentUserId != null && currentUserId.equals(tendererId);
    }

    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            try {
                return jwtUserIdExtractor.requireUserId(jwt);
            } catch (RuntimeException ex) {
                return null;
            }
        }

        return parseLongOrNull(authentication.getName());
    }

    public boolean hasPrivilegedRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority) || "ROLE_EVALUATOR".equals(authority));
    }

    private Long parseLongOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}

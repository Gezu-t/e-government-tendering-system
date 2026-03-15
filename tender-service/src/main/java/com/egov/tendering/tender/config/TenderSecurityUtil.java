package com.egov.tendering.tender.config;

import com.egov.tendering.tender.dal.model.PreBidClarification;
import com.egov.tendering.tender.dal.model.Tender;
import com.egov.tendering.tender.dal.model.TenderStatus;
import com.egov.tendering.tender.dal.repository.PreBidClarificationRepository;
import com.egov.tendering.tender.dal.repository.TenderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component("tenderSecurityUtil")
@RequiredArgsConstructor
public class TenderSecurityUtil {

    private static final Set<TenderStatus> PUBLICLY_VISIBLE_STATUSES = Set.of(
            TenderStatus.PUBLISHED,
            TenderStatus.AMENDED,
            TenderStatus.CLOSED,
            TenderStatus.AWARDED,
            TenderStatus.CANCELLED
    );

    private final PreBidClarificationRepository clarificationRepository;
    private final TenderRepository tenderRepository;
    private final JwtUserIdExtractor jwtUserIdExtractor;

    public boolean canAccessTender(Long tenderId) {
        Tender tender = tenderRepository.findById(tenderId).orElse(null);
        if (tender == null) {
            return false;
        }

        if (PUBLICLY_VISIBLE_STATUSES.contains(tender.getStatus())) {
            return true;
        }

        if (isAdmin()) {
            return true;
        }

        Long currentUserId = getCurrentUserId();
        return currentUserId != null && currentUserId.equals(tender.getTendereeId());
    }

    public boolean canManageTender(Long tenderId) {
        Tender tender = tenderRepository.findById(tenderId).orElse(null);
        if (tender == null) {
            return false;
        }

        if (isAdmin()) {
            return true;
        }

        Long currentUserId = getCurrentUserId();
        return currentUserId != null && currentUserId.equals(tender.getTendereeId());
    }

    public boolean canManageClarification(Long tenderId, Long clarificationId) {
        PreBidClarification clarification = clarificationRepository.findById(clarificationId).orElse(null);
        if (clarification == null || !clarification.getTenderId().equals(tenderId)) {
            return false;
        }
        return canManageTender(tenderId);
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Jwt jwt)) {
            return null;
        }

        try {
            return jwtUserIdExtractor.requireUserId(jwt);
        } catch (RuntimeException ex) {
            return null;
        }
    }
}

package com.egov.tendering.user.security;

import com.egov.tendering.user.dal.model.User;
import com.egov.tendering.user.dal.model.UserOrganization;
import com.egov.tendering.user.dal.repository.UserOrganizationRepository;
import com.egov.tendering.user.dal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserSecurityUtil {

    private final UserRepository userRepository;
    private final UserOrganizationRepository userOrganizationRepository;

    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        return user != null ? user.getId() : null;
    }

    public boolean canAccessUser(Long userId) {
        if (hasRole("ROLE_ADMIN")) {
            return true;
        }

        Long currentUserId = getCurrentUserId();
        return currentUserId != null && currentUserId.equals(userId);
    }

    public boolean canAccessUsername(String username) {
        if (hasRole("ROLE_ADMIN")) {
            return true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return username.equals(authentication.getName());
    }

    public boolean canManageOrganization(Long organizationId) {
        if (hasRole("ROLE_ADMIN")) {
            return true;
        }

        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            return false;
        }

        UserOrganization userOrganization = userOrganizationRepository
                .findByUserIdAndOrganizationId(currentUserId, organizationId)
                .orElse(null);

        return userOrganization != null && "ADMIN".equalsIgnoreCase(userOrganization.getRole());
    }

    private boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
    }
}

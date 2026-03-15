package com.egov.tendering.contract.config;

import com.egov.tendering.contract.dal.model.Contract;
import com.egov.tendering.contract.dal.model.ContractMilestone;
import com.egov.tendering.contract.dal.repository.ContractMilestoneRepository;
import com.egov.tendering.contract.dal.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContractSecurityUtil {

    private final ContractRepository contractRepository;
    private final ContractMilestoneRepository milestoneRepository;
    private final JwtUserIdExtractor jwtUserIdExtractor;

    public boolean canAccessContract(Long contractId) {
        if (hasRole("ROLE_ADMIN")) {
            return true;
        }

        return contractRepository.findById(contractId)
                .map(this::canAccessContract)
                .orElse(false);
    }

    public boolean canManageContract(Long contractId) {
        if (hasRole("ROLE_ADMIN")) {
            return true;
        }

        return contractRepository.findById(contractId)
                .map(contract -> hasRole("ROLE_TENDEREE") && getCurrentUsername().equals(contract.getCreatedBy()))
                .orElse(false);
    }

    public boolean canAccessBidderContracts(Long bidderId) {
        if (hasRole("ROLE_ADMIN") || hasRole("ROLE_TENDEREE")) {
            return true;
        }
        return hasRole("ROLE_TENDERER") && bidderId != null && bidderId.equals(getCurrentUserId());
    }

    public boolean canAccessTenderContracts(Long tenderId) {
        if (hasRole("ROLE_ADMIN")) {
            return true;
        }

        if (!hasRole("ROLE_TENDEREE")) {
            return false;
        }

        String currentUsername = getCurrentUsername();
        if (currentUsername == null) {
            return false;
        }

        return contractRepository.findByTenderId(tenderId).stream()
                .allMatch(contract -> currentUsername.equals(contract.getCreatedBy()));
    }

    public boolean canAccessMilestone(Long contractId, Long milestoneId) {
        if (!canAccessContract(contractId)) {
            return false;
        }
        return milestoneRepository.findById(milestoneId)
                .map(ContractMilestone::getContract)
                .map(Contract::getId)
                .map(contractId::equals)
                .orElse(false);
    }

    public boolean canManageMilestone(Long contractId, Long milestoneId) {
        if (!canManageContract(contractId)) {
            return false;
        }
        return milestoneRepository.findById(milestoneId)
                .map(ContractMilestone::getContract)
                .map(Contract::getId)
                .map(contractId::equals)
                .orElse(false);
    }

    private boolean canAccessContract(Contract contract) {
        if (hasRole("ROLE_TENDEREE") && getCurrentUsername().equals(contract.getCreatedBy())) {
            return true;
        }
        return hasRole("ROLE_TENDERER")
                && contract.getBidderId() != null
                && contract.getBidderId().equals(getCurrentUserId());
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        return authentication.getName();
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return null;
        }

        try {
            return jwtUserIdExtractor.requireUserId(jwt);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> role.equals(authority.getAuthority()));
    }
}

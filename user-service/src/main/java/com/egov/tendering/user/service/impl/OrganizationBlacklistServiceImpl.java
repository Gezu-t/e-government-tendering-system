package com.egov.tendering.user.service.impl;

import com.egov.tendering.user.dal.dto.BlacklistRequest;
import com.egov.tendering.user.dal.dto.OrganizationBlacklistDTO;
import com.egov.tendering.user.dal.model.Organization;
import com.egov.tendering.user.dal.model.OrganizationBlacklist;
import com.egov.tendering.user.dal.repository.OrganizationBlacklistRepository;
import com.egov.tendering.user.dal.repository.OrganizationRepository;
import com.egov.tendering.user.service.OrganizationBlacklistService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationBlacklistServiceImpl implements OrganizationBlacklistService {

    private final OrganizationBlacklistRepository blacklistRepository;
    private final OrganizationRepository organizationRepository;

    @Override
    @Transactional
    public OrganizationBlacklistDTO blacklistOrganization(BlacklistRequest request, Long imposedBy) {
        log.info("Blacklisting organization {} type {} by user {}",
                request.getOrganizationId(), request.getType(), imposedBy);

        organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + request.getOrganizationId()));

        OrganizationBlacklist blacklist = OrganizationBlacklist.builder()
                .organizationId(request.getOrganizationId())
                .type(request.getType())
                .reason(request.getReason())
                .referenceNumber(request.getReferenceNumber())
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveUntil(request.getEffectiveUntil())
                .isPermanent(Boolean.TRUE.equals(request.getIsPermanent()))
                .imposedBy(imposedBy)
                .active(true)
                .build();

        blacklist = blacklistRepository.save(blacklist);
        log.warn("Organization {} has been blacklisted ({}): {}", request.getOrganizationId(), request.getType(), request.getReason());

        return toDTO(blacklist);
    }

    @Override
    @Transactional
    public OrganizationBlacklistDTO liftBlacklist(Long blacklistId, String reason, Long liftedBy) {
        log.info("Lifting blacklist {} by user {}", blacklistId, liftedBy);

        OrganizationBlacklist blacklist = blacklistRepository.findById(blacklistId)
                .orElseThrow(() -> new EntityNotFoundException("Blacklist entry not found: " + blacklistId));

        blacklist.setActive(false);
        blacklist.setLiftedBy(liftedBy);
        blacklist.setLiftedAt(LocalDateTime.now());
        blacklist.setLiftReason(reason);
        blacklist = blacklistRepository.save(blacklist);

        log.info("Blacklist {} lifted for organization {}", blacklistId, blacklist.getOrganizationId());
        return toDTO(blacklist);
    }

    @Override
    public boolean isOrganizationBlacklisted(Long organizationId) {
        return blacklistRepository.isOrganizationBlacklisted(organizationId, LocalDate.now());
    }

    @Override
    public List<OrganizationBlacklistDTO> getBlacklistsForOrganization(Long organizationId) {
        return blacklistRepository.findByOrganizationIdAndActiveTrue(organizationId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public Page<OrganizationBlacklistDTO> getActiveBlacklists(Pageable pageable) {
        return blacklistRepository.findByActiveTrue(pageable).map(this::toDTO);
    }

    @Override
    @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
    @Transactional
    public void processExpiredBlacklists() {
        log.info("Processing expired blacklists");
        List<OrganizationBlacklist> expired = blacklistRepository.findExpiredBlacklists(LocalDate.now());
        for (OrganizationBlacklist b : expired) {
            b.setActive(false);
            blacklistRepository.save(b);
            log.info("Blacklist {} expired for organization {}", b.getId(), b.getOrganizationId());
        }
        log.info("Processed {} expired blacklists", expired.size());
    }

    private OrganizationBlacklistDTO toDTO(OrganizationBlacklist b) {
        String orgName = organizationRepository.findById(b.getOrganizationId())
                .map(Organization::getName).orElse("Unknown");
        return OrganizationBlacklistDTO.builder()
                .id(b.getId())
                .organizationId(b.getOrganizationId())
                .organizationName(orgName)
                .type(b.getType())
                .reason(b.getReason())
                .referenceNumber(b.getReferenceNumber())
                .effectiveFrom(b.getEffectiveFrom())
                .effectiveUntil(b.getEffectiveUntil())
                .isPermanent(b.getIsPermanent())
                .imposedBy(b.getImposedBy())
                .active(b.getActive())
                .createdAt(b.getCreatedAt())
                .build();
    }
}

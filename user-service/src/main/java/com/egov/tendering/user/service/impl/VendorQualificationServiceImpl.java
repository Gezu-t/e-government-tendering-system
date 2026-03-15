package com.egov.tendering.user.service.impl;

import com.egov.tendering.user.dal.dto.QualificationReviewRequest;
import com.egov.tendering.user.dal.dto.VendorQualificationDTO;
import com.egov.tendering.user.dal.dto.VendorQualificationRequest;
import com.egov.tendering.user.dal.model.Organization;
import com.egov.tendering.user.dal.model.QualificationStatus;
import com.egov.tendering.user.dal.model.VendorQualification;
import com.egov.tendering.user.dal.repository.OrganizationRepository;
import com.egov.tendering.user.dal.repository.VendorQualificationRepository;
import com.egov.tendering.user.service.VendorQualificationService;
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
public class VendorQualificationServiceImpl implements VendorQualificationService {

    private final VendorQualificationRepository qualificationRepository;
    private final OrganizationRepository organizationRepository;

    @Override
    @Transactional
    public VendorQualificationDTO submitQualification(VendorQualificationRequest request) {
        log.info("Submitting qualification for organization: {} category: {}",
                request.getOrganizationId(), request.getQualificationCategory());

        Organization org = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + request.getOrganizationId()));

        // Check if qualification already exists for this category
        qualificationRepository.findByOrganizationIdAndQualificationCategory(
                request.getOrganizationId(), request.getQualificationCategory())
                .ifPresent(existing -> {
                    if (existing.getStatus() == QualificationStatus.PENDING
                            || existing.getStatus() == QualificationStatus.UNDER_REVIEW) {
                        throw new IllegalStateException(
                                "A qualification application is already pending for this category");
                    }
                });

        VendorQualification qualification = VendorQualification.builder()
                .organizationId(request.getOrganizationId())
                .qualificationCategory(request.getQualificationCategory())
                .status(QualificationStatus.PENDING)
                .businessLicenseNumber(request.getBusinessLicenseNumber())
                .taxRegistrationNumber(request.getTaxRegistrationNumber())
                .yearsOfExperience(request.getYearsOfExperience())
                .annualRevenue(request.getAnnualRevenue())
                .employeeCount(request.getEmployeeCount())
                .pastContractsCount(request.getPastContractsCount())
                .certificationDetails(request.getCertificationDetails())
                .build();

        qualification = qualificationRepository.save(qualification);
        log.info("Qualification submitted successfully with ID: {}", qualification.getId());

        return toDTO(qualification, org.getName());
    }

    @Override
    public VendorQualificationDTO getQualificationById(Long qualificationId) {
        VendorQualification q = qualificationRepository.findById(qualificationId)
                .orElseThrow(() -> new EntityNotFoundException("Qualification not found: " + qualificationId));
        String orgName = getOrganizationName(q.getOrganizationId());
        return toDTO(q, orgName);
    }

    @Override
    public List<VendorQualificationDTO> getQualificationsByOrganization(Long organizationId) {
        String orgName = getOrganizationName(organizationId);
        return qualificationRepository.findByOrganizationId(organizationId)
                .stream()
                .map(q -> toDTO(q, orgName))
                .collect(Collectors.toList());
    }

    @Override
    public Page<VendorQualificationDTO> getQualificationsByStatus(QualificationStatus status, Pageable pageable) {
        return qualificationRepository.findByStatus(status, pageable)
                .map(q -> toDTO(q, getOrganizationName(q.getOrganizationId())));
    }

    @Override
    @Transactional
    public VendorQualificationDTO reviewQualification(Long qualificationId,
                                                       QualificationReviewRequest request,
                                                       Long reviewerId) {
        log.info("Reviewing qualification: {} by reviewer: {}", qualificationId, reviewerId);

        VendorQualification q = qualificationRepository.findById(qualificationId)
                .orElseThrow(() -> new EntityNotFoundException("Qualification not found: " + qualificationId));

        q.setStatus(request.getStatus());
        q.setReviewerId(reviewerId);
        q.setReviewComments(request.getComments());
        q.setReviewedAt(LocalDateTime.now());

        if (request.getQualificationScore() != null) {
            q.setQualificationScore(request.getQualificationScore());
        }

        if (request.getStatus() == QualificationStatus.QUALIFIED
                || request.getStatus() == QualificationStatus.CONDITIONALLY_QUALIFIED) {
            q.setValidFrom(request.getValidFrom() != null ? request.getValidFrom() : LocalDate.now());
            q.setValidUntil(request.getValidUntil() != null ? request.getValidUntil() : LocalDate.now().plusYears(1));
        }

        if (request.getStatus() == QualificationStatus.DISQUALIFIED) {
            q.setRejectionReason(request.getRejectionReason());
        }

        q = qualificationRepository.save(q);
        String orgName = getOrganizationName(q.getOrganizationId());

        log.info("Qualification {} reviewed. Status: {}", qualificationId, request.getStatus());
        return toDTO(q, orgName);
    }

    @Override
    public boolean isOrganizationQualified(Long organizationId) {
        return qualificationRepository.isOrganizationQualified(organizationId, LocalDate.now());
    }

    @Override
    @Scheduled(cron = "0 0 1 * * *") // Daily at 1 AM
    @Transactional
    public void checkForExpiredQualifications() {
        log.info("Checking for expired vendor qualifications");

        List<VendorQualification> expired = qualificationRepository
                .findExpiredQualifications(LocalDate.now());

        for (VendorQualification q : expired) {
            q.setStatus(QualificationStatus.EXPIRED);
            qualificationRepository.save(q);
            log.info("Qualification {} for organization {} has expired",
                    q.getId(), q.getOrganizationId());
        }

        log.info("Expired {} vendor qualifications", expired.size());
    }

    private String getOrganizationName(Long organizationId) {
        return organizationRepository.findById(organizationId)
                .map(Organization::getName)
                .orElse("Unknown");
    }

    private VendorQualificationDTO toDTO(VendorQualification q, String orgName) {
        return VendorQualificationDTO.builder()
                .id(q.getId())
                .organizationId(q.getOrganizationId())
                .organizationName(orgName)
                .qualificationCategory(q.getQualificationCategory())
                .status(q.getStatus())
                .businessLicenseNumber(q.getBusinessLicenseNumber())
                .taxRegistrationNumber(q.getTaxRegistrationNumber())
                .yearsOfExperience(q.getYearsOfExperience())
                .annualRevenue(q.getAnnualRevenue())
                .employeeCount(q.getEmployeeCount())
                .pastContractsCount(q.getPastContractsCount())
                .certificationDetails(q.getCertificationDetails())
                .qualificationScore(q.getQualificationScore())
                .reviewerId(q.getReviewerId())
                .reviewComments(q.getReviewComments())
                .reviewedAt(q.getReviewedAt())
                .validFrom(q.getValidFrom())
                .validUntil(q.getValidUntil())
                .rejectionReason(q.getRejectionReason())
                .createdAt(q.getCreatedAt())
                .build();
    }
}

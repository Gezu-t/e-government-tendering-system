package com.egov.tendering.contract.service.impl;

import com.egov.tendering.contract.dal.dto.VendorPerformanceDTO;
import com.egov.tendering.contract.dal.dto.VendorPerformanceRequest;
import com.egov.tendering.contract.dal.model.Contract;
import com.egov.tendering.contract.dal.model.ContractStatus;
import com.egov.tendering.contract.dal.model.VendorPerformance;
import com.egov.tendering.contract.dal.repository.ContractRepository;
import com.egov.tendering.contract.dal.repository.VendorPerformanceRepository;
import com.egov.tendering.contract.service.VendorPerformanceService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VendorPerformanceServiceImpl implements VendorPerformanceService {

    private static final EnumSet<ContractStatus> REVIEWABLE_STATUSES =
            EnumSet.of(ContractStatus.ACTIVE, ContractStatus.COMPLETED);

    private final VendorPerformanceRepository performanceRepository;
    private final ContractRepository contractRepository;

    @Override
    @Transactional
    public VendorPerformanceDTO submitPerformanceReview(Long contractId, VendorPerformanceRequest request, Long reviewerId) {
        log.info("Submitting performance review for contract {} by reviewer {}", contractId, reviewerId);

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new EntityNotFoundException("Contract not found: " + contractId));
        validateReviewableContract(contract);

        String normalizedReviewPeriod = request.getReviewPeriod().trim();
        if (performanceRepository.findByContractIdAndReviewPeriod(contractId, normalizedReviewPeriod).isPresent()) {
            throw new IllegalStateException("A performance review already exists for this contract and review period");
        }

        BigDecimal overallScore = request.getQualityScore()
                .add(request.getTimelinessScore())
                .add(request.getComplianceScore())
                .add(request.getCommunicationScore())
                .divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP);

        VendorPerformance performance = VendorPerformance.builder()
                .contractId(contractId)
                .vendorId(contract.getBidderId())
                .tenderId(contract.getTenderId())
                .qualityScore(request.getQualityScore())
                .timelinessScore(request.getTimelinessScore())
                .complianceScore(request.getComplianceScore())
                .communicationScore(request.getCommunicationScore())
                .overallScore(overallScore)
                .reviewComments(request.getReviewComments())
                .reviewedBy(reviewerId)
                .reviewPeriod(normalizedReviewPeriod)
                .build();

        performance = performanceRepository.save(performance);
        log.info("Performance review submitted. Overall score: {}", overallScore);

        return toDTO(performance);
    }

    @Override
    public List<VendorPerformanceDTO> getPerformancesByContract(Long contractId) {
        return performanceRepository.findByContractId(contractId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<VendorPerformanceDTO> getPerformancesByVendor(Long vendorId) {
        return performanceRepository.findByVendorIdOrderByCreatedAtDesc(vendorId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public BigDecimal getVendorAverageScore(Long vendorId) {
        BigDecimal avg = performanceRepository.getAverageOverallScore(vendorId);
        return avg != null ? avg.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    private VendorPerformanceDTO toDTO(VendorPerformance vp) {
        return VendorPerformanceDTO.builder()
                .id(vp.getId()).contractId(vp.getContractId()).vendorId(vp.getVendorId())
                .tenderId(vp.getTenderId()).qualityScore(vp.getQualityScore())
                .timelinessScore(vp.getTimelinessScore()).complianceScore(vp.getComplianceScore())
                .communicationScore(vp.getCommunicationScore()).overallScore(vp.getOverallScore())
                .milestonesCompleted(vp.getMilestonesCompleted()).milestonesTotal(vp.getMilestonesTotal())
                .milestonesOnTime(vp.getMilestonesOnTime()).penaltiesCount(vp.getPenaltiesCount())
                .penaltyAmount(vp.getPenaltyAmount()).reviewComments(vp.getReviewComments())
                .reviewedBy(vp.getReviewedBy()).reviewPeriod(vp.getReviewPeriod())
                .createdAt(vp.getCreatedAt()).build();
    }

    private void validateReviewableContract(Contract contract) {
        if (!REVIEWABLE_STATUSES.contains(contract.getStatus())) {
            throw new IllegalStateException("Vendor performance can only be recorded for active or completed contracts");
        }
    }
}

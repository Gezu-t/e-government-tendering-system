package com.egov.tendering.contract.dal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorPerformanceDTO {
    private Long id;
    private Long contractId;
    private Long vendorId;
    private Long tenderId;
    private BigDecimal qualityScore;
    private BigDecimal timelinessScore;
    private BigDecimal complianceScore;
    private BigDecimal communicationScore;
    private BigDecimal overallScore;
    private Integer milestonesCompleted;
    private Integer milestonesTotal;
    private Integer milestonesOnTime;
    private Integer penaltiesCount;
    private BigDecimal penaltyAmount;
    private String reviewComments;
    private Long reviewedBy;
    private String reviewPeriod;
    private LocalDateTime createdAt;
}

package com.egov.tendering.user.dal.dto;

import com.egov.tendering.user.dal.model.QualificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorQualificationDTO {
    private Long id;
    private Long organizationId;
    private String organizationName;
    private String qualificationCategory;
    private QualificationStatus status;
    private String businessLicenseNumber;
    private String taxRegistrationNumber;
    private Integer yearsOfExperience;
    private String annualRevenue;
    private Integer employeeCount;
    private Integer pastContractsCount;
    private String certificationDetails;
    private Integer qualificationScore;
    private Long reviewerId;
    private String reviewComments;
    private LocalDateTime reviewedAt;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private String rejectionReason;
    private LocalDateTime createdAt;
}

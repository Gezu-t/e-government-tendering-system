package com.egov.tendering.user.dal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorQualificationRequest {

    @NotNull(message = "Organization ID is required")
    private Long organizationId;

    @NotBlank(message = "Qualification category is required")
    @Size(max = 100)
    private String qualificationCategory;

    private String businessLicenseNumber;

    private String taxRegistrationNumber;

    private Integer yearsOfExperience;

    private String annualRevenue;

    private Integer employeeCount;

    private Integer pastContractsCount;

    private String certificationDetails;
}

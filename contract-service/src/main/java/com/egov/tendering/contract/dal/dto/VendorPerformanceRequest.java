package com.egov.tendering.contract.dal.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorPerformanceRequest {

    @NotNull @DecimalMin("0.0") @DecimalMax("10.0")
    private BigDecimal qualityScore;

    @NotNull @DecimalMin("0.0") @DecimalMax("10.0")
    private BigDecimal timelinessScore;

    @NotNull @DecimalMin("0.0") @DecimalMax("10.0")
    private BigDecimal complianceScore;

    @NotNull @DecimalMin("0.0") @DecimalMax("10.0")
    private BigDecimal communicationScore;

    private String reviewComments;

    @NotBlank
    private String reviewPeriod;
}

package com.egov.tendering.contract.dal.dto;

import com.egov.tendering.contract.dal.model.ContractAmendment.AmendmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractAmendmentRequest {

    @NotNull(message = "Amendment type is required")
    private AmendmentType type;

    @NotBlank(message = "Reason is required")
    private String reason;

    private String description;

    private BigDecimal newValue;

    private LocalDate newEndDate;
}

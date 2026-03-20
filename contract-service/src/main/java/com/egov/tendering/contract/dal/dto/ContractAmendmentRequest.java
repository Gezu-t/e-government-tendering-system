package com.egov.tendering.contract.dal.dto;

import com.egov.tendering.contract.dal.model.ContractAmendment.AmendmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    @Size(max = 1000, message = "Reason cannot exceed 1000 characters")
    private String reason;

    @Size(max = 4000, message = "Description cannot exceed 4000 characters")
    private String description;

    private BigDecimal newValue;

    private LocalDate newEndDate;
}

package com.egov.tendering.contract.dal.dto;

import com.egov.tendering.contract.dal.model.ContractAmendment.AmendmentStatus;
import com.egov.tendering.contract.dal.model.ContractAmendment.AmendmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractAmendmentDTO {
    private Long id;
    private Long contractId;
    private Integer amendmentNumber;
    private AmendmentType type;
    private String reason;
    private String description;
    private BigDecimal previousValue;
    private BigDecimal newValue;
    private LocalDate previousEndDate;
    private LocalDate newEndDate;
    private AmendmentStatus status;
    private Long requestedBy;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
}

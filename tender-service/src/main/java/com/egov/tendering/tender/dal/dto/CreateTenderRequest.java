package com.egov.tendering.tender.dal.dto;

import com.egov.tendering.tender.dal.model.AllocationStrategy;
import com.egov.tendering.tender.dal.model.TenderType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTenderRequest {
    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;

    @Size(max = 5000, message = "Description cannot exceed 5000 characters")
    private String description;

    @NotNull(message = "Tender type is required")
    private TenderType type;

    @NotNull(message = "Submission deadline is required")
    @Future(message = "Submission deadline must be in the future")
    private LocalDateTime submissionDeadline;

    @NotNull(message = "Allocation strategy is required")
    private AllocationStrategy allocationStrategy;

    @Min(value = 1, message = "Minimum winners must be at least 1")
    private Integer minWinners;

    @Min(value = 1, message = "Maximum winners must be at least 1")
    private Integer maxWinners;

    private BigDecimal cutoffScore;

    private Boolean isAverageAllocation;

    @NotEmpty(message = "At least one criterion is required")
    @Valid
    private List<TenderCriteriaRequest> criteria;

    @NotEmpty(message = "At least one item is required")
    @Valid
    private List<TenderItemRequest> items;
}
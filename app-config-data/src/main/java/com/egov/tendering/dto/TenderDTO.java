package com.egov.tendering.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for tender data from the tender service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenderDTO {
    private Long id;
    private String title;
    private String description;
    private String status;
    private LocalDateTime submissionDeadline;
    private LocalDateTime evaluationDeadline;
    private AllocationStrategy allocationStrategy;
    private BigDecimal cutoffScore;
    private Boolean isAverageAllocation;
    private List<TenderItemDTO> items;
    private List<TenderCriteriaDTO> criteria;
}

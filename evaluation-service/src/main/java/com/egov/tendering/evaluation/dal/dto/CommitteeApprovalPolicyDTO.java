package com.egov.tendering.evaluation.dal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommitteeApprovalPolicyDTO {

    private Long id;
    private Long tenderId;
    private Integer requiredReviewCount;
    private Integer minimumApprovalCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

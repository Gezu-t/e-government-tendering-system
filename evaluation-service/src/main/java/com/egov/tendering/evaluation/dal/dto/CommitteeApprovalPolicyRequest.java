package com.egov.tendering.evaluation.dal.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommitteeApprovalPolicyRequest {

    @NotNull
    @Min(1)
    private Integer requiredReviewCount;

    @NotNull
    @Min(1)
    private Integer minimumApprovalCount;
}

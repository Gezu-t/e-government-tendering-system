package com.egov.tendering.user.dal.dto;

import com.egov.tendering.user.dal.model.QualificationStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualificationReviewRequest {

    @NotNull(message = "Status is required")
    private QualificationStatus status;

    private String comments;

    @Min(0) @Max(100)
    private Integer qualificationScore;

    private LocalDate validFrom;

    private LocalDate validUntil;

    private String rejectionReason;
}

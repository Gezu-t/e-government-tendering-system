package com.egov.tendering.tender.dal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenderAmendmentRequest {

    @NotBlank(message = "Amendment reason is required")
    @Size(max = 2000, message = "Reason must not exceed 2000 characters")
    private String reason;

    private String description;

    private LocalDateTime newSubmissionDeadline;
}

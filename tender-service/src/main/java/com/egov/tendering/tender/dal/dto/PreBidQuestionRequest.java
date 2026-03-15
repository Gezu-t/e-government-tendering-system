package com.egov.tendering.tender.dal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreBidQuestionRequest {

    @NotBlank(message = "Question is required")
    @Size(max = 5000, message = "Question must not exceed 5000 characters")
    private String question;

    @Size(max = 100)
    private String category;

    private String organizationName;
}

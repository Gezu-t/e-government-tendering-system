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
public class PreBidAnswerRequest {

    @NotBlank(message = "Answer is required")
    @Size(max = 10000, message = "Answer must not exceed 10000 characters")
    private String answer;

    private Boolean makePublic;
}

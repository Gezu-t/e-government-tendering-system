package com.egov.tendering.tender.dal.dto;

import com.egov.tendering.tender.dal.model.PreBidClarification.ClarificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreBidClarificationDTO {
    private Long id;
    private Long tenderId;
    private String question;
    private String answer;
    private Long askedBy;
    private String askedByOrgName;
    private Long answeredBy;
    private String category;
    private Boolean isPublic;
    private ClarificationStatus status;
    private LocalDateTime askedAt;
    private LocalDateTime answeredAt;
}

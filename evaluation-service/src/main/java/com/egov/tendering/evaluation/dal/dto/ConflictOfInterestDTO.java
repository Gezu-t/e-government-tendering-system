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
public class ConflictOfInterestDTO {
    private Long id;
    private Long tenderId;
    private Long evaluatorId;
    private Boolean hasConflict;
    private String conflictDescription;
    private Long relatedOrganizationId;
    private String relationshipType;
    private Boolean acknowledged;
    private String reviewDecision;
    private String reviewComments;
    private LocalDateTime declaredAt;
}

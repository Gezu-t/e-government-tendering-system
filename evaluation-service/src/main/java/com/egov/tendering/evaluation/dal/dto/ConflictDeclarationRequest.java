package com.egov.tendering.evaluation.dal.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConflictDeclarationRequest {

    @NotNull(message = "Conflict status is required")
    private Boolean hasConflict;

    private String conflictDescription;

    private Long relatedOrganizationId;

    private String relationshipType;

    private String declarationText;
}

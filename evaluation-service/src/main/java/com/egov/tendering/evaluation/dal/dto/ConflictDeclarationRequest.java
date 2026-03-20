package com.egov.tendering.evaluation.dal.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @Size(max = 2000, message = "Conflict description cannot exceed 2000 characters")
    private String conflictDescription;

    private Long relatedOrganizationId;

    @Size(max = 200, message = "Relationship type cannot exceed 200 characters")
    private String relationshipType;

    @Size(max = 4000, message = "Declaration text cannot exceed 4000 characters")
    private String declarationText;
}

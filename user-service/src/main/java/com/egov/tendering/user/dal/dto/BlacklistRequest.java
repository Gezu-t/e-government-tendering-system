package com.egov.tendering.user.dal.dto;

import com.egov.tendering.user.dal.model.OrganizationBlacklist.BlacklistType;
import jakarta.validation.constraints.NotBlank;
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
public class BlacklistRequest {

    @NotNull(message = "Organization ID is required")
    private Long organizationId;

    @NotNull(message = "Blacklist type is required")
    private BlacklistType type;

    @NotBlank(message = "Reason is required")
    private String reason;

    private String referenceNumber;

    @NotNull(message = "Effective from date is required")
    private LocalDate effectiveFrom;

    private LocalDate effectiveUntil;

    private Boolean isPermanent;
}

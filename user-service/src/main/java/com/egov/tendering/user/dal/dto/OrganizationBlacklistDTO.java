package com.egov.tendering.user.dal.dto;

import com.egov.tendering.user.dal.model.OrganizationBlacklist.BlacklistType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationBlacklistDTO {
    private Long id;
    private Long organizationId;
    private String organizationName;
    private BlacklistType type;
    private String reason;
    private String referenceNumber;
    private LocalDate effectiveFrom;
    private LocalDate effectiveUntil;
    private Boolean isPermanent;
    private Long imposedBy;
    private Boolean active;
    private LocalDateTime createdAt;
}

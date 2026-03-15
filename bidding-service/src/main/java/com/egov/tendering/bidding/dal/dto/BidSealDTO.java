package com.egov.tendering.bidding.dal.dto;

import com.egov.tendering.bidding.dal.model.SealStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidSealDTO {
    private Long id;
    private Long bidId;
    private Long tenderId;
    private String contentHash;
    private String encryptionAlgorithm;
    private SealStatus status;
    private LocalDateTime sealedAt;
    private Long sealedBy;
    private LocalDateTime unsealedAt;
    private Long unsealedBy;
    private LocalDateTime scheduledUnsealTime;
    private Boolean integrityVerified;
}

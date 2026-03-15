package com.egov.tendering.bidding.dal.dto;

import com.egov.tendering.bidding.dal.model.SignatureStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DigitalSignatureDTO {
    private Long id;
    private String entityType;
    private Long entityId;
    private Long signerId;
    private String signerName;
    private String contentHash;
    private String algorithm;
    private String certificateSerial;
    private SignatureStatus status;
    private LocalDateTime signedAt;
    private LocalDateTime verifiedAt;
    private Long verifiedBy;
    private String rejectionReason;
}

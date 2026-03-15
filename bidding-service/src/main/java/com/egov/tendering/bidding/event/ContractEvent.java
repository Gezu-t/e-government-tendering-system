package com.egov.tendering.bidding.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractEvent {
    private Long eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private Long contractId;
    private String contractReference;
    private Long tenderId;
    private Long bidId;
    private Long bidderId;
    private Double contractValue;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String contractStatus;
}

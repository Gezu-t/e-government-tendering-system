package com.egov.tendering.bidding.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenderRankingEventData {
    private Long id;
    private Long tenderId;
    private Long bidId;
    private String bidderName;
    private BigDecimal finalScore;
    private Integer rank;
    private Boolean isWinner;
}

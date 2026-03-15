package com.egov.tendering.bidding.dal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AntiCollusionReport {
    private Long tenderId;
    private int totalBids;
    private int flaggedBids;
    private List<CollusionFlag> flags;
    private boolean collusionSuspected;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollusionFlag {
        private String type;
        private String description;
        private List<Long> involvedBidIds;
        private List<Long> involvedTendererIds;
        private String severity;
    }
}

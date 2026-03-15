package com.egov.tendering.bidding.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenderEvaluationCompletedEvent {
    private Long tenderId;
    private String tenderTitle;
    private List<TenderRankingEventData> rankings;
    private List<AllocationResultEventData> allocations;
    private LocalDateTime timestamp;
}

package com.egov.tendering.audit.dal.dto;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * Bid statistics for a given date range.
 */
@Value
@Builder
public class BidStatisticsDto {

    String period;

    /** Total distinct bid IDs ever seen in audit logs */
    long totalBids;

    /** Bids submitted during the period */
    long submittedThisPeriod;

    /** Bids flagged for collusion / tampering / rejection */
    long flaggedBids;

    /**
     * Average number of bids per tender, computed as
     * totalBids / totalDistinctTenders (0 if no tenders exist).
     */
    double averageBidsPerTender;

    /** Count of bids grouped by their last known lifecycle status */
    Map<String, Long> byStatus;

    /** Count of bids evaluated (received an EVALUATED action) during the period */
    long evaluatedThisPeriod;

    /** Count of bids withdrawn during the period */
    long withdrawnThisPeriod;
}

package com.egov.tendering.audit.dal.dto;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * Tender breakdown report for a given date range.
 */
@Value
@Builder
public class TenderStatusReportDto {

    String period;

    /** Total distinct tender IDs ever seen in audit logs */
    long totalDistinctTenders;

    /** Count of tenders grouped by their last known lifecycle status */
    Map<String, Long> byStatus;

    /** Tenders that received a PUBLISHED event during the period */
    long publishedThisPeriod;

    /** Tenders that received a CLOSED event during the period */
    long closedThisPeriod;

    /** Tenders that received an AWARDED event during the period */
    long awardedThisPeriod;

    /** Tenders that received an AMENDED event during the period */
    long amendedThisPeriod;
}

package com.egov.tendering.audit.dal.dto;

import lombok.Builder;
import lombok.Value;

/**
 * Compact summary data for dashboard widget cards.
 * All counts are derived from audit log entries.
 */
@Value
@Builder
public class DashboardWidgetsDto {

    /** Distinct tenders whose last known status is PUBLISHED or ACTIVE */
    long activeTenders;

    /** Distinct bids whose last known status is SUBMITTED (awaiting evaluation) */
    long pendingBids;

    /** Distinct contracts whose last known status is ACTIVE */
    long activeContracts;

    /** Security/collusion audit events recorded today */
    long auditAlertsToday;

    /** Tenders published within the current calendar month */
    long tendersPublishedThisMonth;

    /** Bids submitted within the current calendar month */
    long bidsSubmittedThisMonth;

    /** Contracts awarded within the current calendar month */
    long contractsAwardedThisMonth;

    /** Total audit entries recorded today */
    long auditEntriesCreatedToday;
}

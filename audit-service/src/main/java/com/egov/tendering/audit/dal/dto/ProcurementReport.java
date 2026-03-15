package com.egov.tendering.audit.dal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcurementReport {
    private LocalDate reportDate;
    private String reportPeriod;
    private TenderSummary tenderSummary;
    private BidSummary bidSummary;
    private ContractSummary contractSummary;
    private AuditSummary auditSummary;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TenderSummary {
        private long totalTenders;
        private Map<String, Long> tendersByStatus;
        private Map<String, Long> tendersByType;
        private long tendersPublishedThisPeriod;
        private long tendersClosedThisPeriod;
        private long tendersAwardedThisPeriod;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BidSummary {
        private long totalBids;
        private Map<String, Long> bidsByStatus;
        private double averageBidsPerTender;
        private long bidsSubmittedThisPeriod;
        private long flaggedBids;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ContractSummary {
        private long totalContracts;
        private Map<String, Long> contractsByStatus;
        private long activeContracts;
        private long completedContracts;
        private long overdueMilestones;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AuditSummary {
        private long totalAuditEntries;
        private Map<String, Long> entriesByAction;
        private Map<String, Long> entriesByModule;
        private long entriesThisPeriod;
    }
}

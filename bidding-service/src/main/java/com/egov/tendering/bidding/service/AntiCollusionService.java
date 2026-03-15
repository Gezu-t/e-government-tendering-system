package com.egov.tendering.bidding.service;

import com.egov.tendering.bidding.dal.dto.AntiCollusionReport;

public interface AntiCollusionService {

    /**
     * Records metadata about a bid submission for anti-collusion analysis.
     */
    void recordSubmissionMetadata(Long bidId, Long tenderId, Long tendererId,
                                  String ipAddress, String userAgent,
                                  String deviceFingerprint, String sessionId);

    /**
     * Analyzes all bids for a tender and generates a collusion detection report.
     * Checks for: same IP, same device, suspicious pricing patterns, timing anomalies.
     */
    AntiCollusionReport analyzeForCollusion(Long tenderId);

    /**
     * Flags a specific bid as suspicious with a reason.
     */
    void flagBid(Long bidId, String reason);
}

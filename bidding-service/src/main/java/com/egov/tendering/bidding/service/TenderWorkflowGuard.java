package com.egov.tendering.bidding.service;

import com.egov.tendering.bidding.client.TenderClient;
import com.egov.tendering.bidding.exception.ResourceNotFoundException;
import com.egov.tendering.dto.TenderDTO;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class TenderWorkflowGuard {

    private static final Set<String> OPEN_FOR_BIDDING_STATUSES = Set.of("PUBLISHED", "AMENDED");

    private final TenderClient tenderClient;

    public TenderDTO requireTender(Long tenderId) {
        try {
            return tenderClient.getTenderById(tenderId);
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Tender", "id", tenderId);
        } catch (FeignException ex) {
            throw new IllegalStateException("Unable to validate tender workflow for tender " + tenderId, ex);
        }
    }

    public TenderDTO validateOpenForBidSubmission(Long tenderId, LocalDateTime now) {
        TenderDTO tender = requireTender(tenderId);
        String status = tender.getStatus();
        if (status == null || !OPEN_FOR_BIDDING_STATUSES.contains(status.toUpperCase())) {
            throw new IllegalStateException("Tender " + tenderId + " is not open for bid submissions");
        }
        LocalDateTime submissionDeadline = tender.getSubmissionDeadline();
        if (submissionDeadline == null) {
            throw new IllegalStateException("Tender " + tenderId + " does not define a submission deadline");
        }
        if (!now.isBefore(submissionDeadline)) {
            throw new IllegalStateException("Tender " + tenderId + " is no longer accepting bids");
        }
        return tender;
    }

    public LocalDateTime getSubmissionDeadline(Long tenderId) {
        LocalDateTime submissionDeadline = requireTender(tenderId).getSubmissionDeadline();
        if (submissionDeadline == null) {
            throw new IllegalStateException("Tender " + tenderId + " does not define a submission deadline");
        }
        return submissionDeadline;
    }
}

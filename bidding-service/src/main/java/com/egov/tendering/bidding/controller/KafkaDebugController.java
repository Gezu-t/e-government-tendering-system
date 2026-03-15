package com.egov.tendering.bidding.controller;


import com.egov.tendering.bidding.dal.model.Bid;
import com.egov.tendering.bidding.dal.model.BidStatus;
import com.egov.tendering.bidding.dal.repository.BidRepository;
import com.egov.tendering.bidding.event.BidCreatedEvent;
import com.egov.tendering.bidding.event.BidEvent;
import com.egov.tendering.bidding.event.BidSubmittedEvent;
import com.egov.tendering.bidding.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/debug/kafka")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Kafka Debug", description = "Debug APIs for testing Kafka event publishing")
public class KafkaDebugController {

    private final KafkaTemplate<String, BidEvent> kafkaTemplate;
    private final BidRepository bidRepository;

    @Value("${app.kafka.topics.bid-events}")
    private String bidEventsTopic;

    @PostMapping("/bids/{bidId}/publish-created")
    @Operation(summary = "Publish a BID_CREATED event for testing",
            description = "Manually publishes a BID_CREATED event for the specified bid")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> publishBidCreatedEvent(
            @PathVariable @Parameter(description = "ID of the bid") Long bidId) {

        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new ResourceNotFoundException("Bid", "id", bidId));

        BidCreatedEvent event = BidCreatedEvent.builder()
                .eventId(System.currentTimeMillis())
                .eventType("BID_CREATED")
                .timestamp(LocalDateTime.now())
                .bidId(bid.getId())
                .tenderId(bid.getTenderId())
                .tendererId(bid.getTendererId())
                .totalPrice(bid.getTotalPrice())
                .itemCount(bid.getItems().size())
                .build();

        log.info("Manually publishing BID_CREATED event: {}", event);
        kafkaTemplate.send(bidEventsTopic, bid.getId().toString(), event);

        return ResponseEntity.ok("BID_CREATED event published for bid ID: " + bidId);
    }

    @PostMapping("/bids/{bidId}/publish-submitted")
    @Operation(summary = "Publish a BID_SUBMITTED event for testing",
            description = "Manually publishes a BID_SUBMITTED event for the specified bid")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> publishBidSubmittedEvent(
            @PathVariable @Parameter(description = "ID of the bid") Long bidId) {

        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new ResourceNotFoundException("Bid", "id", bidId));

        // Update bid status if not already submitted
        if (bid.getStatus() != BidStatus.SUBMITTED) {
            bid.setStatus(BidStatus.SUBMITTED);
            bid.setSubmissionTime(LocalDateTime.now());
            bidRepository.save(bid);
        }

        BidSubmittedEvent event = BidSubmittedEvent.builder()
                .eventId(System.currentTimeMillis())
                .eventType("BID_SUBMITTED")
                .timestamp(LocalDateTime.now())
                .bidId(bid.getId())
                .tenderId(bid.getTenderId())
                .tendererId(bid.getTendererId())
                .totalPrice(bid.getTotalPrice())
                .submissionTime(bid.getSubmissionTime())
                .build();

        log.info("Manually publishing BID_SUBMITTED event: {}", event);
        kafkaTemplate.send(bidEventsTopic, bid.getId().toString(), event);

        return ResponseEntity.ok("BID_SUBMITTED event published for bid ID: " + bidId);
    }
}
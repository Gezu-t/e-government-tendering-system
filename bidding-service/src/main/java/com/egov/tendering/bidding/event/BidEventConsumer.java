package com.egov.tendering.bidding.event;

import com.egov.tendering.bidding.dal.dto.BidDTO;
import com.egov.tendering.bidding.dal.model.BidStatus;
import com.egov.tendering.bidding.service.BidService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BidEventConsumer {

    private final BidService bidService;

    /**
     * Listen for tender events to take appropriate actions on bids
     */
    @KafkaListener(topics = "${app.kafka.topics.tender-events:tender-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeTenderEvents(@Payload TenderEvent event, Acknowledgment ack) {
        try {
            log.info("Received tender event: {}", event);

            switch (event.getEventType()) {
                case "TENDER_PUBLISHED" -> {
                    log.info("Processing tender published event for tender ID: {}", event.getTenderId());
                    // Potential logic to prepare for new bids for this tender
                }

                case "TENDER_CLOSED" -> {
                    log.info("Processing tender closed event for tender ID: {}", event.getTenderId());
                    bidService.closeBidsForTender(event.getTenderId());
                }

                case "TENDER_CANCELLED" -> {
                    log.info("Processing tender cancelled event for tender ID: {}", event.getTenderId());
                    bidService.cancelBidsForTender(event.getTenderId(), "Tender was cancelled");
                }

                default ->
                        log.warn("Unknown tender event type: {}", event.getEventType());
            }

            // Acknowledge successful processing
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing tender event: {}", event, e);
            // Do not acknowledge - message will be redelivered
        }
    }

    /**
     * Listen for completed tender evaluation events and reflect them in bid state.
     */
    @KafkaListener(topics = "${app.kafka.topics.tender-evaluation-completed:tender-evaluation-completed}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeTenderEvaluationCompleted(@Payload TenderEvaluationCompletedEvent event, Acknowledgment ack) {
        try {
            log.info("Received tender evaluation completed event for tender ID: {}", event.getTenderId());

            for (TenderRankingEventData ranking : safeList(event.getRankings())) {
                BidDTO bid = bidService.getBidById(ranking.getBidId());
                if (bid.getStatus() == BidStatus.SUBMITTED || bid.getStatus() == BidStatus.UNDER_EVALUATION) {
                    bidService.updateBidEvaluationStatus(
                            ranking.getBidId(),
                            "PASS",
                            null,
                            "Tender evaluation completed"
                    );
                }
            }

            safeList(event.getAllocations()).stream()
                    .map(AllocationResultEventData::getBidId)
                    .filter(bidId -> bidId != null)
                    .distinct()
                    .forEach(bidId -> {
                        BidDTO bid = bidService.getBidById(bidId);
                        if (bid.getStatus() == BidStatus.EVALUATED) {
                            bidService.awardBid(bidId, null, "Awarded based on tender evaluation results");
                        }
                    });

            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing tender evaluation completed event: {}", event, e);
        }
    }

    /**
     * Listen for contract events to update bid statuses
     */
    @KafkaListener(topics = "${app.kafka.topics.contract-events:contract-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeContractEvents(@Payload ContractEvent event, Acknowledgment ack) {
        try {
            log.info("Received contract event: {}", event);

            if ("CONTRACT_CREATED".equals(event.getEventType())) {
                if (event.getBidId() != null) {
                    log.info("Processing contract created event for bid ID: {}", event.getBidId());
                    bidService.updateBidContractStatus(event.getBidId(), event.getContractId());
                } else if (event.getTenderId() != null && event.getBidderId() != null) {
                    log.info("Processing contract created event for tender ID: {} and bidder ID: {}",
                            event.getTenderId(), event.getBidderId());
                    bidService.updateBidContractStatusByTenderAndTenderer(
                            event.getTenderId(), event.getBidderId(), event.getContractId());
                } else {
                    log.warn("Contract created event is missing both bidId and tenderId/bidderId: {}", event);
                }
            } else {
                log.warn("Unknown contract event type: {}", event.getEventType());
            }

            // Acknowledge successful processing
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing contract event: {}", event, e);
            // Do not acknowledge - message will be redelivered
        }
    }

    private <T> List<T> safeList(List<T> values) {
        return values != null ? values : List.of();
    }
}

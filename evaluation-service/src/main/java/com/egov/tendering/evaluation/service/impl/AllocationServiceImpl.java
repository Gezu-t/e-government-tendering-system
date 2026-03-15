package com.egov.tendering.evaluation.service.impl;

import com.egov.tendering.dto.BidDTO;
import com.egov.tendering.dto.BidItemDTO;
import com.egov.tendering.dto.TenderDTO;
import com.egov.tendering.dto.TenderItemDTO;
import com.egov.tendering.evaluation.client.BidClient;
import com.egov.tendering.evaluation.client.TenderClient;
import com.egov.tendering.evaluation.dal.dto.AllocationResultDTO;
import com.egov.tendering.evaluation.dal.dto.TenderRankingDTO;
import com.egov.tendering.evaluation.dal.mapper.AllocationResultMapper;
import com.egov.tendering.evaluation.dal.model.AllocationResult;
import com.egov.tendering.evaluation.dal.model.Evaluation;
import com.egov.tendering.evaluation.dal.model.TenderRanking;
import com.egov.tendering.evaluation.dal.repository.AllocationResultRepository;
import com.egov.tendering.evaluation.dal.repository.TenderRankingRepository;
import com.egov.tendering.evaluation.exception.AllocationException;
import com.egov.tendering.evaluation.service.AllocationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementation of the AllocationService.
 * Handles the three contract selection strategies described in the paper:
 * 1. Single Winner
 * 2. Cooperative Allocation
 * 3. Competitive Allocation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AllocationServiceImpl implements AllocationService {

    private final AllocationResultRepository allocationRepository;
    private final TenderRankingRepository rankingRepository;
    private final AllocationResultMapper allocationMapper;
    private final TenderClient tenderClient;
    private final BidClient bidClient;

    /**
     * Allocates all items to a single winner with the highest score.
     * In case of a tie, one winner is selected randomly.
     */
    @Override
    @Transactional
    public List<AllocationResultDTO> allocateSingleWinner(Long tenderId, List<TenderRankingDTO> rankings) {
        log.info("Allocating single winner for tender ID: {}", tenderId);

        // Clear existing allocations
        clearAllocations(tenderId);

        if (rankings.isEmpty()) {
            log.warn("No rankings available for tender ID: {}", tenderId);
            return Collections.emptyList();
        }

        // Get top-ranked bid
        TenderRankingDTO topRanking = rankings.stream()
                .min(Comparator.comparing(TenderRankingDTO::getRank))
                .orElseThrow(() -> new AllocationException("No rankings available for tender ID: " + tenderId));

        log.info("Selected winner for tender ID: {} is bid ID: {}", tenderId, topRanking.getBidId());

        // Mark as winner in rankings
        TenderRanking winnerRanking = rankingRepository.findByTenderIdAndBidId(tenderId, topRanking.getBidId())
                .orElseThrow(() -> new AllocationException("Ranking not found for bid ID: " + topRanking.getBidId()));
        winnerRanking.setIsWinner(true);
        rankingRepository.save(winnerRanking);

        // Get tender and bid details
        TenderDTO tenderDTO = tenderClient.getTenderById(tenderId);
        BidDTO bidDTO = bidClient.getBidById(topRanking.getBidId());

        // Map of bid items by criteria ID
        Map<Long, BidItemDTO> bidItemsByCriteriaId = bidDTO.getItems().stream()
                .collect(Collectors.toMap(BidItemDTO::getCriteriaId, Function.identity()));

        // Allocate all items to the winner
        List<AllocationResult> allocations = new ArrayList<>();

        for (TenderItemDTO item : tenderDTO.getItems()) {
            BidItemDTO bidItem = bidItemsByCriteriaId.get(item.getCriteriaId());

            if (bidItem != null) {
                AllocationResult allocation = new AllocationResult();
                allocation.setTenderId(tenderId);
                allocation.setBidId(topRanking.getBidId());
                allocation.setItemId(item.getId());
                allocation.setQuantity(item.getQuantity());
                allocation.setUnitPrice(bidItem.getValue());
                allocation.setTotalPrice(bidItem.getValue().multiply(BigDecimal.valueOf(item.getQuantity())));

                allocations.add(allocation);
            } else {
                log.warn("No bid item found for criteria ID: {} in bid ID: {}",
                        item.getCriteriaId(), topRanking.getBidId());
            }
        }

        allocations = allocationRepository.saveAll(allocations);

        List<AllocationResultDTO> allocationDTOs = allocationMapper.toDtoList(allocations);

        return enrichAllocationDTOs(allocationDTOs);
    }

    /**
     * Implements cooperative allocation where for each price item,
     * the supplier with the highest score for that item is selected.
     * This allows specialization where different suppliers can be selected
     * for different items based on their expertise.
     */
    @Override
    @Transactional
    public List<AllocationResultDTO> allocateCooperative(Long tenderId, List<Evaluation> evaluations) {
        log.info("Allocating cooperatively for tender ID: {}", tenderId);

        // Clear existing allocations
        clearAllocations(tenderId);

        if (evaluations.isEmpty()) {
            log.warn("No evaluations available for tender ID: {}", tenderId);
            return Collections.emptyList();
        }

        TenderDTO tenderDTO = tenderClient.getTenderById(tenderId);

        // Map of criteria scores by bid ID and criteria ID
        Map<Long, Map<Long, List<BigDecimal>>> scoresByBidAndCriteria = new HashMap<>();

        // Organize scores by bid and criteria
        for (Evaluation evaluation : evaluations) {
            evaluation.getCriteriaScores().forEach(score -> {
                scoresByBidAndCriteria
                        .computeIfAbsent(evaluation.getBidId(), k -> new HashMap<>())
                        .computeIfAbsent(score.getCriteriaId(), k -> new ArrayList<>())
                        .add(score.getScore());
            });
        }

        // Calculate average score for each bid and criteria
        Map<Long, Map<Long, BigDecimal>> avgScoresByBidAndCriteria = new HashMap<>();

        scoresByBidAndCriteria.forEach((bidId, criteriaScores) -> {
            Map<Long, BigDecimal> avgScores = new HashMap<>();

            criteriaScores.forEach((criteriaId, scores) -> {
                if (!scores.isEmpty()) {
                    BigDecimal sum = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal avgScore = sum.divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
                    avgScores.put(criteriaId, avgScore);
                }
            });

            avgScoresByBidAndCriteria.put(bidId, avgScores);
        });

        // List to store allocations
        List<AllocationResult> allocations = new ArrayList<>();

        // For each tender item, find the best bidder for its criteria
        for (TenderItemDTO item : tenderDTO.getItems()) {
            Long criteriaId = item.getCriteriaId();

            // Find all bids with max score for this criteria
            BigDecimal maxScore = BigDecimal.ZERO;
            List<Long> maxScoreBids = new ArrayList<>();

            for (Map.Entry<Long, Map<Long, BigDecimal>> entry : avgScoresByBidAndCriteria.entrySet()) {
                Long bidId = entry.getKey();
                Map<Long, BigDecimal> avgScores = entry.getValue();

                if (avgScores.containsKey(criteriaId)) {
                    BigDecimal score = avgScores.get(criteriaId);

                    if (score.compareTo(maxScore) > 0) {
                        maxScore = score;
                        maxScoreBids.clear();
                        maxScoreBids.add(bidId);
                    } else if (score.compareTo(maxScore) == 0) {
                        maxScoreBids.add(bidId);
                    }
                }
            }

            // Randomly select one winner if there are multiple with max score
            Long bestBidId = null;
            if (!maxScoreBids.isEmpty()) {
                if (maxScoreBids.size() == 1) {
                    bestBidId = maxScoreBids.get(0);
                } else {
                    // Random selection for tie-breaking
                    int randomIndex = new Random().nextInt(maxScoreBids.size());
                    bestBidId = maxScoreBids.get(randomIndex);
                    log.info("Multiple bids with max score for criteria {}. Randomly selected bid: {}",
                            criteriaId, bestBidId);
                }
            } else {
                log.warn("No eligible bids found for item with criteria ID: {}", criteriaId);
                continue;
            }

            try {
                // Get the bid and its item
                BidDTO bid = bidClient.getBidById(bestBidId);
                Optional<BidItemDTO> bidItem = bid.getItems().stream()
                        .filter(bi -> bi.getCriteriaId().equals(criteriaId))
                        .findFirst();

                if (bidItem.isPresent()) {
                    // Mark as winner in rankings
                    Optional<TenderRanking> rankingOpt = rankingRepository.findByTenderIdAndBidId(tenderId, bestBidId);
                    if (rankingOpt.isPresent()) {
                        TenderRanking ranking = rankingOpt.get();
                        ranking.setIsWinner(true);
                        rankingRepository.save(ranking);
                    } else {
                        log.warn("No ranking found for tender ID: {} and bid ID: {}", tenderId, bestBidId);
                    }

                    // Create allocation
                    AllocationResult allocation = new AllocationResult();
                    allocation.setTenderId(tenderId);
                    allocation.setBidId(bestBidId);
                    allocation.setItemId(item.getId());
                    allocation.setQuantity(item.getQuantity());
                    allocation.setUnitPrice(bidItem.get().getValue());
                    allocation.setTotalPrice(bidItem.get().getValue().multiply(
                            BigDecimal.valueOf(item.getQuantity())));

                    allocations.add(allocation);
                } else {
                    log.warn("No bid item found for criteria ID: {} in bid ID: {}", criteriaId, bestBidId);
                }
            } catch (Exception e) {
                log.error("Error retrieving bid information for bid ID: {}", bestBidId, e);
            }
        }

        if (allocations.isEmpty()) {
            log.warn("No allocations could be made for tender ID: {}", tenderId);
            return Collections.emptyList();
        }

        allocations = allocationRepository.saveAll(allocations);

        List<AllocationResultDTO> allocationDTOs = allocationMapper.toDtoList(allocations);

        return enrichAllocationDTOs(allocationDTOs);
    }

    /**
     * Implements competitive allocation where multiple winners are selected
     * based on a cut-off score and items are allocated among them.
     * This prevents monopolization by a single supplier.
     */
    @Override
    @Transactional
    public List<AllocationResultDTO> allocateCompetitive(
            Long tenderId,
            List<TenderRankingDTO> rankings,
            BigDecimal cutoffScore,
            boolean isAverageAllocation) {

        log.info("Allocating competitively for tender ID: {}", tenderId);

        // Clear existing allocations
        clearAllocations(tenderId);

        if (rankings.isEmpty()) {
            log.warn("No rankings available for tender ID: {}", tenderId);
            return Collections.emptyList();
        }

        // Get tender details
        TenderDTO tenderDTO = tenderClient.getTenderById(tenderId);

        // Filter bids above cutoff score (higher scores are better)
        List<TenderRankingDTO> eligibleRankings = rankings.stream()
                .filter(r -> r.getFinalScore().compareTo(cutoffScore) >= 0)
                .collect(Collectors.toList());

        if (eligibleRankings.isEmpty()) {
            log.warn("No bids meet the cutoff score: {} for tender ID: {}", cutoffScore, tenderId);
            return Collections.emptyList();
        }

        log.info("Selected {} winners for competitive allocation with cutoff score: {}",
                eligibleRankings.size(), cutoffScore);

        // Mark winners in rankings
        for (TenderRankingDTO ranking : eligibleRankings) {
            TenderRanking winnerRanking = rankingRepository.findByTenderIdAndBidId(tenderId, ranking.getBidId())
                    .orElseThrow(() -> new AllocationException("Ranking not found for bid ID: " + ranking.getBidId()));
            winnerRanking.setIsWinner(true);
            rankingRepository.save(winnerRanking);
        }

        // Calculate composite bids (weighted sum of prices)
        Map<Long, BigDecimal> compositeBids = new HashMap<>();
        Map<Long, BidDTO> bidsByBidId = new HashMap<>();

        for (TenderRankingDTO ranking : eligibleRankings) {
            try {
                BidDTO bid = bidClient.getBidById(ranking.getBidId());
                bidsByBidId.put(ranking.getBidId(), bid);

                BigDecimal compositeBid = calculateCompositeBid(bid, tenderDTO);
                // Handle zero composite bid by using a very small value instead
                compositeBid = compositeBid.compareTo(BigDecimal.ZERO) <= 0
                        ? new BigDecimal("0.0001")
                        : compositeBid;
                compositeBids.put(ranking.getBidId(), compositeBid);
            } catch (Exception e) {
                log.error("Error retrieving bid information for bid ID: {}", ranking.getBidId(), e);
                // Skip this bid if we can't retrieve it
                continue;
            }
        }

        if (compositeBids.isEmpty()) {
            log.warn("No valid composite bids calculated for tender ID: {}", tenderId);
            return Collections.emptyList();
        }

        // Find min and max composite bid values
        BigDecimal minCompositeBid = compositeBids.values().stream()
                .min(Comparator.naturalOrder())
                .orElse(new BigDecimal("0.0001"));  // Ensure non-zero minimum

        BigDecimal maxCompositeBid = compositeBids.values().stream()
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ONE);  // Default to 1 if no max found

        // Calculate adjustment rates (cutoff_bid / composite_bid)
        // Using the cutoff score as the normalized value (or minCompositeBid if cutoffScore is 0)
        BigDecimal effectiveCutoff = cutoffScore.compareTo(BigDecimal.ZERO) <= 0
                ? minCompositeBid
                : cutoffScore;

        Map<Long, BigDecimal> adjustmentRates = new HashMap<>();

        for (TenderRankingDTO ranking : eligibleRankings) {
            if (compositeBids.containsKey(ranking.getBidId())) {
                BigDecimal compositeBid = compositeBids.get(ranking.getBidId());
                BigDecimal adjustmentRate = effectiveCutoff.divide(compositeBid, 4, RoundingMode.HALF_UP);
                adjustmentRates.put(ranking.getBidId(), adjustmentRate);
            }
        }

        // Calculate unified unit prices for each item
        Map<Long, BigDecimal> unifiedUnitPrices = new HashMap<>();

        for (TenderItemDTO item : tenderDTO.getItems()) {
            BigDecimal sumPrice = BigDecimal.ZERO;
            int count = 0;

            for (TenderRankingDTO ranking : eligibleRankings) {
                if (!bidsByBidId.containsKey(ranking.getBidId()) ||
                        !adjustmentRates.containsKey(ranking.getBidId())) {
                    continue;
                }

                BidDTO bid = bidsByBidId.get(ranking.getBidId());
                Optional<BidItemDTO> bidItem = bid.getItems().stream()
                        .filter(bi -> bi.getCriteriaId().equals(item.getCriteriaId()))
                        .findFirst();

                if (bidItem.isPresent()) {
                    BigDecimal adjustmentRate = adjustmentRates.get(ranking.getBidId());
                    BigDecimal adjustedPrice = bidItem.get().getValue().multiply(adjustmentRate);
                    sumPrice = sumPrice.add(adjustedPrice);
                    count++;
                }
            }

            if (count > 0) {
                BigDecimal unitPrice = sumPrice.divide(
                        BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
                unifiedUnitPrices.put(item.getId(), unitPrice);
            } else {
                // If no prices available, use a default unit price based on tender details
                // This could be an average of item prices or a predefined value
                log.warn("No price data available for item ID: {}. Using default price.", item.getId());
                unifiedUnitPrices.put(item.getId(), BigDecimal.ONE); // Default to 1 as a placeholder
            }
        }

        // Allocate quantities
        List<AllocationResult> allocations = new ArrayList<>();

        for (TenderItemDTO item : tenderDTO.getItems()) {
            BigDecimal unitPrice = unifiedUnitPrices.get(item.getId());

            if (unitPrice != null) {
                if (isAverageAllocation) {
                    // Average allocation
                    allocateAverageQuantities(tenderId, item, unitPrice, eligibleRankings, allocations);
                } else {
                    // Proportional allocation based on scores
                    allocateProportionalQuantities(tenderId, item, unitPrice, eligibleRankings, allocations);
                }
            } else {
                log.warn("No unit price calculated for item ID: {}. Skipping allocation.", item.getId());
            }
        }

        if (allocations.isEmpty()) {
            log.warn("No allocations could be made for tender ID: {}", tenderId);
            return Collections.emptyList();
        }

        allocations = allocationRepository.saveAll(allocations);

        List<AllocationResultDTO> allocationDTOs = allocationMapper.toDtoList(allocations);

        return enrichAllocationDTOs(allocationDTOs);
    }

    /**
     * Helper method to allocate quantities using average allocation approach
     */
    private void allocateAverageQuantities(
            Long tenderId,
            TenderItemDTO item,
            BigDecimal unitPrice,
            List<TenderRankingDTO> eligibleRankings,
            List<AllocationResult> allocations) {

        int baseQuantity = item.getQuantity() / eligibleRankings.size();
        int remainder = item.getQuantity() % eligibleRankings.size();

        // Sort by score (descending) for remainder distribution
        List<TenderRankingDTO> sortedRankings = new ArrayList<>(eligibleRankings);
        sortedRankings.sort(Comparator.comparing(TenderRankingDTO::getFinalScore).reversed());

        for (int i = 0; i < sortedRankings.size(); i++) {
            TenderRankingDTO ranking = sortedRankings.get(i);

            // Extra quantity for top-ranked bidders if there's a remainder
            int extraQuantity = (i < remainder) ? 1 : 0;
            int quantity = baseQuantity + extraQuantity;

            if (quantity > 0) {
                AllocationResult allocation = new AllocationResult();
                allocation.setTenderId(tenderId);
                allocation.setBidId(ranking.getBidId());
                allocation.setItemId(item.getId());
                allocation.setQuantity(quantity);
                allocation.setUnitPrice(unitPrice);
                allocation.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(quantity)));

                allocations.add(allocation);
            }
        }
    }

    /**
     * Helper method to allocate quantities proportionally based on scores
     */
    private void allocateProportionalQuantities(
            Long tenderId,
            TenderItemDTO item,
            BigDecimal unitPrice,
            List<TenderRankingDTO> eligibleRankings,
            List<AllocationResult> allocations) {

        BigDecimal totalScore = eligibleRankings.stream()
                .map(TenderRankingDTO::getFinalScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Ensure totalScore is not zero to avoid division by zero
        if (totalScore.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Total score is zero or negative. Using equal allocation instead.");
            allocateAverageQuantities(tenderId, item, unitPrice, eligibleRankings, allocations);
            return;
        }

        // Track total allocated quantity to handle rounding errors
        int totalAllocated = 0;

        // Sort by score (descending) for consistent allocation
        List<TenderRankingDTO> sortedRankings = new ArrayList<>(eligibleRankings);
        sortedRankings.sort(Comparator.comparing(TenderRankingDTO::getFinalScore).reversed());

        // Calculate proportional quantities
        List<QuantityAllocation> quantityAllocations = new ArrayList<>();

        for (TenderRankingDTO ranking : sortedRankings) {
            BigDecimal proportion = ranking.getFinalScore().divide(totalScore, 4, RoundingMode.HALF_UP);
            int calculatedQuantity = proportion.multiply(BigDecimal.valueOf(item.getQuantity()))
                    .setScale(0, RoundingMode.HALF_UP)
                    .intValue();

            // Ensure minimum of 1 quantity if score is positive
            int quantity = calculatedQuantity <= 0 && ranking.getFinalScore().compareTo(BigDecimal.ZERO) > 0
                    ? 1
                    : calculatedQuantity;

            quantityAllocations.add(new QuantityAllocation(ranking.getBidId(), quantity));
            totalAllocated += quantity;
        }

        // Adjust for over-allocation or under-allocation
        if (totalAllocated != item.getQuantity()) {
            if (totalAllocated > item.getQuantity()) {
                // Reduce quantities starting from lowest score
                int excess = totalAllocated - item.getQuantity();
                adjustForOverAllocation(quantityAllocations, excess);
            } else if (totalAllocated < item.getQuantity()) {
                // Increase quantities starting from highest score
                int deficit = item.getQuantity() - totalAllocated;
                adjustForUnderAllocation(quantityAllocations, deficit);
            }
        }

        // Create allocations
        for (QuantityAllocation qa : quantityAllocations) {
            if (qa.quantity > 0) {
                AllocationResult allocation = new AllocationResult();
                allocation.setTenderId(tenderId);
                allocation.setBidId(qa.bidId);
                allocation.setItemId(item.getId());
                allocation.setQuantity(qa.quantity);
                allocation.setUnitPrice(unitPrice);
                allocation.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(qa.quantity)));

                allocations.add(allocation);
            }
        }
    }

    /**
     * Adjusts allocations when total allocated quantity exceeds item quantity
     */
    private void adjustForOverAllocation(List<QuantityAllocation> quantityAllocations, int excess) {
        // Sort by quantity ascending (allocate from smallest to largest)
        quantityAllocations.sort(Comparator.comparingInt(qa -> qa.quantity));

        for (QuantityAllocation qa : quantityAllocations) {
            if (excess <= 0) break;

            int reduction = Math.min(qa.quantity - 1, excess);
            if (reduction > 0) {
                qa.quantity -= reduction;
                excess -= reduction;
            }
        }
    }

    /**
     * Adjusts allocations when total allocated quantity is less than item quantity
     */
    private void adjustForUnderAllocation(List<QuantityAllocation> quantityAllocations, int deficit) {
        // Sort by quantity descending (allocate from largest to smallest)
        quantityAllocations.sort((qa1, qa2) -> Integer.compare(qa2.quantity, qa1.quantity));

        for (QuantityAllocation qa : quantityAllocations) {
            if (deficit <= 0) break;

            qa.quantity += 1;
            deficit -= 1;
        }
    }

    /**
     * Calculates composite bid value as described in the paper.
     * Composite Bid = Sum(Item Price * Item Weight)
     */
    private BigDecimal calculateCompositeBid(BidDTO bid, TenderDTO tenderDTO) {
        BigDecimal compositeBid = BigDecimal.ZERO;

        // Map of criteria weights
        Map<Long, BigDecimal> criteriaWeights = tenderDTO.getCriteria().stream()
                .collect(Collectors.toMap(c -> c.getId(), c -> c.getWeight()));

        for (BidItemDTO bidItem : bid.getItems()) {
            BigDecimal weight = criteriaWeights.getOrDefault(bidItem.getCriteriaId(), BigDecimal.ONE);
            BigDecimal weightedPrice = bidItem.getValue().multiply(weight);
            compositeBid = compositeBid.add(weightedPrice);
        }

        return compositeBid;
    }

    @Override
    public List<AllocationResultDTO> getAllocationsByTender(Long tenderId) {
        log.info("Getting allocations for tender ID: {}", tenderId);

        List<AllocationResult> allocations = allocationRepository.findByTenderId(tenderId);

        List<AllocationResultDTO> allocationDTOs = allocationMapper.toDtoList(allocations);

        return enrichAllocationDTOs(allocationDTOs);
    }

    @Override
    public List<AllocationResultDTO> getAllocationsByBid(Long bidId) {
        log.info("Getting allocations for bid ID: {}", bidId);

        List<AllocationResult> allocations = allocationRepository.findByBidId(bidId);

        List<AllocationResultDTO> allocationDTOs = allocationMapper.toDtoList(allocations);

        return enrichAllocationDTOs(allocationDTOs);
    }

    @Override
    @Transactional
    public void clearAllocations(Long tenderId) {
        log.info("Clearing allocations for tender ID: {}", tenderId);

        allocationRepository.deleteByTenderId(tenderId);
    }

    /**
     * Helper class for ratio-based quantity allocation.
     */
    private static class QuantityAllocation {
        private final Long bidId;
        private int quantity;

        public QuantityAllocation(Long bidId, int quantity) {
            this.bidId = bidId;
            this.quantity = quantity;
        }
    }

    /**
     * Helper method to enrich allocation DTOs with bidder and item names.
     * Uses batch fetching to minimize API calls.
     */
    private List<AllocationResultDTO> enrichAllocationDTOs(List<AllocationResultDTO> allocationDTOs) {
        if (allocationDTOs.isEmpty()) {
            return allocationDTOs;
        }

        // Get unique tender IDs and bid IDs
        Set<Long> tenderIds = allocationDTOs.stream()
                .map(AllocationResultDTO::getTenderId)
                .collect(Collectors.toSet());

        Set<Long> bidIds = allocationDTOs.stream()
                .map(AllocationResultDTO::getBidId)
                .collect(Collectors.toSet());

        // Pre-fetch tender and bid data in batches
        Map<Long, String> bidderNames = new HashMap<>();
        Map<Long, TenderDTO> tenderDataCache = new HashMap<>();
        Map<Long, String> itemNames = new HashMap<>();

        // Fetch bid data
        for (Long bidId : bidIds) {
            try {
                BidDTO bid = bidClient.getBidById(bidId);
                bidderNames.put(bidId, bid.getTendererName());
            } catch (Exception e) {
                log.warn("Failed to get bid data for bid ID: {}", bidId, e);
                bidderNames.put(bidId, "Unknown");
            }
        }

        // Fetch tender data
        for (Long tenderId : tenderIds) {
            try {
                TenderDTO tender = tenderClient.getTenderById(tenderId);
                tenderDataCache.put(tenderId, tender);

                // Pre-compute item names
                for (TenderItemDTO item : tender.getItems()) {
                    itemNames.put(item.getId(), item.getName());
                }
            } catch (Exception e) {
                log.warn("Failed to get tender data for tender ID: {}", tenderId, e);
            }
        }

        // Enrich DTOs with bidder and item names
        for (AllocationResultDTO dto : allocationDTOs) {
            // Set bidder name
            dto.setBidderName(bidderNames.getOrDefault(dto.getBidId(), "Unknown"));

            // Set item name
            dto.setItemName(itemNames.getOrDefault(dto.getItemId(), "Unknown"));
        }

        return allocationDTOs;
    }
}

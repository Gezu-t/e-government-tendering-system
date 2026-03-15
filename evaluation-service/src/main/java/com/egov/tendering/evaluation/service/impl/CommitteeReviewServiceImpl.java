package com.egov.tendering.evaluation.service.impl;

import com.egov.tendering.evaluation.client.UserClient;
import com.egov.tendering.evaluation.dal.dto.CommitteeApprovalPolicyDTO;
import com.egov.tendering.evaluation.dal.dto.CommitteeApprovalPolicyRequest;
import com.egov.tendering.evaluation.dal.dto.CommitteeReviewDTO;
import com.egov.tendering.evaluation.dal.dto.ReviewRequest;
import com.egov.tendering.evaluation.dal.model.CommitteeApprovalPolicy;
import com.egov.tendering.evaluation.dal.mapper.CommitteeReviewMapper;
import com.egov.tendering.evaluation.dal.model.CommitteeReview;
import com.egov.tendering.evaluation.dal.model.ReviewStatus;
import com.egov.tendering.evaluation.dal.repository.CommitteeApprovalPolicyRepository;
import com.egov.tendering.evaluation.dal.repository.CommitteeReviewRepository;
import com.egov.tendering.evaluation.event.EvaluationEventPublisher;
import com.egov.tendering.evaluation.exception.ReviewNotFoundException;
import com.egov.tendering.evaluation.service.CommitteeReviewService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommitteeReviewServiceImpl implements CommitteeReviewService {

    @Value("${app.committee.default-required-review-count:3}")
    private int defaultRequiredReviewCount;

    @Value("${app.committee.default-minimum-approval-count:3}")
    private int defaultMinimumApprovalCount;

    private final CommitteeReviewRepository reviewRepository;
    private final CommitteeApprovalPolicyRepository policyRepository;
    private final CommitteeReviewMapper reviewMapper;
    private final EvaluationEventPublisher eventPublisher;
    private final UserClient userClient;

    @Override
    @Transactional
    public CommitteeReviewDTO createReview(Long tenderId, ReviewRequest request, Long committeeMemberId) {
        log.info("Creating review for tender ID: {} by committee member ID: {}", tenderId, committeeMemberId);
        boolean approvedBefore = isEvaluationApprovedByCommittee(tenderId);

        // Check if review already exists
        reviewRepository.findByTenderIdAndCommitteeMemberId(tenderId, committeeMemberId)
                .ifPresent(review -> {
                    throw new IllegalStateException("Review already exists for this tender and committee member");
                });

        CommitteeReview review = new CommitteeReview();
        review.setTenderId(tenderId);
        review.setCommitteeMemberId(committeeMemberId);
        review.setStatus(request.getStatus());
        review.setComments(request.getComments());

        review = reviewRepository.save(review);

        // Publish event
        eventPublisher.publishReviewCreatedEvent(review);

        publishApprovalEventIfThresholdCrossed(tenderId, approvedBefore);

        return enrichReviewDTO(reviewMapper.toDto(review));
    }

    @Override
    public CommitteeReviewDTO getReviewById(Long reviewId) {
        log.info("Getting review by ID: {}", reviewId);

        CommitteeReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        return enrichReviewDTO(reviewMapper.toDto(review));
    }

    @Override
    @Transactional
    public CommitteeReviewDTO updateReview(Long reviewId, ReviewRequest request) {
        log.info("Updating review ID: {} with status: {}", reviewId, request.getStatus());

        CommitteeReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));
        boolean approvedBefore = isEvaluationApprovedByCommittee(review.getTenderId());

        ReviewStatus oldStatus = review.getStatus();
        review.setStatus(request.getStatus());
        review.setComments(request.getComments());

        review = reviewRepository.save(review);

        // Publish event
        eventPublisher.publishReviewUpdatedEvent(review, oldStatus);

        publishApprovalEventIfThresholdCrossed(review.getTenderId(), approvedBefore);

        return enrichReviewDTO(reviewMapper.toDto(review));
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId) {
        log.info("Deleting review ID: {}", reviewId);

        CommitteeReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));
        boolean approvedBefore = isEvaluationApprovedByCommittee(review.getTenderId());

        // Publish event before deletion
        eventPublisher.publishReviewDeletedEvent(review);

        reviewRepository.delete(review);
        publishApprovalEventIfThresholdCrossed(review.getTenderId(), approvedBefore);
    }

    @Override
    public List<CommitteeReviewDTO> getReviewsByTender(Long tenderId) {
        log.info("Getting reviews for tender ID: {}", tenderId);

        List<CommitteeReview> reviews = reviewRepository.findByTenderId(tenderId);

        return enrichReviewDTOs(reviewMapper.toDtoList(reviews));
    }

    @Override
    public List<CommitteeReviewDTO> getReviewsByTenderAndStatus(Long tenderId, ReviewStatus status) {
        log.info("Getting reviews for tender ID: {} with status: {}", tenderId, status);

        List<CommitteeReview> reviews = reviewRepository.findByTenderIdAndStatus(tenderId, status);

        return enrichReviewDTOs(reviewMapper.toDtoList(reviews));
    }

    @Override
    public List<CommitteeReviewDTO> getReviewsByCommitteeMember(Long committeeMemberId) {
        log.info("Getting reviews by committee member ID: {}", committeeMemberId);

        List<CommitteeReview> reviews = reviewRepository.findByCommitteeMemberId(committeeMemberId);

        return enrichReviewDTOs(reviewMapper.toDtoList(reviews));
    }

    @Override
    public CommitteeReviewDTO getReviewByTenderAndCommitteeMember(Long tenderId, Long committeeMemberId) {
        log.info("Getting review for tender ID: {} and committee member ID: {}", tenderId, committeeMemberId);

        CommitteeReview review = reviewRepository.findByTenderIdAndCommitteeMemberId(tenderId, committeeMemberId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found for tender ID: " +
                        tenderId + " and committee member ID: " + committeeMemberId));

        return enrichReviewDTO(reviewMapper.toDto(review));
    }

    @Override
    public boolean isEvaluationApprovedByCommittee(Long tenderId) {
        CommitteeApprovalPolicy policy = resolvePolicy(tenderId);
        List<CommitteeReview> reviews = reviewRepository.findByTenderId(tenderId);
        if (reviews.size() < policy.getRequiredReviewCount()) {
            return false;
        }
        long approvedCount = reviews.stream()
                .filter(review -> review.getStatus() == ReviewStatus.APPROVED)
                .count();
        boolean hasRejectedReview = reviews.stream()
                .anyMatch(review -> review.getStatus() == ReviewStatus.REJECTED);
        boolean hasPendingReview = reviews.stream()
                .anyMatch(review -> review.getStatus() == ReviewStatus.PENDING);

        return !hasRejectedReview
                && !hasPendingReview
                && approvedCount >= policy.getMinimumApprovalCount();
    }

    @Override
    public CommitteeApprovalPolicyDTO getApprovalPolicy(Long tenderId) {
        return toPolicyDto(resolvePolicy(tenderId));
    }

    @Override
    @Transactional
    public CommitteeApprovalPolicyDTO upsertApprovalPolicy(Long tenderId, CommitteeApprovalPolicyRequest request) {
        validatePolicy(request.getRequiredReviewCount(), request.getMinimumApprovalCount());
        CommitteeApprovalPolicy policy = policyRepository.findByTenderId(tenderId)
                .orElseGet(() -> CommitteeApprovalPolicy.builder().tenderId(tenderId).build());
        policy.setRequiredReviewCount(request.getRequiredReviewCount());
        policy.setMinimumApprovalCount(request.getMinimumApprovalCount());
        return toPolicyDto(policyRepository.save(policy));
    }

    // Helper method to enrich review DTO with committee member name
    private CommitteeReviewDTO enrichReviewDTO(CommitteeReviewDTO reviewDTO) {
        try {
            String memberName = userClient.getUsernameById(reviewDTO.getCommitteeMemberId());
            reviewDTO.setCommitteeMemberName(memberName);
        } catch (Exception e) {
            log.warn("Failed to get committee member name for ID: {}", reviewDTO.getCommitteeMemberId());
            reviewDTO.setCommitteeMemberName("Unknown");
        }
        return reviewDTO;
    }

    // Helper method to enrich a list of review DTOs
    private List<CommitteeReviewDTO> enrichReviewDTOs(List<CommitteeReviewDTO> reviewDTOs) {
        return reviewDTOs.stream()
                .map(this::enrichReviewDTO)
                .collect(Collectors.toList());
    }

    private void publishApprovalEventIfThresholdCrossed(Long tenderId, boolean approvedBefore) {
        boolean approvedAfter = isEvaluationApprovedByCommittee(tenderId);
        if (!approvedBefore && approvedAfter) {
            eventPublisher.publishTenderEvaluationApprovedEvent(tenderId);
        }
    }

    private CommitteeApprovalPolicy resolvePolicy(Long tenderId) {
        return policyRepository.findByTenderId(tenderId)
                .orElseGet(() -> CommitteeApprovalPolicy.builder()
                        .tenderId(tenderId)
                        .requiredReviewCount(defaultRequiredReviewCount)
                        .minimumApprovalCount(defaultMinimumApprovalCount)
                        .build());
    }

    private CommitteeApprovalPolicyDTO toPolicyDto(CommitteeApprovalPolicy policy) {
        return CommitteeApprovalPolicyDTO.builder()
                .id(policy.getId())
                .tenderId(policy.getTenderId())
                .requiredReviewCount(policy.getRequiredReviewCount())
                .minimumApprovalCount(policy.getMinimumApprovalCount())
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .build();
    }

    private void validatePolicy(int requiredReviewCount, int minimumApprovalCount) {
        if (minimumApprovalCount > requiredReviewCount) {
            throw new IllegalArgumentException("Minimum approval count cannot exceed required review count");
        }
    }
}

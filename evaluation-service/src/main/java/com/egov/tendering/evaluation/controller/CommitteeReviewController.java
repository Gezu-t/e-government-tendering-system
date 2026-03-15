package com.egov.tendering.evaluation.controller;

import com.egov.tendering.evaluation.config.JwtUserIdExtractor;
import com.egov.tendering.evaluation.dal.dto.CommitteeReviewDTO;
import com.egov.tendering.evaluation.dal.dto.CommitteeApprovalPolicyDTO;
import com.egov.tendering.evaluation.dal.dto.CommitteeApprovalPolicyRequest;
import com.egov.tendering.evaluation.dal.dto.ReviewRequest;
import com.egov.tendering.evaluation.dal.model.ReviewStatus;
import com.egov.tendering.evaluation.service.CommitteeReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for committee review operations.
 */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Slf4j
public class CommitteeReviewController {

    private final CommitteeReviewService reviewService;
    private final JwtUserIdExtractor jwtUserIdExtractor;

    /**
     * Creates a new committee review for a tender.
     *
     * @param tenderId          The tender ID
     * @param request           The review request
     * @param committeeMemberId The ID of the committee member
     * @return The created review
     */
    @PostMapping("/tenders/{tenderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    public ResponseEntity<CommitteeReviewDTO> createReview(
            @PathVariable Long tenderId,
            @Valid @RequestBody ReviewRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long committeeMemberId = getUserId(jwt);

        log.info("Creating review for tender ID: {} by committee member ID: {}",
                tenderId, committeeMemberId);
        CommitteeReviewDTO createdReview = reviewService.createReview(tenderId, request, committeeMemberId);
        return new ResponseEntity<>(createdReview, HttpStatus.CREATED);
    }

    /**
     * Gets a review by its ID.
     *
     * @param reviewId The review ID
     * @return The review
     */
    @GetMapping("/{reviewId}")
    public ResponseEntity<CommitteeReviewDTO> getReviewById(@PathVariable Long reviewId) {
        log.info("Getting review by ID: {}", reviewId);
        CommitteeReviewDTO review = reviewService.getReviewById(reviewId);
        return ResponseEntity.ok(review);
    }

    /**
     * Updates an existing review.
     *
     * @param reviewId The review ID
     * @param request  The updated review request
     * @return The updated review
     */
    @PutMapping("/{reviewId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    public ResponseEntity<CommitteeReviewDTO> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewRequest request) {

        log.info("Updating review ID: {}", reviewId);
        CommitteeReviewDTO updatedReview = reviewService.updateReview(reviewId, request);
        return ResponseEntity.ok(updatedReview);
    }

    /**
     * Deletes a review.
     *
     * @param reviewId The review ID
     * @return No content response
     */
    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId) {
        log.info("Deleting review ID: {}", reviewId);
        reviewService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Gets all reviews for a tender.
     *
     * @param tenderId The tender ID
     * @return List of reviews
     */
    @GetMapping("/tenders/{tenderId}")
    public ResponseEntity<List<CommitteeReviewDTO>> getReviewsByTender(@PathVariable Long tenderId) {
        log.info("Getting reviews for tender ID: {}", tenderId);
        List<CommitteeReviewDTO> reviews = reviewService.getReviewsByTender(tenderId);
        return ResponseEntity.ok(reviews);
    }

    /**
     * Gets reviews for a tender with a specific status.
     *
     * @param tenderId The tender ID
     * @param status   The status to filter by
     * @return List of filtered reviews
     */
    @GetMapping("/tenders/{tenderId}/status/{status}")
    public ResponseEntity<List<CommitteeReviewDTO>> getReviewsByTenderAndStatus(
            @PathVariable Long tenderId,
            @PathVariable ReviewStatus status) {

        log.info("Getting reviews for tender ID: {} with status: {}", tenderId, status);
        List<CommitteeReviewDTO> reviews = reviewService.getReviewsByTenderAndStatus(tenderId, status);
        return ResponseEntity.ok(reviews);
    }

    /**
     * Gets all reviews by a committee member.
     *
     * @param committeeMemberId The committee member ID
     * @return List of reviews
     */
    @GetMapping("/members/{committeeMemberId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    public ResponseEntity<List<CommitteeReviewDTO>> getReviewsByCommitteeMember(
            @PathVariable Long committeeMemberId) {

        log.info("Getting reviews by committee member ID: {}", committeeMemberId);
        List<CommitteeReviewDTO> reviews = reviewService.getReviewsByCommitteeMember(committeeMemberId);
        return ResponseEntity.ok(reviews);
    }

    /**
     * Gets a review by tender and committee member.
     *
     * @param tenderId          The tender ID
     * @param committeeMemberId The committee member ID
     * @return The review
     */
    @GetMapping("/tenders/{tenderId}/members/{committeeMemberId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    public ResponseEntity<CommitteeReviewDTO> getReviewByTenderAndCommitteeMember(
            @PathVariable Long tenderId,
            @PathVariable Long committeeMemberId) {

        log.info("Getting review for tender ID: {} and committee member ID: {}",
                tenderId, committeeMemberId);
        CommitteeReviewDTO review = reviewService.getReviewByTenderAndCommitteeMember(tenderId, committeeMemberId);
        return ResponseEntity.ok(review);
    }

    /**
     * Checks if a tender evaluation is approved by the committee.
     *
     * @param tenderId The tender ID
     * @return Whether the evaluation is approved
     */
    @GetMapping("/tenders/{tenderId}/approved")
    public ResponseEntity<Boolean> isEvaluationApprovedByCommittee(@PathVariable Long tenderId) {
        log.info("Checking if evaluation is approved for tender ID: {}", tenderId);
        boolean isApproved = reviewService.isEvaluationApprovedByCommittee(tenderId);
        return ResponseEntity.ok(isApproved);
    }

    @GetMapping("/tenders/{tenderId}/policy")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    public ResponseEntity<CommitteeApprovalPolicyDTO> getApprovalPolicy(@PathVariable Long tenderId) {
        log.info("Getting committee approval policy for tender ID: {}", tenderId);
        return ResponseEntity.ok(reviewService.getApprovalPolicy(tenderId));
    }

    @PutMapping("/tenders/{tenderId}/policy")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    public ResponseEntity<CommitteeApprovalPolicyDTO> upsertApprovalPolicy(
            @PathVariable Long tenderId,
            @Valid @RequestBody CommitteeApprovalPolicyRequest request) {
        log.info("Updating committee approval policy for tender ID: {}", tenderId);
        return ResponseEntity.ok(reviewService.upsertApprovalPolicy(tenderId, request));
    }

    private Long getUserId(Jwt jwt) {
        return jwtUserIdExtractor.requireUserId(jwt);
    }
}

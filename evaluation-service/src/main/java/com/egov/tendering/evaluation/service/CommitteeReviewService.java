package com.egov.tendering.evaluation.service;

import com.egov.tendering.evaluation.dal.dto.CommitteeReviewDTO;
import com.egov.tendering.evaluation.dal.dto.CommitteeApprovalPolicyDTO;
import com.egov.tendering.evaluation.dal.dto.CommitteeApprovalPolicyRequest;
import com.egov.tendering.evaluation.dal.dto.ReviewRequest;
import com.egov.tendering.evaluation.dal.model.ReviewStatus;

import java.util.List;

public interface CommitteeReviewService {

    CommitteeReviewDTO createReview(Long tenderId, ReviewRequest request, Long committeeMemberId);

    CommitteeReviewDTO getReviewById(Long reviewId);

    CommitteeReviewDTO updateReview(Long reviewId, ReviewRequest request);

    void deleteReview(Long reviewId);

    List<CommitteeReviewDTO> getReviewsByTender(Long tenderId);

    List<CommitteeReviewDTO> getReviewsByTenderAndStatus(Long tenderId, ReviewStatus status);

    List<CommitteeReviewDTO> getReviewsByCommitteeMember(Long committeeMemberId);

    CommitteeReviewDTO getReviewByTenderAndCommitteeMember(Long tenderId, Long committeeMemberId);

    boolean isEvaluationApprovedByCommittee(Long tenderId);

    CommitteeApprovalPolicyDTO getApprovalPolicy(Long tenderId);

    CommitteeApprovalPolicyDTO upsertApprovalPolicy(Long tenderId, CommitteeApprovalPolicyRequest request);
}

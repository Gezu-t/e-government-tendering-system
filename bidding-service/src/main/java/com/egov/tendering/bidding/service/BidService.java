package com.egov.tendering.bidding.service;

import com.egov.tendering.bidding.dal.dto.*;
import com.egov.tendering.bidding.dal.model.BidStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BidService {

  /**
   * Create a new bid draft
   */
  BidDTO createBid(BidSubmissionRequest request, Long tendererId);

  /**
   * Get a bid by its ID
   */
  BidDTO getBidById(Long bidId);

  /**
   * Get all bids for a specific tender with pagination
   */
  PageDTO<BidDTO> getBidsByTender(Long tenderId, Pageable pageable);

  /**
   * Get all bids for a specific tenderer with pagination
   */
  PageDTO<BidDTO> getBidsByTenderer(Long tendererId, Pageable pageable);

  /**
   * Get all bids for a specific tender with a specific status
   */
  List<BidDTO> getBidsByTenderAndStatus(Long tenderId, BidStatus status);

  /**
   * Update an existing bid
   */
  BidDTO updateBid(Long bidId, BidSubmissionRequest request, Long tendererId);

  /**
   * Submit a bid for evaluation
   */
  BidDTO submitBid(Long bidId);

  /**
   * Update a bid's status
   */
  BidDTO updateBidStatus(Long bidId, BidStatus status);

  /**
   * Delete a draft bid
   */
  void deleteBid(Long bidId);

  /**
   * Add a document to a bid
   */
  BidDTO addDocumentToBid(Long bidId, MultipartFile file, String fileName);

  /**
   * Remove a document from a bid
   */
  void removeDocumentFromBid(Long bidId, Long documentId);

  /**
   * Check if a tenderer has already created a bid for a tender
   */
  boolean hasTendererBidForTender(Long tenderId, Long tendererId);

  /**
   * Submit a bid with security document
   */
  BidDTO submitBidWithSecurity(Long bidId, BidSecurityRequest securityRequest, MultipartFile securityDocument);

  /**
   * Get bid security for a bid
   */
  BidSecurityDTO getBidSecurityByBidId(Long bidId);

  /**
   * Validate bid compliance against tender requirements
   */
  ComplianceCheckResult validateBidCompliance(Long bidId);

  /**
   * Get clarifications for a bid
   */
  List<BidClarificationDTO> getClarificationsByBidId(Long bidId);

  /**
   * Request a clarification for a bid
   */
  BidClarificationDTO requestClarification(Long bidId, String question, Long evaluatorId, int daysToRespond);

  /**
   * Respond to a clarification
   */
  BidClarificationDTO respondToClarification(Long clarificationId, String response, Long tendererId);

  /**
   * Get version history for a bid
   */
  List<BidVersionDTO> getBidVersions(Long bidId);

  /**
   * Get a specific version of a bid
   */
  BidVersionDTO getBidVersion(Long bidId, Integer versionNumber);

  /**
   * Close all bids for a tender when tender closing date is reached
   */
  void closeBidsForTender(Long tenderId);

  /**
   * Cancel all bids for a tender (called when tender is cancelled)
   */
  void cancelBidsForTender(Long tenderId, String reason);

  /**
   * Update a bid's status based on evaluation results
   */
  void updateBidEvaluationStatus(Long bidId, String evaluationResult, Long evaluatedBy, String comments);

  /**
   * Mark a bid as awarded
   */
  void awardBid(Long bidId, Long awardedBy, String awardComments);

  /**
   * Update a bid's status to reflect contract creation
   */
  void updateBidContractStatus(Long bidId, Long contractId);

  /**
   * Update a bid's contract status when a downstream service only provides tender and bidder identifiers.
   */
  void updateBidContractStatusByTenderAndTenderer(Long tenderId, Long tendererId, Long contractId);


}

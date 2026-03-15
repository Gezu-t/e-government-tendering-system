package com.egov.tendering.tender.service;


import com.egov.tendering.tender.dal.dto.*;
import com.egov.tendering.tender.dal.model.TenderStatus;
import com.egov.tendering.tender.dal.model.TenderType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;



public interface TenderService {

  TenderDTO createTender(CreateTenderRequest request, Long tendereeId);

  TenderDTO getTenderById(Long tenderId);

  Page<TenderDTO> searchTenders(String title, TenderStatus status, TenderType type, Pageable pageable);

  Page<TenderDTO> getTendersByTenderee(Long tendereeId, Pageable pageable);

  TenderDTO updateTenderStatus(Long tenderId, UpdateTenderStatusRequest request);

  TenderDTO publishTender(Long tenderId);

  TenderDTO closeTender(Long tenderId);

  /**
   * Amends a published tender. Updates the tender details and notifies all registered bidders.
   * Only PUBLISHED or AMENDED tenders can be amended.
   */
  TenderDTO amendTender(Long tenderId, TenderAmendmentRequest request, Long amendedBy);

  /**
   * Gets all amendments for a tender.
   */
  List<TenderAmendmentDTO> getTenderAmendments(Long tenderId);

  void checkForExpiredTenders();
}

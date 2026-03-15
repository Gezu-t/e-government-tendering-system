package com.egov.tendering.tender.service;

import com.egov.tendering.tender.dal.dto.PreBidAnswerRequest;
import com.egov.tendering.tender.dal.dto.PreBidClarificationDTO;
import com.egov.tendering.tender.dal.dto.PreBidQuestionRequest;

import java.util.List;

public interface PreBidClarificationService {

    PreBidClarificationDTO askQuestion(Long tenderId, PreBidQuestionRequest request, Long userId);

    PreBidClarificationDTO answerQuestion(Long tenderId, Long clarificationId, PreBidAnswerRequest request, Long userId);

    PreBidClarificationDTO rejectQuestion(Long tenderId, Long clarificationId, Long userId);

    List<PreBidClarificationDTO> getPublicClarifications(Long tenderId);

    List<PreBidClarificationDTO> getAllClarifications(Long tenderId);

    List<PreBidClarificationDTO> getPendingClarifications(Long tenderId);

    List<PreBidClarificationDTO> getMyClarifications(Long tenderId, Long userId);
}

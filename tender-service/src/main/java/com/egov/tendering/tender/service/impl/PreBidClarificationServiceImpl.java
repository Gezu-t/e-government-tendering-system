package com.egov.tendering.tender.service.impl;

import com.egov.tendering.tender.dal.dto.PreBidAnswerRequest;
import com.egov.tendering.tender.dal.dto.PreBidClarificationDTO;
import com.egov.tendering.tender.dal.dto.PreBidQuestionRequest;
import com.egov.tendering.tender.dal.model.PreBidClarification;
import com.egov.tendering.tender.dal.model.PreBidClarification.ClarificationStatus;
import com.egov.tendering.tender.dal.model.Tender;
import com.egov.tendering.tender.dal.model.TenderStatus;
import com.egov.tendering.tender.dal.repository.PreBidClarificationRepository;
import com.egov.tendering.tender.dal.repository.TenderRepository;
import com.egov.tendering.tender.exception.TenderNotFoundException;
import com.egov.tendering.tender.service.PreBidClarificationService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PreBidClarificationServiceImpl implements PreBidClarificationService {

    private final PreBidClarificationRepository clarificationRepository;
    private final TenderRepository tenderRepository;

    @Override
    @Transactional
    public PreBidClarificationDTO askQuestion(Long tenderId, PreBidQuestionRequest request, Long userId) {
        log.info("User {} asking pre-bid question for tender {}", userId, tenderId);

        Tender tender = tenderRepository.findById(tenderId)
                .orElseThrow(() -> new TenderNotFoundException("Tender not found: " + tenderId));

        if (tender.getStatus() != TenderStatus.PUBLISHED && tender.getStatus() != TenderStatus.AMENDED) {
            throw new IllegalStateException("Questions can only be asked for PUBLISHED or AMENDED tenders");
        }

        if (LocalDateTime.now().isAfter(tender.getSubmissionDeadline())) {
            throw new IllegalStateException("Cannot ask questions after the submission deadline");
        }

        PreBidClarification clarification = PreBidClarification.builder()
                .tenderId(tenderId)
                .question(request.getQuestion())
                .askedBy(userId)
                .askedByOrgName(request.getOrganizationName())
                .category(request.getCategory())
                .status(ClarificationStatus.PENDING)
                .isPublic(true)
                .build();

        clarification = clarificationRepository.save(clarification);
        log.info("Pre-bid question {} created for tender {}", clarification.getId(), tenderId);

        return toDTO(clarification);
    }

    @Override
    @Transactional
    public PreBidClarificationDTO answerQuestion(Long clarificationId, PreBidAnswerRequest request, Long userId) {
        log.info("User {} answering clarification {}", userId, clarificationId);

        PreBidClarification clarification = clarificationRepository.findById(clarificationId)
                .orElseThrow(() -> new EntityNotFoundException("Clarification not found: " + clarificationId));

        if (clarification.getStatus() != ClarificationStatus.PENDING) {
            throw new IllegalStateException("Only pending clarifications can be answered");
        }

        clarification.setAnswer(request.getAnswer());
        clarification.setAnsweredBy(userId);
        clarification.setAnsweredAt(LocalDateTime.now());
        clarification.setStatus(ClarificationStatus.ANSWERED);

        if (request.getMakePublic() != null) {
            clarification.setIsPublic(request.getMakePublic());
        }

        clarification = clarificationRepository.save(clarification);
        log.info("Clarification {} answered", clarificationId);

        return toDTO(clarification);
    }

    @Override
    @Transactional
    public PreBidClarificationDTO rejectQuestion(Long clarificationId, Long userId) {
        log.info("User {} rejecting clarification {}", userId, clarificationId);

        PreBidClarification clarification = clarificationRepository.findById(clarificationId)
                .orElseThrow(() -> new EntityNotFoundException("Clarification not found: " + clarificationId));

        clarification.setStatus(ClarificationStatus.REJECTED);
        clarification.setAnsweredBy(userId);
        clarification.setAnsweredAt(LocalDateTime.now());
        clarification = clarificationRepository.save(clarification);

        return toDTO(clarification);
    }

    @Override
    public List<PreBidClarificationDTO> getPublicClarifications(Long tenderId) {
        return clarificationRepository.findByTenderIdAndIsPublicTrueOrderByAskedAtDesc(tenderId)
                .stream()
                .filter(c -> c.getStatus() == ClarificationStatus.ANSWERED)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PreBidClarificationDTO> getAllClarifications(Long tenderId) {
        return clarificationRepository.findByTenderIdOrderByAskedAtDesc(tenderId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PreBidClarificationDTO> getPendingClarifications(Long tenderId) {
        return clarificationRepository.findByTenderIdAndStatusOrderByAskedAtDesc(tenderId, ClarificationStatus.PENDING)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PreBidClarificationDTO> getMyClarifications(Long tenderId, Long userId) {
        return clarificationRepository.findByTenderIdAndAskedByOrderByAskedAtDesc(tenderId, userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private PreBidClarificationDTO toDTO(PreBidClarification c) {
        return PreBidClarificationDTO.builder()
                .id(c.getId())
                .tenderId(c.getTenderId())
                .question(c.getQuestion())
                .answer(c.getAnswer())
                .askedBy(c.getAskedBy())
                .askedByOrgName(c.getAskedByOrgName())
                .answeredBy(c.getAnsweredBy())
                .category(c.getCategory())
                .isPublic(c.getIsPublic())
                .status(c.getStatus())
                .askedAt(c.getAskedAt())
                .answeredAt(c.getAnsweredAt())
                .build();
    }
}

package com.egov.tendering.evaluation.service.impl;

import com.egov.tendering.evaluation.dal.dto.ConflictDeclarationRequest;
import com.egov.tendering.evaluation.dal.dto.ConflictOfInterestDTO;
import com.egov.tendering.evaluation.dal.model.ConflictOfInterest;
import com.egov.tendering.evaluation.dal.repository.ConflictOfInterestRepository;
import com.egov.tendering.evaluation.service.ConflictOfInterestService;
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
public class ConflictOfInterestServiceImpl implements ConflictOfInterestService {

    private static final String DEFAULT_DECLARATION = "I hereby declare that the information provided above is true and complete. " +
            "I understand that failure to disclose a conflict of interest may result in disqualification from the evaluation process.";

    private final ConflictOfInterestRepository repository;

    @Override
    @Transactional
    public ConflictOfInterestDTO declareConflict(Long tenderId, ConflictDeclarationRequest request, Long evaluatorId) {
        log.info("Evaluator {} declaring conflict of interest for tender {}", evaluatorId, tenderId);

        if (repository.existsByTenderIdAndEvaluatorId(tenderId, evaluatorId)) {
            throw new IllegalStateException("Conflict of interest already declared for this tender");
        }

        ConflictOfInterest declaration = ConflictOfInterest.builder()
                .tenderId(tenderId)
                .evaluatorId(evaluatorId)
                .hasConflict(request.getHasConflict())
                .conflictDescription(request.getConflictDescription())
                .relatedOrganizationId(request.getRelatedOrganizationId())
                .relationshipType(request.getRelationshipType())
                .declarationText(request.getDeclarationText() != null ? request.getDeclarationText() : DEFAULT_DECLARATION)
                .acknowledged(true)
                .build();

        declaration = repository.save(declaration);

        if (Boolean.TRUE.equals(request.getHasConflict())) {
            log.warn("CONFLICT OF INTEREST: Evaluator {} declared conflict for tender {} with org {}",
                    evaluatorId, tenderId, request.getRelatedOrganizationId());
        }

        return toDTO(declaration);
    }

    @Override
    public List<ConflictOfInterestDTO> getDeclarationsForTender(Long tenderId) {
        return repository.findByTenderId(tenderId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<ConflictOfInterestDTO> getConflictsForTender(Long tenderId) {
        return repository.findByTenderIdAndHasConflictTrue(tenderId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public boolean hasEvaluatorDeclared(Long tenderId, Long evaluatorId) {
        return repository.existsByTenderIdAndEvaluatorId(tenderId, evaluatorId);
    }

    @Override
    @Transactional
    public ConflictOfInterestDTO reviewDeclaration(Long declarationId, String decision, String comments, Long reviewerId) {
        log.info("Reviewing conflict declaration {} by reviewer {}", declarationId, reviewerId);

        ConflictOfInterest declaration = repository.findById(declarationId)
                .orElseThrow(() -> new EntityNotFoundException("Declaration not found: " + declarationId));

        declaration.setReviewedBy(reviewerId);
        declaration.setReviewDecision(decision);
        declaration.setReviewComments(comments);
        declaration.setReviewedAt(LocalDateTime.now());
        declaration = repository.save(declaration);

        return toDTO(declaration);
    }

    private ConflictOfInterestDTO toDTO(ConflictOfInterest c) {
        return ConflictOfInterestDTO.builder()
                .id(c.getId())
                .tenderId(c.getTenderId())
                .evaluatorId(c.getEvaluatorId())
                .hasConflict(c.getHasConflict())
                .conflictDescription(c.getConflictDescription())
                .relatedOrganizationId(c.getRelatedOrganizationId())
                .relationshipType(c.getRelationshipType())
                .acknowledged(c.getAcknowledged())
                .reviewDecision(c.getReviewDecision())
                .reviewComments(c.getReviewComments())
                .declaredAt(c.getDeclaredAt())
                .build();
    }
}

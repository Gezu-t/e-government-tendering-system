package com.egov.tendering.evaluation.service;

import com.egov.tendering.evaluation.dal.dto.ConflictDeclarationRequest;
import com.egov.tendering.evaluation.dal.dto.ConflictOfInterestDTO;

import java.util.List;

public interface ConflictOfInterestService {

    ConflictOfInterestDTO declareConflict(Long tenderId, ConflictDeclarationRequest request, Long evaluatorId);

    List<ConflictOfInterestDTO> getDeclarationsForTender(Long tenderId);

    List<ConflictOfInterestDTO> getConflictsForTender(Long tenderId);

    boolean hasEvaluatorDeclared(Long tenderId, Long evaluatorId);

    ConflictOfInterestDTO reviewDeclaration(Long declarationId, String decision, String comments, Long reviewerId);
}

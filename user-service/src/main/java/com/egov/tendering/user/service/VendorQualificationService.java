package com.egov.tendering.user.service;

import com.egov.tendering.user.dal.dto.QualificationReviewRequest;
import com.egov.tendering.user.dal.dto.VendorQualificationDTO;
import com.egov.tendering.user.dal.dto.VendorQualificationRequest;
import com.egov.tendering.user.dal.model.QualificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface VendorQualificationService {

    VendorQualificationDTO submitQualification(VendorQualificationRequest request);

    VendorQualificationDTO getQualificationById(Long qualificationId);

    List<VendorQualificationDTO> getQualificationsByOrganization(Long organizationId);

    Page<VendorQualificationDTO> getQualificationsByStatus(QualificationStatus status, Pageable pageable);

    VendorQualificationDTO reviewQualification(Long qualificationId, QualificationReviewRequest request, Long reviewerId);

    boolean isOrganizationQualified(Long organizationId);

    void checkForExpiredQualifications();
}

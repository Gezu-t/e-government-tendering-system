package com.egov.tendering.user.dal.repository;

import com.egov.tendering.user.dal.model.QualificationStatus;
import com.egov.tendering.user.dal.model.VendorQualification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface VendorQualificationRepository extends JpaRepository<VendorQualification, Long> {

    List<VendorQualification> findByOrganizationId(Long organizationId);

    Optional<VendorQualification> findByOrganizationIdAndQualificationCategory(Long organizationId, String category);

    Page<VendorQualification> findByStatus(QualificationStatus status, Pageable pageable);

    List<VendorQualification> findByOrganizationIdAndStatus(Long organizationId, QualificationStatus status);

    @Query("SELECT vq FROM VendorQualification vq WHERE vq.status = 'QUALIFIED' AND vq.validUntil < :date")
    List<VendorQualification> findExpiredQualifications(@Param("date") LocalDate date);

    @Query("SELECT CASE WHEN COUNT(vq) > 0 THEN true ELSE false END FROM VendorQualification vq " +
           "WHERE vq.organizationId = :orgId AND vq.status = 'QUALIFIED' AND vq.validUntil >= :date")
    boolean isOrganizationQualified(@Param("orgId") Long organizationId, @Param("date") LocalDate date);
}

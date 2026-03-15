package com.egov.tendering.user.dal.repository;

import com.egov.tendering.user.dal.model.OrganizationBlacklist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface OrganizationBlacklistRepository extends JpaRepository<OrganizationBlacklist, Long> {

    List<OrganizationBlacklist> findByOrganizationIdAndActiveTrue(Long organizationId);

    Page<OrganizationBlacklist> findByActiveTrue(Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM OrganizationBlacklist b " +
           "WHERE b.organizationId = :orgId AND b.active = true " +
           "AND (b.isPermanent = true OR b.effectiveUntil >= :date)")
    boolean isOrganizationBlacklisted(@Param("orgId") Long organizationId, @Param("date") LocalDate date);

    @Query("SELECT b FROM OrganizationBlacklist b WHERE b.active = true " +
           "AND b.isPermanent = false AND b.effectiveUntil < :date")
    List<OrganizationBlacklist> findExpiredBlacklists(@Param("date") LocalDate date);
}

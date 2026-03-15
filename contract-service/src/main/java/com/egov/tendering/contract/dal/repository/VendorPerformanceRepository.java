package com.egov.tendering.contract.dal.repository;

import com.egov.tendering.contract.dal.model.VendorPerformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface VendorPerformanceRepository extends JpaRepository<VendorPerformance, Long> {

    List<VendorPerformance> findByContractId(Long contractId);

    List<VendorPerformance> findByVendorIdOrderByCreatedAtDesc(Long vendorId);

    Optional<VendorPerformance> findByContractIdAndReviewPeriod(Long contractId, String reviewPeriod);

    @Query("SELECT AVG(vp.overallScore) FROM VendorPerformance vp WHERE vp.vendorId = :vendorId")
    BigDecimal getAverageOverallScore(@Param("vendorId") Long vendorId);

    @Query("SELECT COUNT(vp) FROM VendorPerformance vp WHERE vp.vendorId = :vendorId AND vp.overallScore >= 7.0")
    long countGoodPerformances(@Param("vendorId") Long vendorId);
}

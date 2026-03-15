package com.egov.tendering.tender.dal.repository;

import com.egov.tendering.tender.dal.model.TenderAmendment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TenderAmendmentRepository extends JpaRepository<TenderAmendment, Long> {

    List<TenderAmendment> findByTenderIdOrderByAmendmentNumberDesc(Long tenderId);

    @Query("SELECT COALESCE(MAX(ta.amendmentNumber), 0) FROM TenderAmendment ta WHERE ta.tenderId = :tenderId")
    Integer findMaxAmendmentNumber(@Param("tenderId") Long tenderId);

    Optional<TenderAmendment> findByTenderIdAndAmendmentNumber(Long tenderId, Integer amendmentNumber);
}

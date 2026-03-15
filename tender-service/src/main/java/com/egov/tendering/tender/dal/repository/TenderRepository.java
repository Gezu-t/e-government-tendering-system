package com.egov.tendering.tender.dal.repository;

import com.egov.tendering.tender.dal.model.Tender;
import com.egov.tendering.tender.dal.model.TenderStatus;
import com.egov.tendering.tender.dal.model.TenderType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;


@Repository
public interface TenderRepository extends JpaRepository<Tender, Long> {

    Page<Tender> findByTendereeId(Long tendereeId, Pageable pageable);

    Page<Tender> findByStatus(TenderStatus status, Pageable pageable);

    Page<Tender> findByStatusAndType(TenderStatus status, TenderType type, Pageable pageable);

    @Query("SELECT t FROM Tender t WHERE t.status = :status AND t.submissionDeadline < :now")
    List<Tender> findExpiredTenders(@Param("status") TenderStatus status, @Param("now") LocalDateTime now);

    @Query("SELECT t FROM Tender t WHERE " +
            "(:title IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
            "(:status IS NULL OR t.status = :status) AND " +
            "(:type IS NULL OR t.type = :type)")
    Page<Tender> searchTenders(
            @Param("title") String title,
            @Param("status") TenderStatus status,
            @Param("type") TenderType type,
            Pageable pageable);

    @Query("SELECT t FROM Tender t WHERE " +
            "(:title IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
            "(t.status IN :statuses) AND " +
            "(:type IS NULL OR t.type = :type)")
    Page<Tender> searchPublicTenders(
            @Param("title") String title,
            @Param("statuses") List<TenderStatus> statuses,
            @Param("type") TenderType type,
            Pageable pageable);
}

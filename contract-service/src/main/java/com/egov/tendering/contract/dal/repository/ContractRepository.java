package com.egov.tendering.contract.dal.repository;

import com.egov.tendering.contract.dal.model.Contract;
import com.egov.tendering.contract.dal.model.ContractStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {

    List<Contract> findByTenderId(Long tenderId);

    Page<Contract> findByBidderId(Long bidderId, Pageable pageable);

    boolean existsByTenderIdAndBidderId(Long tenderId, Long bidderId);

    @Query("SELECT c FROM Contract c WHERE " +
            "(:title IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
            "(:status IS NULL OR c.status = :status) AND " +
            "(:tenderId IS NULL OR c.tenderId = :tenderId) AND " +
            "(:bidderId IS NULL OR c.bidderId = :bidderId)")
    Page<Contract> searchContracts(
            @Param("title") String title,
            @Param("status") ContractStatus status,
            @Param("tenderId") Long tenderId,
            @Param("bidderId") Long bidderId,
            Pageable pageable);

    @Query("SELECT c FROM Contract c WHERE " +
            "c.createdBy = :createdBy AND " +
            "(:title IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
            "(:status IS NULL OR c.status = :status) AND " +
            "(:tenderId IS NULL OR c.tenderId = :tenderId) AND " +
            "(:bidderId IS NULL OR c.bidderId = :bidderId)")
    Page<Contract> searchContractsByCreator(
            @Param("createdBy") String createdBy,
            @Param("title") String title,
            @Param("status") ContractStatus status,
            @Param("tenderId") Long tenderId,
            @Param("bidderId") Long bidderId,
            Pageable pageable);

    List<Contract> findByStatusAndEndDateBefore(ContractStatus status, LocalDate currentDate);
}

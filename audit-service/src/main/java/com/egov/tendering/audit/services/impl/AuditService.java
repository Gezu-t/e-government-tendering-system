package com.egov.tendering.audit.services.impl;


import com.egov.tendering.audit.dal.dto.*;
import com.egov.tendering.audit.dal.dto.BidStatisticsDto;
import com.egov.tendering.audit.dal.dto.DashboardWidgetsDto;
import com.egov.tendering.audit.dal.dto.TenderStatusReportDto;
import com.egov.tendering.audit.dal.mapper.AuditMapper;
import com.egov.tendering.audit.dal.model.AuditActionType;
import com.egov.tendering.audit.dal.model.AuditLog;
import com.egov.tendering.audit.dal.repository.AuditLogRepository;

import com.egov.tendering.audit.specification.AuditLogSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing audit logs
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final AuditMapper auditMapper;

    /**
     * Create a new audit log entry
     */
    @Transactional
    public AuditLogResponse createAuditLog(AuditLogRequest request) {
        log.debug("Creating audit log entry for action: {}", request.getAction());

        AuditLog auditLog = auditMapper.toEntity(request);

        // Set hostname
        try {
            auditLog.setHostName(InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            log.warn("Unable to determine hostname", e);
            auditLog.setHostName("unknown");
        }

        AuditLog savedAuditLog = auditLogRepository.save(auditLog);
        return auditMapper.toResponse(savedAuditLog);
    }

    /**
     * Create an audit log entry from an event received from Kafka
     */
    @Transactional
    public AuditLogResponse createAuditLogFromEvent(AuditEventDTO event) {
        log.debug("Creating audit log entry from event for action: {}", event.getAction());

        AuditLog auditLog = auditMapper.toEntity(event);
        AuditLog savedAuditLog = auditLogRepository.save(auditLog);
        return auditMapper.toResponse(savedAuditLog);
    }

    /**
     * Get an audit log by ID
     */
    @Transactional(readOnly = true)
    public AuditLogResponse getAuditLog(Long id) {
        log.debug("Getting audit log with ID: {}", id);

        AuditLog auditLog = auditLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Audit log not found with ID: " + id));

        return auditMapper.toResponse(auditLog);
    }

    /**
     * Search audit logs with filtering
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> searchAuditLogs(AuditLogFilter filter, Pageable pageable) {
        log.debug("Searching audit logs with filter: {}", filter);

        Specification<AuditLog> spec = buildSpecification(filter);
        Page<AuditLog> auditLogs = auditLogRepository.findAll(spec, pageable);

        return auditLogs.map(auditMapper::toResponse);
    }

    /**
     * Get audit logs by entity
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogsByEntity(String entityType, String entityId, Pageable pageable) {
        log.debug("Getting audit logs for entity type: {}, ID: {}", entityType, entityId);

        Page<AuditLog> auditLogs = auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(
                entityType, entityId, pageable);

        return auditLogs.map(auditMapper::toResponse);
    }

    /**
     * Get audit logs by user
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogsByUser(Long userId, Pageable pageable) {
        log.debug("Getting audit logs for user: {}", userId);

        Page<AuditLog> auditLogs = auditLogRepository.findByUserIdOrderByTimestampDesc(userId, pageable);

        return auditLogs.map(auditMapper::toResponse);
    }

    /**
     * Get audit logs by action type
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogsByActionType(AuditActionType actionType, Pageable pageable) {
        log.debug("Getting audit logs for action type: {}", actionType);

        Page<AuditLog> auditLogs = auditLogRepository.findByActionTypeOrderByTimestampDesc(actionType, pageable);

        return auditLogs.map(auditMapper::toResponse);
    }

    /**
     * Get audit logs by time range
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogsByTimeRange(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        log.debug("Getting audit logs between {} and {}", startTime, endTime);

        Page<AuditLog> auditLogs = auditLogRepository.findByTimestampBetweenOrderByTimestampDesc(
                startTime, endTime, pageable);

        return auditLogs.map(auditMapper::toResponse);
    }

    /**
     * Get audit logs by correlation ID
     */
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditLogsByCorrelationId(String correlationId) {
        log.debug("Getting audit logs for correlation ID: {}", correlationId);

        List<AuditLog> auditLogs = auditLogRepository.findByCorrelationIdOrderByTimestampAsc(correlationId);

        return auditLogs.stream()
                .map(auditMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get audit statistics
     */
    @Transactional(readOnly = true)
    public AuditStatisticsDto getAuditStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        log.debug("Getting audit statistics between {} and {}", startTime, endTime);

        // Get total count
        long totalCount = auditLogRepository.count();

        // Get success and failure counts
        long successCount = 0;
        long failureCount = 0;
        Map<AuditActionType, Long> actionTypeCounts = new HashMap<>();
        Map<String, Long> entityTypeCounts = new HashMap<>();
        Map<String, Long> userActivityCounts = new HashMap<>();

        // This approach might not be efficient for large datasets
        // In a production environment, consider using aggregation queries
        AuditLogFilter filter = AuditLogFilter.builder()
                .startTime(startTime)
                .endTime(endTime)
                .build();

        Specification<AuditLog> spec = buildSpecification(filter);
        List<AuditLog> auditLogs = auditLogRepository.findAll(spec);

        for (AuditLog log : auditLogs) {
            if (log.isSuccess()) {
                successCount++;
            } else {
                failureCount++;
            }

            // Count by action type
            actionTypeCounts.put(log.getActionType(),
                    actionTypeCounts.getOrDefault(log.getActionType(), 0L) + 1);

            // Count by entity type
            entityTypeCounts.put(log.getEntityType(),
                    entityTypeCounts.getOrDefault(log.getEntityType(), 0L) + 1);

            // Count by user
            userActivityCounts.put(String.valueOf(log.getUserId()),
                    userActivityCounts.getOrDefault(log.getUserId(), 0L) + 1);
        }

        return AuditStatisticsDto.builder()
                .totalAuditLogs(totalCount)
                .successfulActions(successCount)
                .failedActions(failureCount)
                .actionTypeCounts(actionTypeCounts)
                .entityTypeCounts(entityTypeCounts)
                .userActivityCounts(userActivityCounts)
                .build();
    }

    /**
     * Get daily audit summary for a specific action type
     */
    @Transactional(readOnly = true)
    public List<AuditSummaryDto> getDailySummary(AuditActionType actionType, LocalDateTime startTime, LocalDateTime endTime) {
        log.debug("Getting daily summary for action type: {} between {} and {}", actionType, startTime, endTime);

        List<Object[]> results = auditLogRepository.getActionSummaryByDay(actionType, startTime, endTime);

        List<AuditSummaryDto> summaries = new ArrayList<>();
        for (Object[] result : results) {
            LocalDate date = ((java.sql.Date) result[0]).toLocalDate();
            long count = ((Number) result[1]).longValue();

            // We would need additional queries to get success/failure counts
            // This is a simplified implementation
            AuditSummaryDto summary = AuditSummaryDto.builder()
                    .date(date)
                    .actionType(actionType.name())
                    .count(count)
                    .build();

            summaries.add(summary);
        }

        return summaries;
    }

    /**
     * Get total audit log count
     */
    @Transactional(readOnly = true)
    public long getTotalAuditCount() {
        return auditLogRepository.count();
    }

    /**
     * Get audit count grouped by action type
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getAuditCountByAction() {
        List<AuditLog> all = auditLogRepository.findAll();
        return all.stream()
                .filter(a -> a.getActionType() != null)
                .collect(Collectors.groupingBy(
                        a -> a.getActionType().name(),
                        Collectors.counting()));
    }

    /**
     * Get audit count grouped by module/entity type
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getAuditCountByModule() {
        List<AuditLog> all = auditLogRepository.findAll();
        return all.stream()
                .filter(a -> a.getEntityType() != null)
                .collect(Collectors.groupingBy(
                        AuditLog::getEntityType,
                        Collectors.counting()));
    }

    /**
     * Get audit count for a specific time period
     */
    @Transactional(readOnly = true)
    public long getAuditCountForPeriod(LocalDateTime startTime, LocalDateTime endTime) {
        AuditLogFilter filter = AuditLogFilter.builder()
                .startTime(startTime)
                .endTime(endTime)
                .build();
        Specification<AuditLog> spec = buildSpecification(filter);
        return auditLogRepository.count(spec);
    }

    @Transactional(readOnly = true)
    public ProcurementReport buildProcurementReport(LocalDate from, LocalDate to) {
        LocalDateTime startTime = from.atStartOfDay();
        LocalDateTime endTime = to.atTime(23, 59, 59);

        List<AuditLog> allLogs = auditLogRepository.findAll();
        List<AuditLog> periodLogs = auditLogRepository.findAll(buildSpecification(AuditLogFilter.builder()
                .startTime(startTime)
                .endTime(endTime)
                .build()));

        Map<String, Long> tendersByStatus = summarizeLatestStatus(allLogs, Set.of("TENDER"));
        Map<String, Long> bidsByStatus = summarizeLatestStatus(allLogs, Set.of("TENDEROFFER", "BID"));
        Map<String, Long> contractsByStatus = summarizeLatestStatus(allLogs, Set.of("CONTRACT"));

        ProcurementReport.TenderSummary tenderSummary = ProcurementReport.TenderSummary.builder()
                .totalTenders(countDistinctEntities(allLogs, Set.of("TENDER")))
                .tendersByStatus(tendersByStatus)
                .tendersByType(Map.of())
                .tendersPublishedThisPeriod(countActions(periodLogs, Set.of("TENDER"), "PUBLISHED"))
                .tendersClosedThisPeriod(countActions(periodLogs, Set.of("TENDER"), "CLOSED"))
                .tendersAwardedThisPeriod(countActions(periodLogs, Set.of("TENDER"), "AWARDED"))
                .build();

        long totalTenders = tenderSummary.getTotalTenders();
        long totalBids = countDistinctEntities(allLogs, Set.of("TENDEROFFER", "BID"));
        ProcurementReport.BidSummary bidSummary = ProcurementReport.BidSummary.builder()
                .totalBids(totalBids)
                .bidsByStatus(bidsByStatus)
                .averageBidsPerTender(totalTenders == 0 ? 0.0 : (double) totalBids / totalTenders)
                .bidsSubmittedThisPeriod(countActions(periodLogs, Set.of("TENDEROFFER", "BID"), "SUBMITTED"))
                .flaggedBids(countFlaggedBids(periodLogs))
                .build();

        ProcurementReport.ContractSummary contractSummary = ProcurementReport.ContractSummary.builder()
                .totalContracts(countDistinctEntities(allLogs, Set.of("CONTRACT")))
                .contractsByStatus(contractsByStatus)
                .activeContracts(contractsByStatus.getOrDefault("ACTIVE", 0L))
                .completedContracts(contractsByStatus.getOrDefault("COMPLETED", 0L))
                .overdueMilestones(0L)
                .build();

        ProcurementReport.AuditSummary auditSummary = ProcurementReport.AuditSummary.builder()
                .totalAuditEntries(allLogs.size())
                .entriesByAction(getAuditCountByAction())
                .entriesByModule(getAuditCountByModule())
                .entriesThisPeriod(periodLogs.size())
                .build();

        return ProcurementReport.builder()
                .reportDate(LocalDate.now())
                .reportPeriod(from + " to " + to)
                .tenderSummary(tenderSummary)
                .bidSummary(bidSummary)
                .contractSummary(contractSummary)
                .auditSummary(auditSummary)
                .build();
    }

    // -------------------------------------------------------------------------
    // Phase 11 – Dashboard widgets, tender status report, bid statistics
    // -------------------------------------------------------------------------

    /**
     * Returns compact counts for dashboard widget cards.
     */
    @Transactional(readOnly = true)
    public DashboardWidgetsDto getDashboardWidgets() {
        List<AuditLog> all = auditLogRepository.findAll();

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        List<AuditLog> todayLogs = all.stream()
                .filter(l -> !l.getTimestamp().isBefore(todayStart))
                .collect(Collectors.toList());

        List<AuditLog> monthLogs = all.stream()
                .filter(l -> !l.getTimestamp().isBefore(monthStart))
                .collect(Collectors.toList());

        Map<String, Long> tenderStatus = summarizeLatestStatus(all, Set.of("TENDER"));
        Map<String, Long> bidStatus    = summarizeLatestStatus(all, Set.of("TENDEROFFER", "BID"));
        Map<String, Long> contractStatus = summarizeLatestStatus(all, Set.of("CONTRACT"));

        long activeTenders  = tenderStatus.getOrDefault("PUBLISHED", 0L) + tenderStatus.getOrDefault("ACTIVE", 0L);
        long pendingBids    = bidStatus.getOrDefault("SUBMITTED", 0L);
        long activeContracts = contractStatus.getOrDefault("ACTIVE", 0L);

        long auditAlertsToday = todayLogs.stream()
                .filter(l -> l.getActionType() != null
                        && (l.getActionType().name().contains("COLLUSION")
                            || l.getActionType().name().contains("TAMPER")
                            || l.getActionType().name().contains("FLAG")
                            || l.getActionType().name().contains("BLACKLIST")))
                .count();

        long tendersPublishedThisMonth = countActions(monthLogs, Set.of("TENDER"), "PUBLISHED");
        long bidsSubmittedThisMonth    = countActions(monthLogs, Set.of("TENDEROFFER", "BID"), "SUBMITTED");
        long contractsAwardedThisMonth = countActions(monthLogs, Set.of("CONTRACT"), "AWARD");

        return DashboardWidgetsDto.builder()
                .activeTenders(activeTenders)
                .pendingBids(pendingBids)
                .activeContracts(activeContracts)
                .auditAlertsToday(auditAlertsToday)
                .tendersPublishedThisMonth(tendersPublishedThisMonth)
                .bidsSubmittedThisMonth(bidsSubmittedThisMonth)
                .contractsAwardedThisMonth(contractsAwardedThisMonth)
                .auditEntriesCreatedToday(todayLogs.size())
                .build();
    }

    /**
     * Returns a tender status breakdown for the given date range.
     */
    @Transactional(readOnly = true)
    public TenderStatusReportDto getTenderStatusReport(LocalDate from, LocalDate to) {
        LocalDateTime startTime = from.atStartOfDay();
        LocalDateTime endTime   = to.atTime(23, 59, 59);

        List<AuditLog> all = auditLogRepository.findAll();
        List<AuditLog> periodLogs = all.stream()
                .filter(l -> !l.getTimestamp().isBefore(startTime) && !l.getTimestamp().isAfter(endTime))
                .collect(Collectors.toList());

        Map<String, Long> byStatus = summarizeLatestStatus(all, Set.of("TENDER"));

        return TenderStatusReportDto.builder()
                .period(from + " to " + to)
                .totalDistinctTenders(countDistinctEntities(all, Set.of("TENDER")))
                .byStatus(byStatus)
                .publishedThisPeriod(countActions(periodLogs, Set.of("TENDER"), "PUBLISHED"))
                .closedThisPeriod(countActions(periodLogs, Set.of("TENDER"), "CLOSED"))
                .awardedThisPeriod(countActions(periodLogs, Set.of("TENDER"), "AWARDED"))
                .amendedThisPeriod(countActions(periodLogs, Set.of("TENDER"), "AMEND"))
                .build();
    }

    /**
     * Returns bid statistics for the given date range.
     */
    @Transactional(readOnly = true)
    public BidStatisticsDto getBidStatistics(LocalDate from, LocalDate to) {
        LocalDateTime startTime = from.atStartOfDay();
        LocalDateTime endTime   = to.atTime(23, 59, 59);

        List<AuditLog> all = auditLogRepository.findAll();
        List<AuditLog> periodLogs = all.stream()
                .filter(l -> !l.getTimestamp().isBefore(startTime) && !l.getTimestamp().isAfter(endTime))
                .collect(Collectors.toList());

        long totalBids    = countDistinctEntities(all, Set.of("TENDEROFFER", "BID"));
        long totalTenders = countDistinctEntities(all, Set.of("TENDER"));
        Map<String, Long> byStatus = summarizeLatestStatus(all, Set.of("TENDEROFFER", "BID"));

        return BidStatisticsDto.builder()
                .period(from + " to " + to)
                .totalBids(totalBids)
                .submittedThisPeriod(countActions(periodLogs, Set.of("TENDEROFFER", "BID"), "SUBMITTED"))
                .flaggedBids(countFlaggedBids(periodLogs))
                .averageBidsPerTender(totalTenders == 0 ? 0.0 : (double) totalBids / totalTenders)
                .byStatus(byStatus)
                .evaluatedThisPeriod(countActions(periodLogs, Set.of("TENDEROFFER", "BID"), "EVALUAT"))
                .withdrawnThisPeriod(countActions(periodLogs, Set.of("TENDEROFFER", "BID"), "WITHDRAW"))
                .build();
    }

    /**
     * Delete audit logs older than a certain date
     */
    @Transactional
    public long purgeOldAuditLogs(LocalDateTime olderThan) {
        log.info("Purging audit logs older than: {}", olderThan);

        AuditLogFilter filter = AuditLogFilter.builder()
                .endTime(olderThan)
                .build();

        Specification<AuditLog> spec = buildSpecification(filter);
        List<AuditLog> logsToDelete = auditLogRepository.findAll(spec);

        int count = logsToDelete.size();
        if (count > 0) {
            auditLogRepository.deleteAll(logsToDelete);
            log.info("Deleted {} audit logs", count);
        }

        return count;
    }

    /**
     * Build a specification for filtering audit logs
     */
    private Specification<AuditLog> buildSpecification(AuditLogFilter filter) {
        Specification<AuditLog> spec = Specification.where(null);

        if (filter.getUserId() != null) {
            spec = spec.and(AuditLogSpecification.hasUserId(filter.getUserId()));
        }

        if (filter.getUsername() != null && !filter.getUsername().isEmpty()) {
            spec = spec.and(AuditLogSpecification.hasUsername(filter.getUsername()));
        }

        if (filter.getActionTypes() != null && !filter.getActionTypes().isEmpty()) {
            spec = spec.and(AuditLogSpecification.hasActionTypes(filter.getActionTypes()));
        }

        if (filter.getEntityType() != null && !filter.getEntityType().isEmpty()) {
            spec = spec.and(AuditLogSpecification.hasEntityType(filter.getEntityType()));
        }

        if (filter.getEntityId() != null && !filter.getEntityId().isEmpty()) {
            spec = spec.and(AuditLogSpecification.hasEntityId(filter.getEntityId()));
        }

        if (filter.getAction() != null && !filter.getAction().isEmpty()) {
            spec = spec.and(AuditLogSpecification.hasAction(filter.getAction()));
        }

        if (filter.getSuccess() != null) {
            spec = spec.and(AuditLogSpecification.hasSuccess(filter.getSuccess()));
        }

        if (filter.getKeyword() != null && !filter.getKeyword().isEmpty()) {
            spec = spec.and(AuditLogSpecification.containsKeyword(filter.getKeyword()));
        }

        if (filter.getCorrelationId() != null && !filter.getCorrelationId().isEmpty()) {
            spec = spec.and(AuditLogSpecification.hasCorrelationId(filter.getCorrelationId()));
        }

        if (filter.getServiceId() != null && !filter.getServiceId().isEmpty()) {
            spec = spec.and(AuditLogSpecification.hasServiceId(filter.getServiceId()));
        }

        if (filter.getStartTime() != null && filter.getEndTime() != null) {
            spec = spec.and(AuditLogSpecification.isInTimeRange(filter.getStartTime(), filter.getEndTime()));
        } else if (filter.getStartTime() != null) {
            spec = spec.and(AuditLogSpecification.isAfterTime(filter.getStartTime()));
        } else if (filter.getEndTime() != null) {
            spec = spec.and(AuditLogSpecification.isBeforeTime(filter.getEndTime()));
        }

        return spec;
    }

    private long countDistinctEntities(List<AuditLog> logs, Set<String> entityTypes) {
        return logs.stream()
                .filter(log -> matchesEntityType(log, entityTypes))
                .map(AuditLog::getEntityId)
                .filter(Objects::nonNull)
                .filter(id -> !id.isBlank())
                .filter(id -> !"UNKNOWN".equalsIgnoreCase(id))
                .distinct()
                .count();
    }

    private Map<String, Long> summarizeLatestStatus(List<AuditLog> logs, Set<String> entityTypes) {
        Map<String, AuditLog> latestByEntity = new HashMap<>();
        for (AuditLog log : logs) {
            if (!matchesEntityType(log, entityTypes) || log.getEntityId() == null || log.getEntityId().isBlank()) {
                continue;
            }
            AuditLog current = latestByEntity.get(log.getEntityId());
            if (current == null || log.getTimestamp().isAfter(current.getTimestamp())) {
                latestByEntity.put(log.getEntityId(), log);
            }
        }

        return latestByEntity.values().stream()
                .map(this::inferLifecycleStatus)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(status -> status, Collectors.counting()));
    }

    private long countActions(List<AuditLog> logs, Set<String> entityTypes, String keyword) {
        return logs.stream()
                .filter(log -> matchesEntityType(log, entityTypes))
                .filter(log -> containsKeyword(log.getAction(), keyword) || containsKeyword(log.getEventType(), keyword))
                .count();
    }

    private long countFlaggedBids(List<AuditLog> logs) {
        return logs.stream()
                .filter(log -> matchesEntityType(log, Set.of("TENDEROFFER", "BID")))
                .filter(log -> containsKeyword(log.getAction(), "FLAG")
                        || containsKeyword(log.getAction(), "TAMPER")
                        || containsKeyword(log.getAction(), "REJECT")
                        || containsKeyword(log.getEventType(), "FLAG")
                        || containsKeyword(log.getEventType(), "TAMPER")
                        || containsKeyword(log.getEventType(), "REJECT"))
                .map(AuditLog::getEntityId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
    }

    private boolean matchesEntityType(AuditLog log, Set<String> entityTypes) {
        if (log.getEntityType() == null) {
            return false;
        }
        return entityTypes.contains(log.getEntityType().toUpperCase(Locale.ROOT));
    }

    private boolean containsKeyword(String value, String keyword) {
        return value != null && value.toUpperCase(Locale.ROOT).contains(keyword);
    }

    private String inferLifecycleStatus(AuditLog log) {
        String combined = ((log.getAction() == null ? "" : log.getAction()) + " "
                + (log.getEventType() == null ? "" : log.getEventType())).toUpperCase(Locale.ROOT);
        if (combined.contains("CANCEL")) return "CANCELLED";
        if (combined.contains("TERMINAT")) return "TERMINATED";
        if (combined.contains("COMPLET")) return "COMPLETED";
        if (combined.contains("ACTIVE") || combined.contains("ACTIVAT")) return "ACTIVE";
        if (combined.contains("AWARD")) return "AWARDED";
        if (combined.contains("CLOSE")) return "CLOSED";
        if (combined.contains("PUBLISH")) return "PUBLISHED";
        if (combined.contains("SUBMIT")) return "SUBMITTED";
        if (combined.contains("EVALUAT")) return "EVALUATED";
        if (combined.contains("REJECT")) return "REJECTED";
        if (combined.contains("CREATE")) return "CREATED";
        return null;
    }
}

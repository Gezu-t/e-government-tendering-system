package com.egov.tendering.audit.controller;

import com.egov.tendering.audit.dal.dto.BidStatisticsDto;
import com.egov.tendering.audit.dal.dto.DashboardWidgetsDto;
import com.egov.tendering.audit.dal.dto.ProcurementReport;
import com.egov.tendering.audit.dal.dto.TenderStatusReportDto;
import com.egov.tendering.audit.services.impl.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final AuditService auditService;

    @GetMapping("/procurement-summary")
    public ResponseEntity<ProcurementReport> getProcurementSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        if (from == null) from = LocalDate.now().minusMonths(1);
        if (to == null) to = LocalDate.now();

        log.info("Generating procurement summary report from {} to {}", from, to);
        return ResponseEntity.ok(auditService.buildProcurementReport(from, to));
    }

    @GetMapping("/audit-activity")
    public ResponseEntity<Map<String, Object>> getAuditActivity(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        if (from == null) from = LocalDate.now().minusDays(7);
        if (to == null) to = LocalDate.now();

        Map<String, Object> activity = new HashMap<>();
        activity.put("period", from + " to " + to);
        activity.put("byAction", auditService.getAuditCountByAction());
        activity.put("byModule", auditService.getAuditCountByModule());
        activity.put("totalEntries", auditService.getAuditCountForPeriod(
                from.atStartOfDay(), to.atTime(23, 59, 59)));

        return ResponseEntity.ok(activity);
    }

    @GetMapping("/dashboard-widgets")
    public ResponseEntity<DashboardWidgetsDto> getDashboardWidgets() {
        log.info("Fetching dashboard widget counts");
        return ResponseEntity.ok(auditService.getDashboardWidgets());
    }

    @GetMapping("/tender-status")
    public ResponseEntity<TenderStatusReportDto> getTenderStatusReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        if (from == null) from = LocalDate.now().minusMonths(1);
        if (to == null) to = LocalDate.now();

        log.info("Generating tender status report from {} to {}", from, to);
        return ResponseEntity.ok(auditService.getTenderStatusReport(from, to));
    }

    @GetMapping("/bid-statistics")
    public ResponseEntity<BidStatisticsDto> getBidStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        if (from == null) from = LocalDate.now().minusMonths(1);
        if (to == null) to = LocalDate.now();

        log.info("Generating bid statistics from {} to {}", from, to);
        return ResponseEntity.ok(auditService.getBidStatistics(from, to));
    }
}

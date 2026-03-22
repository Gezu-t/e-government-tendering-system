package com.egov.tendering.audit.controller;

import com.egov.tendering.audit.dal.model.AuditLog;
import com.egov.tendering.audit.dal.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping("/search")
    public Page<AuditLog> search(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        Specification<AuditLog> spec = Specification.where(null);

        if (action != null && !action.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("action"), action));
        }
        if (entityType != null && !entityType.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("entityType"), entityType));
        }
        if (from != null && !from.isBlank()) {
            LocalDateTime start = LocalDate.parse(from).atStartOfDay();
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("timestamp"), start));
        }
        if (to != null && !to.isBlank()) {
            LocalDateTime end = LocalDate.parse(to).atTime(23, 59, 59);
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("timestamp"), end));
        }

        return auditLogRepository.findAll(spec, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp")));
    }
}

package com.egov.tendering.audit.specification;


import com.egov.tendering.audit.dal.model.AuditActionType;
import com.egov.tendering.audit.dal.model.AuditLog;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Specifications for filtering AuditLog entities
 */
public class AuditLogSpecification {

    public static Specification<AuditLog> hasUserId(Long userId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("userId"), userId);
    }

    public static Specification<AuditLog> hasUsername(String username) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("username"), username);
    }

    public static Specification<AuditLog> hasActionTypes(List<AuditActionType> actionTypes) {
        return (root, query, criteriaBuilder) ->
                root.get("actionType").in(actionTypes);
    }

    public static Specification<AuditLog> hasEntityType(String entityType) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("entityType"), entityType);
    }

    public static Specification<AuditLog> hasEntityId(String entityId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("entityId"), entityId);
    }

    public static Specification<AuditLog> hasAction(String action) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("action"), action);
    }

    public static Specification<AuditLog> hasSuccess(boolean success) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("success"), success);
    }

    public static Specification<AuditLog> containsKeyword(String keyword) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("action")),
                                "%" + keyword.toLowerCase() + "%"),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("details")),
                                "%" + keyword.toLowerCase() + "%")
                );
    }

    public static Specification<AuditLog> hasCorrelationId(String correlationId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("correlationId"), correlationId);
    }

    public static Specification<AuditLog> hasServiceId(String serviceId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("serviceId"), serviceId);
    }

    public static Specification<AuditLog> isInTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.between(root.get("timestamp"), startTime, endTime);
    }

    public static Specification<AuditLog> isAfterTime(LocalDateTime startTime) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get("timestamp"), startTime);
    }

    public static Specification<AuditLog> isBeforeTime(LocalDateTime endTime) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(root.get("timestamp"), endTime);
    }
}

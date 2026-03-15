package com.egov.tendering.user.dal.model;

import java.util.EnumSet;
import java.util.Set;

import static com.egov.tendering.user.dal.model.Permission.*;

/**
 * User roles with mapped permissions for the e-government tendering system.
 * Each role has a defined set of fine-grained permissions.
 */
public enum UserRole {

    ADMIN(EnumSet.allOf(Permission.class)),

    TENDEREE(EnumSet.of(
            TENDER_CREATE, TENDER_READ, TENDER_UPDATE, TENDER_PUBLISH, TENDER_AMEND, TENDER_CLOSE, TENDER_DELETE,
            BID_READ, BID_UNSEAL,
            EVALUATION_READ, EVALUATION_APPROVE,
            CONTRACT_CREATE, CONTRACT_READ, CONTRACT_UPDATE, CONTRACT_ACTIVATE, CONTRACT_TERMINATE,
            DOCUMENT_UPLOAD, DOCUMENT_READ, DOCUMENT_DELETE,
            USER_READ,
            QUALIFICATION_REVIEW,
            BLACKLIST_MANAGE,
            AUDIT_READ, AUDIT_EXPORT,
            REPORT_READ, REPORT_GENERATE,
            SIGNATURE_SIGN, SIGNATURE_VERIFY,
            COLLUSION_ANALYZE
    )),

    TENDERER(EnumSet.of(
            TENDER_READ,
            BID_CREATE, BID_READ, BID_SUBMIT, BID_UPDATE, BID_DELETE, BID_SEAL,
            CONTRACT_READ,
            DOCUMENT_UPLOAD, DOCUMENT_READ,
            USER_READ, USER_UPDATE,
            QUALIFICATION_SUBMIT,
            SIGNATURE_SIGN
    )),

    EVALUATOR(EnumSet.of(
            TENDER_READ,
            BID_READ,
            EVALUATION_CREATE, EVALUATION_READ, EVALUATION_UPDATE,
            CONTRACT_READ,
            DOCUMENT_READ,
            USER_READ,
            SIGNATURE_SIGN, SIGNATURE_VERIFY
    )),

    COMMITTEE(EnumSet.of(
            TENDER_READ,
            BID_READ,
            EVALUATION_READ, EVALUATION_APPROVE,
            CONTRACT_READ,
            DOCUMENT_READ,
            USER_READ,
            REPORT_READ,
            SIGNATURE_VERIFY
    ));

    private final Set<Permission> permissions;

    UserRole(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }
}

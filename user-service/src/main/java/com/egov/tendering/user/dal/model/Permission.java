package com.egov.tendering.user.dal.model;

/**
 * Fine-grained permissions for the e-government tendering system.
 * Each UserRole maps to a set of permissions.
 */
public enum Permission {

    // Tender permissions
    TENDER_CREATE,
    TENDER_READ,
    TENDER_UPDATE,
    TENDER_PUBLISH,
    TENDER_AMEND,
    TENDER_CLOSE,
    TENDER_DELETE,

    // Bid permissions
    BID_CREATE,
    BID_READ,
    BID_SUBMIT,
    BID_UPDATE,
    BID_DELETE,
    BID_SEAL,
    BID_UNSEAL,

    // Evaluation permissions
    EVALUATION_CREATE,
    EVALUATION_READ,
    EVALUATION_UPDATE,
    EVALUATION_APPROVE,

    // Contract permissions
    CONTRACT_CREATE,
    CONTRACT_READ,
    CONTRACT_UPDATE,
    CONTRACT_ACTIVATE,
    CONTRACT_TERMINATE,

    // Document permissions
    DOCUMENT_UPLOAD,
    DOCUMENT_READ,
    DOCUMENT_DELETE,

    // User management permissions
    USER_READ,
    USER_UPDATE,
    USER_DELETE,
    USER_MANAGE_ROLES,

    // Vendor qualification permissions
    QUALIFICATION_SUBMIT,
    QUALIFICATION_REVIEW,

    // Blacklist permissions
    BLACKLIST_MANAGE,

    // Audit permissions
    AUDIT_READ,
    AUDIT_EXPORT,

    // Report permissions
    REPORT_READ,
    REPORT_GENERATE,

    // Digital signature permissions
    SIGNATURE_SIGN,
    SIGNATURE_VERIFY,

    // Anti-collusion permissions
    COLLUSION_ANALYZE,

    // System permissions
    SYSTEM_ADMIN
}

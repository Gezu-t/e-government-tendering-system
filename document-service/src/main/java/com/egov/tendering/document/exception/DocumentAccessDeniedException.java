package com.egov.tendering.document.exception;

public class DocumentAccessDeniedException extends RuntimeException {

    public DocumentAccessDeniedException(String message) {
        super(message);
    }
}

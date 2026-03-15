package com.egov.tendering.bidding.exception;

public class BidVersioningException extends RuntimeException {

    public BidVersioningException(String message) {
        super(message);
    }

    public BidVersioningException(String message, Throwable cause) {
        super(message, cause);
    }
}

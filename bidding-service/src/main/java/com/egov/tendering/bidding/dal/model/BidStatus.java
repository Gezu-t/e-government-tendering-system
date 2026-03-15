package com.egov.tendering.bidding.dal.model;

import java.util.EnumSet;
import java.util.Map;

/**
 * Bid lifecycle states with enforced transition rules.
 *
 * Valid transitions:
 *   DRAFT -> SUBMITTED, CANCELLED
 *   SUBMITTED -> UNDER_EVALUATION, CANCELLED
 *   UNDER_EVALUATION -> EVALUATED, REJECTED, CANCELLED
 *   EVALUATED -> ACCEPTED, REJECTED
 *   ACCEPTED -> AWARDED
 *   AWARDED -> CONTRACTED
 *   CONTRACTED -> TERMINATED
 *   REJECTED -> (terminal)
 *   CANCELLED -> (terminal)
 *   TERMINATED -> (terminal)
 */
public enum BidStatus {
    DRAFT,
    SUBMITTED,
    UNDER_EVALUATION,
    EVALUATED,
    ACCEPTED,
    REJECTED,
    NOT_SUBMITTED,
    CANCELLED,
    AWARDED,
    CONTRACTED,
    TERMINATED;

    private static final Map<BidStatus, EnumSet<BidStatus>> ALLOWED_TRANSITIONS = Map.of(
            DRAFT, EnumSet.of(SUBMITTED, CANCELLED),
            SUBMITTED, EnumSet.of(UNDER_EVALUATION, CANCELLED),
            UNDER_EVALUATION, EnumSet.of(EVALUATED, REJECTED, CANCELLED),
            EVALUATED, EnumSet.of(ACCEPTED, REJECTED),
            ACCEPTED, EnumSet.of(AWARDED),
            AWARDED, EnumSet.of(CONTRACTED),
            CONTRACTED, EnumSet.of(TERMINATED)
    );

    /**
     * Checks if transitioning from this status to the target is allowed.
     */
    public boolean canTransitionTo(BidStatus target) {
        if (this == target) return true;
        EnumSet<BidStatus> allowed = ALLOWED_TRANSITIONS.get(this);
        return allowed != null && allowed.contains(target);
    }

    /**
     * Validates transition and throws if not allowed.
     */
    public void validateTransitionTo(BidStatus target) {
        if (!canTransitionTo(target)) {
            throw new IllegalStateException(
                    "Bid status transition from " + this + " to " + target + " is not allowed");
        }
    }

    public boolean isTerminal() {
        return this == REJECTED || this == CANCELLED || this == TERMINATED;
    }

    public boolean isModifiable() {
        return this == DRAFT;
    }
}

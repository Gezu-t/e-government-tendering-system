package com.egov.tendering.tender.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
public class TenderAmendedEvent extends TenderEvent {
    private Integer amendmentNumber;
    private String reason;
    private LocalDateTime previousDeadline;
    private LocalDateTime newDeadline;
    private Long amendedBy;
}

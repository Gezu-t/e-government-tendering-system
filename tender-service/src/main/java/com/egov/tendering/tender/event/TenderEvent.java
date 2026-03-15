package com.egov.tendering.tender.event;



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;


@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class TenderEvent {
    private Long eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private Long tenderId;
    private String tenderTitle;
    private Long actorUserId;
}

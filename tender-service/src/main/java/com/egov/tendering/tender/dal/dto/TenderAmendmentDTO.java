package com.egov.tendering.tender.dal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenderAmendmentDTO {
    private Long id;
    private Long tenderId;
    private Integer amendmentNumber;
    private String reason;
    private String description;
    private LocalDateTime previousDeadline;
    private LocalDateTime newDeadline;
    private Long amendedBy;
    private LocalDateTime createdAt;
}

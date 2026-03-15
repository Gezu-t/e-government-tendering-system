package com.egov.tendering.tender.dal.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tender_amendments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenderAmendment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tender_id", nullable = false)
    private Long tenderId;

    @Column(name = "amendment_number", nullable = false)
    private Integer amendmentNumber;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "previous_deadline")
    private LocalDateTime previousDeadline;

    @Column(name = "new_deadline")
    private LocalDateTime newDeadline;

    @Column(name = "previous_description", columnDefinition = "TEXT")
    private String previousDescription;

    @Column(name = "amended_by", nullable = false)
    private Long amendedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

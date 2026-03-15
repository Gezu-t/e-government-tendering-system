package com.egov.tendering.user.dal.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "organization_blacklist")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "type", nullable = false, length = 30)
    private BlacklistType type;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_until")
    private LocalDate effectiveUntil;

    @Column(name = "is_permanent", nullable = false)
    private Boolean isPermanent;

    @Column(name = "imposed_by", nullable = false)
    private Long imposedBy;

    @Column(name = "lifted_by")
    private Long liftedBy;

    @Column(name = "lifted_at")
    private LocalDateTime liftedAt;

    @Column(name = "lift_reason")
    private String liftReason;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (active == null) active = true;
        if (isPermanent == null) isPermanent = false;
    }

    public enum BlacklistType {
        DEBARMENT,
        SUSPENSION,
        WARNING
    }
}

package com.egov.tendering.contract.dal.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "contracts",
        indexes = {
                @Index(name = "idx_tender_bidder", columnList = "tenderId,bidderId"),
                @Index(name = "idx_contract_number", columnList = "contractNumber")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contract {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  @NotNull(message = "Tender ID cannot be null")
  @Positive(message = "Tender ID must be positive")
  private Long tenderId;

  @Column(nullable = false)
  @NotNull(message = "Bidder ID cannot be null")
  @Positive(message = "Bidder ID must be positive")
  private Long bidderId;

  @Column(nullable = false, unique = true)
  @NotNull(message = "Contract number cannot be null")
  @Size(min = 1, max = 50, message = "Contract number must be between 1 and 50 characters")
  private String contractNumber;

  @Column(nullable = false)
  @NotNull(message = "Title cannot be null")
  @Size(min = 1, max = 100, message = "Title must be between 1 and 100 characters")
  private String title;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(nullable = false)
  @NotNull(message = "Start date cannot be null")
  @FutureOrPresent(message = "Start date must be present or future")
  private LocalDate startDate;

  @Column(nullable = false)
  @NotNull(message = "End date cannot be null")
  @Future(message = "End date must be in the future")
  private LocalDate endDate;

  @Column(nullable = false)
  @NotNull(message = "Total value cannot be null")
  @DecimalMin(value = "0.0", inclusive = true, message = "Total value must be non-negative")
  private BigDecimal totalValue;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(nullable = false)
  @NotNull(message = "Status cannot be null")
  private ContractStatus status;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column
  private LocalDateTime updatedAt;

  @Column(nullable = false)
  @NotNull(message = "Created by cannot be null")
  @Size(max = 50, message = "Created by must not exceed 50 characters")
  private String createdBy;

  @Column
  @Size(max = 50, message = "Updated by must not exceed 50 characters")
  private String updatedBy;

  @Version
  private Long version;

  // Relationships
  @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @Builder.Default
  private List<ContractItem> items = new ArrayList<>();

  @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @Builder.Default
  private List<ContractMilestone> milestones = new ArrayList<>();

  // Custom setters to maintain bidirectional relationship
  public void setItems(List<ContractItem> items) {
    if (items != null) {
      this.items.clear();
      items.forEach(item -> item.setContract(this));
      this.items.addAll(items);
    }
  }

  public void setMilestones(List<ContractMilestone> milestones) {
    if (milestones != null) {
      this.milestones.clear();
      milestones.forEach(milestone -> milestone.setContract(this));
      this.milestones.addAll(milestones);
    }
  }

  @PrePersist
  protected void onCreate() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
    if (createdBy == null) {
      createdBy = "system";
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}

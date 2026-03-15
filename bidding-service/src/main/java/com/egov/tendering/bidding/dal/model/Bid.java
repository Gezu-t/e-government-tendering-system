package com.egov.tendering.bidding.dal.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bids", indexes = {
    @Index(name = "idx_bids_tender_id", columnList = "tender_id"),
    @Index(name = "idx_bids_tenderer_id", columnList = "tenderer_id"),
    @Index(name = "idx_bids_tender_status", columnList = "tender_id, status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bid {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version
  private Long version;

  @Column(name = "tender_id", nullable = false)
  private Long tenderId;

  @Column(name = "tenderer_id", nullable = false)
  private Long tendererId;

  private Long contractId;

  private LocalDateTime awardedAt;
  private String awardComments;
  private Long awardedBy;
  private LocalDateTime contractCreatedAt;


  private Long evaluatedBy;
  private String evaluationComments;
  private LocalDateTime evaluatedAt;

  private String statusReason;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private BidStatus status;

  @Column(name = "total_price", nullable = false, precision = 15, scale = 2)
  private BigDecimal totalPrice;

  @Column(name = "submission_time")
  private LocalDateTime submissionTime;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @OneToMany(mappedBy = "bid", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<com.egov.tendering.bidding.dal.model.BidItem> items = new ArrayList<>();

  @OneToMany(mappedBy = "bid", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<BidDocument> documents = new ArrayList<>();

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
    if (status == null) {
      status = BidStatus.DRAFT;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}

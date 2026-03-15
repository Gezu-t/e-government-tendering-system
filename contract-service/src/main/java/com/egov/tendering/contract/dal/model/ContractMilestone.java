package com.egov.tendering.contract.dal.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "contract_milestones")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "contract")
public class ContractMilestone {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "contract_id", nullable = false)
  private Contract contract;

  @Column(nullable = false)
  private String title;

  @Column
  private String description;

  @Column(nullable = false)
  private LocalDate dueDate;

  @Column(nullable = false)
  private BigDecimal paymentAmount;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(nullable = false)
  private MilestoneStatus status;

  @Column
  private LocalDate completedDate;
}

package com.egov.tendering.evaluation.dal.dto;

import com.egov.tendering.evaluation.dal.model.ScoreCategory;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationCategoryConfigDTO {

    private Long id;

    @NotNull(message = "Category is required")
    private ScoreCategory category;

    @NotNull(message = "Weight is required")
    @DecimalMin("0.0")
    @DecimalMax("100.0")
    private BigDecimal weight;

    @DecimalMin("0.0")
    @DecimalMax("10.0")
    private BigDecimal passThreshold;

    private Boolean mandatory;

    private String description;
}

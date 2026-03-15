package com.egov.tendering.tender.dal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenderCategoryDTO {
    private Long id;
    private String name;
    private String type;
    private String description;
    private boolean active;
}

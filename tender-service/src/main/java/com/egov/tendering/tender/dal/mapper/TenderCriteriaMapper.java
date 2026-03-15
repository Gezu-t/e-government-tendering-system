package com.egov.tendering.tender.dal.mapper;


import com.egov.tendering.tender.dal.dto.TenderCriteriaDTO;
import com.egov.tendering.tender.dal.model.TenderCriteria;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TenderCriteriaMapper {

    // Remove the 'tender' mapping since it doesn't exist in the DTO
    TenderCriteriaDTO toDto(TenderCriteria criteria);

    // For entity mapping, ignore fields that shouldn't be set directly
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    TenderCriteria toEntity(TenderCriteriaDTO criteriaDTO);

    List<TenderCriteriaDTO> toDtoList(List<TenderCriteria> criteriaList);
}
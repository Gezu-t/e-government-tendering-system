package com.egov.tendering.tender.dal.mapper;

import com.egov.tendering.tender.dal.dto.TenderCategoryDTO;
import com.egov.tendering.tender.dal.model.TenderCategory;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TenderCategoryMapper {

  TenderCategoryDTO toDTO(TenderCategory tenderCategory);

  TenderCategory toEntity(TenderCategoryDTO tenderCategoryDTO);

  List<TenderCategoryDTO> toDtoList(List<TenderCategory> tenderCategories);
}

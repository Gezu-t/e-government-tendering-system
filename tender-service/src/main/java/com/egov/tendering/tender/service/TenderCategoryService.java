package com.egov.tendering.tender.service;

import com.egov.tendering.tender.dal.dto.TenderCategoryDTO;

import java.util.List;

public interface TenderCategoryService {

  TenderCategoryDTO createCategory(TenderCategoryDTO categoryDTO);

  TenderCategoryDTO getCategoryById(Long id);

  List<TenderCategoryDTO> getAllCategories();

  TenderCategoryDTO updateCategory(Long id, TenderCategoryDTO categoryDTO);

  void deleteCategory(Long id);

  List<TenderCategoryDTO> findActiveCategories();
}

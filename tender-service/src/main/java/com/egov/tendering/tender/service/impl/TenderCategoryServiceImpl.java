package com.egov.tendering.tender.service.impl;

import com.egov.tendering.tender.dal.dto.TenderCategoryDTO;
import com.egov.tendering.tender.dal.mapper.TenderCategoryMapper;
import com.egov.tendering.tender.dal.model.TenderCategory;
import com.egov.tendering.tender.dal.repository.TenderCategoryRepository;
import com.egov.tendering.tender.exception.EntityNotFoundException;
import com.egov.tendering.tender.service.TenderCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TenderCategoryServiceImpl implements TenderCategoryService {

    private final TenderCategoryRepository tenderCategoryRepository;
    private final TenderCategoryMapper tenderCategoryMapper;

    @Override
    public TenderCategoryDTO createCategory(TenderCategoryDTO categoryDTO) {
        TenderCategory category = tenderCategoryMapper.toEntity(categoryDTO);
        category = tenderCategoryRepository.save(category);
        return tenderCategoryMapper.toDTO(category);
    }

    @Override
    public TenderCategoryDTO getCategoryById(Long id) {
        TenderCategory category = tenderCategoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tender category not found with ID: " + id));
        return tenderCategoryMapper.toDTO(category);
    }

    @Override
    public List<TenderCategoryDTO> getAllCategories() {
        List<TenderCategory> categories = tenderCategoryRepository.findAll();
        return tenderCategoryMapper.toDtoList(categories);
    }

    @Override
    public TenderCategoryDTO updateCategory(Long id, TenderCategoryDTO categoryDTO) {
        tenderCategoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tender category not found with ID: " + id));

        TenderCategory category = tenderCategoryMapper.toEntity(categoryDTO);
        category.setId(id);
        category = tenderCategoryRepository.save(category);
        return tenderCategoryMapper.toDTO(category);
    }

    @Override
    public void deleteCategory(Long id) {
        tenderCategoryRepository.deleteById(id);
    }

    @Override
    public List<TenderCategoryDTO> findActiveCategories() {
        List<TenderCategory> categories = tenderCategoryRepository.findByActiveTrue();
        return tenderCategoryMapper.toDtoList(categories);
    }
}

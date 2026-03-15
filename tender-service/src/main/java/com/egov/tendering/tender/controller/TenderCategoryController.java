package com.egov.tendering.tender.controller;

import com.egov.tendering.tender.dal.dto.TenderCategoryDTO;
import com.egov.tendering.tender.service.TenderCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/tenderCategory")
@RequiredArgsConstructor
@Slf4j
public class TenderCategoryController  {

  private final TenderCategoryService tenderCategoryService;


  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<List<TenderCategoryDTO>> getAllTenderCategories() {
    log.info("Fetching all tender categories");
    List<TenderCategoryDTO> categories = tenderCategoryService.getAllCategories();
    return ResponseEntity.ok(categories);
  }


  @GetMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<TenderCategoryDTO> getTenderCategoryById(@PathVariable Long id) {
    log.info("Fetching tender category with ID: {}", id);
    TenderCategoryDTO category = tenderCategoryService.getCategoryById(id);
    return ResponseEntity.ok(category);
  }


  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<TenderCategoryDTO> createTenderCategory(@RequestBody TenderCategoryDTO categoryDTO) {
    log.info("Creating new tender category: {}", categoryDTO.getName());
    TenderCategoryDTO createdCategory = tenderCategoryService.createCategory(categoryDTO);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdCategory);
  }


  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<TenderCategoryDTO> updateTenderCategory(
          @PathVariable Long id, @RequestBody TenderCategoryDTO categoryDTO) {
    log.info("Updating tender category with ID: {}", id);
    TenderCategoryDTO updatedCategory = tenderCategoryService.updateCategory(id, categoryDTO);
    return ResponseEntity.ok(updatedCategory);
  }


  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deleteTenderCategory(@PathVariable Long id) {
    log.info("Deleting tender category with ID: {}", id);
    tenderCategoryService.deleteCategory(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/active")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<List<TenderCategoryDTO>> getActiveCategories() {
    log.info("Fetching all active tender categories");
    List<TenderCategoryDTO> activeCategories = tenderCategoryService.findActiveCategories();
    return ResponseEntity.ok(activeCategories);
  }
}

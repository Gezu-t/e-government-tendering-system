package com.egov.tendering.contract.service;

import com.egov.tendering.contract.dal.dto.VendorPerformanceDTO;
import com.egov.tendering.contract.dal.dto.VendorPerformanceRequest;

import java.math.BigDecimal;
import java.util.List;

public interface VendorPerformanceService {

    VendorPerformanceDTO submitPerformanceReview(Long contractId, VendorPerformanceRequest request, Long reviewerId);

    List<VendorPerformanceDTO> getPerformancesByContract(Long contractId);

    List<VendorPerformanceDTO> getPerformancesByVendor(Long vendorId);

    BigDecimal getVendorAverageScore(Long vendorId);
}

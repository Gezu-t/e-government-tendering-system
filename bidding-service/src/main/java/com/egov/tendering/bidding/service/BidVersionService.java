package com.egov.tendering.bidding.service;


import com.egov.tendering.bidding.dal.dto.BidDTO;
import com.egov.tendering.bidding.dal.dto.BidVersionDTO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface BidVersionService {


    @Transactional
    BidVersionDTO saveBidVersion(Long bidId, BidDTO bidData, String changeSummary, Long createdBy);

    List<BidVersionDTO> getBidVersions(Long bidId);

    BidVersionDTO getBidVersion(Long bidId, Integer versionNumber);

    @Transactional
    BidDTO rollbackToVersion(Long bidId, Integer versionNumber);
}

package com.egov.tendering.bidding.config;

import com.egov.tendering.bidding.dal.model.BidDocument;
import com.egov.tendering.bidding.dal.repository.BidDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BidFileSecurityUtil {

    private final BidDocumentRepository bidDocumentRepository;
    private final BidAccessSecurityUtil bidAccessSecurityUtil;

    public boolean canAccessFile(String fileName) {
        Long currentUserId = bidAccessSecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            return false;
        }

        BidDocument document = bidDocumentRepository.findByFilePath(fileName).orElse(null);
        if (document == null) {
            return false;
        }

        return currentUserId.equals(document.getUploadedBy())
                || currentUserId.equals(document.getBid().getTendererId());
    }
}

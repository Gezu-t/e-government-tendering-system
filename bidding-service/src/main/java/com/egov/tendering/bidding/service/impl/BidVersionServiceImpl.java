package com.egov.tendering.bidding.service.impl;

import com.egov.tendering.bidding.dal.dto.BidDTO;
import com.egov.tendering.bidding.dal.dto.BidVersionDTO;

import com.egov.tendering.bidding.dal.mapper.BidVersionMapper;
import com.egov.tendering.bidding.dal.model.BidVersion;
import com.egov.tendering.bidding.dal.repository.BidVersionRepository;
import com.egov.tendering.bidding.exception.BidVersioningException;
import com.egov.tendering.bidding.service.BidVersionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BidVersionServiceImpl implements BidVersionService {

    private final BidVersionRepository bidVersionRepository;
    private final BidVersionMapper bidVersionMapper;
    private final ObjectMapper objectMapper;

    /**
     * Save a new version of the bid.
     */
    @Transactional
    @Override
    public BidVersionDTO saveBidVersion(Long bidId, BidDTO bidData, String changeSummary, Long createdBy) {
        try {
            Optional<BidVersion> lastVersionOpt = bidVersionRepository.findTopByBidIdOrderByVersionNumberDesc(bidId);
            int nextVersion = lastVersionOpt.map(bidVersion -> bidVersion.getVersionNumber() + 1).orElse(1);

            BidVersion bidVersion = BidVersion.builder()
                    .bidId(bidId)
                    .versionNumber(nextVersion)
                    .versionData(objectMapper.writeValueAsString(bidData))
                    .changeSummary(changeSummary)
                    .createdBy(createdBy)
                    .build();

            return bidVersionMapper.toDto(bidVersionRepository.save(bidVersion));

        } catch (JsonProcessingException e) {
            throw new BidVersioningException("Failed to serialize bid data", e);
        }
    }

    /**
     * Get all bid versions for a given bid.
     */
    @Override
    public List<BidVersionDTO> getBidVersions(Long bidId) {
        return bidVersionRepository.findByBidIdOrderByVersionNumberDesc(bidId)
                .stream()
                .map(bidVersionMapper::toDto)
                .toList();
    }

    @Override
    public BidVersionDTO getBidVersion(Long bidId, Integer versionNumber) {
        BidVersion bidVersion = bidVersionRepository.findByBidIdAndVersionNumber(bidId, versionNumber)
                .orElseThrow(() -> new IllegalArgumentException("Bid version not found"));
        return bidVersionMapper.toDto(bidVersion);
    }

    /**
     * Restore a bid to a previous version.
     */
    @Transactional
    @Override
    public BidDTO rollbackToVersion(Long bidId, Integer versionNumber) {
        BidVersion bidVersion = bidVersionRepository.findByBidIdOrderByVersionNumberDesc(bidId)
                .stream()
                .filter(v -> v.getVersionNumber().equals(versionNumber))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Bid version not found"));

        try {
            return objectMapper.readValue(bidVersion.getVersionData(), BidDTO.class);
        } catch (JsonProcessingException e) {
            throw new BidVersioningException("Failed to deserialize bid data", e);
        }
    }
}

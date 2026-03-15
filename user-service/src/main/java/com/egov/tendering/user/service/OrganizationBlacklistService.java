package com.egov.tendering.user.service;

import com.egov.tendering.user.dal.dto.BlacklistRequest;
import com.egov.tendering.user.dal.dto.OrganizationBlacklistDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrganizationBlacklistService {

    OrganizationBlacklistDTO blacklistOrganization(BlacklistRequest request, Long imposedBy);

    OrganizationBlacklistDTO liftBlacklist(Long blacklistId, String reason, Long liftedBy);

    boolean isOrganizationBlacklisted(Long organizationId);

    List<OrganizationBlacklistDTO> getBlacklistsForOrganization(Long organizationId);

    Page<OrganizationBlacklistDTO> getActiveBlacklists(Pageable pageable);

    void processExpiredBlacklists();
}

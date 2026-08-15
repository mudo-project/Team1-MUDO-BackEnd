package com.academy.mudogroupware.corporatecard.application.port;

import java.util.List;
import java.util.Map;

public interface CorporateCardApproverDirectoryPort {

    Map<Long, ApproverInfo> getApprovers(List<Long> userIds);

    record ApproverInfo(Long userId, String name, String positionName) { }
}

package com.academy.mudogroupware.approval.application.port;

import java.util.List;
import java.util.Map;

public interface ApproverDirectoryPort {

    ApproverInfo getApprover(Long userId);

    Map<Long, ApproverInfo> getApprovers(List<Long> userIds);
}

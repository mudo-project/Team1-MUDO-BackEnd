package com.academy.mudogroupware.approval.infrastructure.persistence;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.approval.application.port.ApproverDirectoryPort;
import com.academy.mudogroupware.approval.application.port.ApproverInfo;

@Component
public class ApproverDirectoryPortAdapter implements ApproverDirectoryPort {

    @Override
    public ApproverInfo getApprover(Long userId) {
        // User 모듈(도메인)이 아직 없어 연동 전까지 미구현 상태로 남겨둠
        throw new UnsupportedOperationException("User module is not integrated yet");
    }

    @Override
    public Map<Long, ApproverInfo> getApprovers(List<Long> userIds) {
        // User 모듈(도메인)이 아직 없어 연동 전까지 미구현 상태로 남겨둠
        throw new UnsupportedOperationException("User module is not integrated yet");
    }
}

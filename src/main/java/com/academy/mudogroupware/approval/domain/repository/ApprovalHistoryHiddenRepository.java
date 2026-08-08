package com.academy.mudogroupware.approval.domain.repository;

import java.time.LocalDateTime;

public interface ApprovalHistoryHiddenRepository {

    boolean exists(Long documentId, Long userId);

    void save(Long documentId, Long userId, LocalDateTime hiddenAt);
}

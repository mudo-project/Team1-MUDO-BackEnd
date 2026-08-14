package com.academy.mudogroupware.file.infrastructure.persistence;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.file.application.port.FileReferenceChecker;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JpaFileReferenceChecker implements FileReferenceChecker {

    private final EntityManager entityManager;

    @Override
    public boolean isReferenced(Long fileId) {
        return exists("notice_attachment", "file_id", fileId)
                || exists("approval_attachment", "file_id", fileId)
                || exists("template", "file_id", fileId)
                || exists("chat_message", "file_id", fileId);
    }

    private boolean exists(String tableName, String columnName, Long fileId) {
        Number count = (Number) entityManager
                .createNativeQuery("select count(*) from " + tableName + " where " + columnName + " = :fileId")
                .setParameter("fileId", fileId)
                .getSingleResult();
        return count.longValue() > 0;
    }
}

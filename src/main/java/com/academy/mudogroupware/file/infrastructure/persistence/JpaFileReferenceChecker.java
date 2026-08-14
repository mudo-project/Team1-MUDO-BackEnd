package com.academy.mudogroupware.file.infrastructure.persistence;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.file.application.port.FileReferenceChecker;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JpaFileReferenceChecker implements FileReferenceChecker {

    private final EntityManager entityManager;

    @Override
    public Set<Long> findReferencedFileIds(Collection<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return Set.of();
        }

        Set<Long> referencedFileIds = new HashSet<>();
        referencedFileIds.addAll(findReferencedIds("notice_attachment", "file_id", fileIds));
        referencedFileIds.addAll(findReferencedIds("approval_attachment", "file_id", fileIds));
        referencedFileIds.addAll(findReferencedIds("template", "file_id", fileIds));
        referencedFileIds.addAll(findReferencedIds("chat_message", "file_id", fileIds));
        return referencedFileIds;
    }

    private Set<Long> findReferencedIds(String tableName, String columnName, Collection<Long> fileIds) {
        @SuppressWarnings("unchecked")
        List<Number> referencedIds = entityManager
                .createNativeQuery("select distinct " + columnName + " from " + tableName
                        + " where " + columnName + " in (:fileIds)")
                .setParameter("fileIds", fileIds)
                .getResultList();

        Set<Long> result = new HashSet<>();
        for (Number referencedId : referencedIds) {
            result.add(referencedId.longValue());
        }
        return result;
    }
}

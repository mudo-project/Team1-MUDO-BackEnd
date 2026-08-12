package com.academy.mudogroupware.approval.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface ApprovalTemplateJpaRepository extends JpaRepository<ApprovalTemplateEntity, Long> {

    Slice<ApprovalTemplateEntity> findAllByType(String type, Pageable pageable);

    List<ApprovalTemplateEntity> findAllByIdInAndType(List<Long> ids, String type);

    Optional<ApprovalTemplateEntity> findByIdAndType(Long id, String type);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from ApprovalTemplateEntity t where t.id = :id and t.type = :type")
    Optional<ApprovalTemplateEntity> findByIdAndTypeForUpdate(
            @Param("id") Long id, @Param("type") String type);
}

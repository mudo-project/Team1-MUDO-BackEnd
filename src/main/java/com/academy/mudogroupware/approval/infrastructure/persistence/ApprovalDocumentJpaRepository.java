package com.academy.mudogroupware.approval.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApprovalDocumentJpaRepository extends JpaRepository<ApprovalDocumentEntity, Long> {

    @Query("select distinct d from ApprovalDocumentEntity d join d.lines l where l.approverId = :approverId")
    List<ApprovalDocumentEntity> findAllByApproverId(@Param("approverId") Long approverId);

    List<ApprovalDocumentEntity> findAllByCreatorId(Long creatorId);
}

package com.academy.mudogroupware.approval.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApprovalTemplateJpaRepository extends JpaRepository<ApprovalTemplateEntity, Long> {

    @Query("select distinct t from ApprovalTemplateEntity t join t.approvalLines l where l.approverId = :approverId")
    List<ApprovalTemplateEntity> findAllByApproverId(@Param("approverId") Long approverId);
}

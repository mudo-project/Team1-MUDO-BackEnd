package com.academy.mudogroupware.approval.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalTemplateJpaRepository extends JpaRepository<ApprovalTemplateEntity, Long> {

    List<ApprovalTemplateEntity> findAllByTypeAndAcademyId(String type, Long academyId);
}

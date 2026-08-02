package com.academy.mudogroupware.approval.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalTemplateJpaRepository extends JpaRepository<ApprovalTemplateEntity, Long> {
}

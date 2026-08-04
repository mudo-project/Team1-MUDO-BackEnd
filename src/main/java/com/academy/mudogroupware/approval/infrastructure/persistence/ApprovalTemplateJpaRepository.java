package com.academy.mudogroupware.approval.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalTemplateJpaRepository extends JpaRepository<ApprovalTemplateEntity, Long> {

    Slice<ApprovalTemplateEntity> findAllByTypeAndAcademyId(String type, Long academyId, Pageable pageable);
}

package com.academy.mudogroupware.workspace.infrastructure.persistence.task;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurringTaskTemplateJpaRepository
    extends JpaRepository<RecurringTaskTemplateJpaEntity, Long> {}

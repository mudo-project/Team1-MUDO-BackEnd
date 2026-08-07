package com.academy.mudogroupware.workspace.infrastructure.persistence.task;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurringTaskSkipJpaRepository
    extends JpaRepository<RecurringTaskSkipJpaEntity, RecurringTaskSkipId> {}

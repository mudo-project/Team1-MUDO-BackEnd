package com.academy.mudogroupware.workspace.infrastructure.persistence.task;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskStatusHistoryJpaRepository extends JpaRepository<TaskStatusHistoryJpaEntity, Long> {}

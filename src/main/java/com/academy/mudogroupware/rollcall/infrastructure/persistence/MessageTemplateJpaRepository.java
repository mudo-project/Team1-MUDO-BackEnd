package com.academy.mudogroupware.rollcall.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.academy.mudogroupware.rollcall.domain.model.AttendanceStatus;

public interface MessageTemplateJpaRepository extends JpaRepository<MessageTemplateEntity, Long> {

    List<MessageTemplateEntity> findAllByOrderByIdAsc();

    Optional<MessageTemplateEntity> findByStatus(AttendanceStatus status);
}

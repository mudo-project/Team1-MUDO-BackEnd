package com.academy.mudogroupware.rollcall.domain.repository;

import java.util.List;
import java.util.Optional;

import com.academy.mudogroupware.rollcall.domain.model.AttendanceStatus;
import com.academy.mudogroupware.rollcall.domain.model.MessageTemplate;

public interface MessageTemplateRepository {

    Optional<MessageTemplate> findById(Long id);

    List<MessageTemplate> findByAcademyId(Long academyId);

    Optional<MessageTemplate> findByAcademyIdAndStatus(Long academyId, AttendanceStatus status);

    MessageTemplate save(MessageTemplate template);

    void deleteById(Long id);
}

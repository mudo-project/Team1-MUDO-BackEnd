package com.academy.mudogroupware.rollcall.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.rollcall.domain.model.AttendanceStatus;
import com.academy.mudogroupware.rollcall.domain.model.MessageTemplate;
import com.academy.mudogroupware.rollcall.domain.repository.MessageTemplateRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MessageTemplateRepositoryImpl implements MessageTemplateRepository {

    private final MessageTemplateJpaRepository messageTemplateJpaRepository;

    @Override
    public Optional<MessageTemplate> findById(Long id) {
        return messageTemplateJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<MessageTemplate> findByAcademyId(Long academyId) {
        return messageTemplateJpaRepository.findAllByAcademyId(academyId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<MessageTemplate> findByAcademyIdAndStatus(Long academyId, AttendanceStatus status) {
        return messageTemplateJpaRepository.findByAcademyIdAndStatus(academyId, status).map(this::toDomain);
    }

    @Override
    public MessageTemplate save(MessageTemplate template) {
        MessageTemplateEntity entity = template.getId() != null ? updateExisting(template) : toNewEntity(template);
        return toDomain(messageTemplateJpaRepository.save(entity));
    }

    @Override
    public void deleteById(Long id) {
        messageTemplateJpaRepository.deleteById(id);
    }

    private MessageTemplateEntity toNewEntity(MessageTemplate template) {
        return MessageTemplateEntity.builder()
                .academyId(template.getAcademyId())
                .name(template.getName())
                .status(template.getStatus())
                .content(template.getContent())
                .createdBy(template.getCreatedBy())
                .build();
    }

    private MessageTemplateEntity updateExisting(MessageTemplate template) {
        MessageTemplateEntity entity = messageTemplateJpaRepository.getReferenceById(template.getId());
        entity.update(template.getName(), template.getContent());
        return entity;
    }

    private MessageTemplate toDomain(MessageTemplateEntity entity) {
        return MessageTemplate.restore(entity.getId(), entity.getAcademyId(), entity.getName(), entity.getStatus(),
                entity.getContent(), entity.getCreatedBy(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}

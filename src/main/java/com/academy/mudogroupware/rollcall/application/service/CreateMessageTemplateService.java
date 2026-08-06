package com.academy.mudogroupware.rollcall.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.rollcall.application.command.CreateMessageTemplateCommand;
import com.academy.mudogroupware.rollcall.application.usecase.CreateMessageTemplateUseCase;
import com.academy.mudogroupware.rollcall.domain.exception.MessageTemplateStatusConflictException;
import com.academy.mudogroupware.rollcall.domain.model.MessageTemplate;
import com.academy.mudogroupware.rollcall.domain.repository.MessageTemplateRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateMessageTemplateService implements CreateMessageTemplateUseCase {

    private final MessageTemplateRepository messageTemplateRepository;
    private final Clock clock;

    @Override
    public Long createTemplate(CreateMessageTemplateCommand command) {
        boolean alreadyExists = messageTemplateRepository
                .findByAcademyIdAndStatus(command.academyId(), command.status())
                .isPresent();
        if (alreadyExists) {
            throw new MessageTemplateStatusConflictException();
        }

        LocalDateTime now = LocalDateTime.now(clock);
        MessageTemplate template = MessageTemplate.create(command.academyId(), command.name(), command.status(),
                command.content(), command.createdBy(), now);
        return messageTemplateRepository.save(template).getId();
    }
}

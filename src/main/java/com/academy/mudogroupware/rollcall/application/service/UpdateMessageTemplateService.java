package com.academy.mudogroupware.rollcall.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.rollcall.application.command.UpdateMessageTemplateCommand;
import com.academy.mudogroupware.rollcall.application.usecase.UpdateMessageTemplateUseCase;
import com.academy.mudogroupware.rollcall.domain.exception.MessageTemplateNotFoundException;
import com.academy.mudogroupware.rollcall.domain.model.MessageTemplate;
import com.academy.mudogroupware.rollcall.domain.repository.MessageTemplateRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateMessageTemplateService implements UpdateMessageTemplateUseCase {

    private final MessageTemplateRepository messageTemplateRepository;
    private final Clock clock;

    @Override
    public void updateTemplate(UpdateMessageTemplateCommand command) {
        MessageTemplate template = messageTemplateRepository.findById(command.templateId())
                .filter(t -> t.getAcademyId().equals(command.academyId()))
                .orElseThrow(MessageTemplateNotFoundException::new);

        template.update(command.name(), command.content(), LocalDateTime.now(clock));
        messageTemplateRepository.save(template);
    }
}

package com.academy.mudogroupware.rollcall.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.rollcall.application.usecase.DeleteMessageTemplateUseCase;
import com.academy.mudogroupware.rollcall.domain.exception.MessageTemplateNotFoundException;
import com.academy.mudogroupware.rollcall.domain.model.MessageTemplate;
import com.academy.mudogroupware.rollcall.domain.repository.MessageTemplateRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DeleteMessageTemplateService implements DeleteMessageTemplateUseCase {

    private final MessageTemplateRepository messageTemplateRepository;

    @Override
    public void deleteTemplate(Long templateId, Long academyId) {
        MessageTemplate template = messageTemplateRepository.findById(templateId)
                .filter(t -> t.getAcademyId().equals(academyId))
                .orElseThrow(MessageTemplateNotFoundException::new);

        messageTemplateRepository.deleteById(template.getId());
    }
}

package com.academy.mudogroupware.rollcall.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.rollcall.application.query.MessageTemplateView;
import com.academy.mudogroupware.rollcall.application.usecase.MessageTemplateQueryUseCase;
import com.academy.mudogroupware.rollcall.domain.repository.MessageTemplateRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageTemplateQueryService implements MessageTemplateQueryUseCase {

    private final MessageTemplateRepository messageTemplateRepository;

    @Override
    public List<MessageTemplateView> getTemplates() {
        return messageTemplateRepository.findAll().stream()
                .map(template -> new MessageTemplateView(template.getId(), template.getName(), template.getStatus(),
                        template.getContent(), template.getCreatedAt(), template.getUpdatedAt()))
                .toList();
    }
}

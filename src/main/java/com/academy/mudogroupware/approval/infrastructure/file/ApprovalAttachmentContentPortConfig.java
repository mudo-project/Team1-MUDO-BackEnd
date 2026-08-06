package com.academy.mudogroupware.approval.infrastructure.file;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.academy.mudogroupware.approval.application.port.AttachmentContentPort;

@Configuration
public class ApprovalAttachmentContentPortConfig {

    @Bean
    @ConditionalOnMissingBean(AttachmentContentPort.class)
    public AttachmentContentPort unavailableAttachmentContentAdapter() {
        return new UnavailableAttachmentContentAdapter();
    }
}

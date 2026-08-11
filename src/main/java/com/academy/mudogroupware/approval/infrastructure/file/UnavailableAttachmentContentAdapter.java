package com.academy.mudogroupware.approval.infrastructure.file;

import com.academy.mudogroupware.approval.application.port.AttachmentContent;
import com.academy.mudogroupware.approval.application.port.AttachmentContentPort;
import com.academy.mudogroupware.approval.application.port.AttachmentContentUnavailableException;

public class UnavailableAttachmentContentAdapter implements AttachmentContentPort {

    @Override
    public AttachmentContent loadContent(Long fileId) {
        throw new AttachmentContentUnavailableException(fileId);
    }
}

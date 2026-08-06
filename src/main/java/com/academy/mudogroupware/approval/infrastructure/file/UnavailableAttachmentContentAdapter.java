package com.academy.mudogroupware.approval.infrastructure.file;

import com.academy.mudogroupware.approval.application.port.AttachmentContentPort;
import com.academy.mudogroupware.approval.application.port.AttachmentContentUnavailableException;

public class UnavailableAttachmentContentAdapter implements AttachmentContentPort {

    @Override
    public String loadContent(Long fileId) {
        throw new AttachmentContentUnavailableException(fileId);
    }
}

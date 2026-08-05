package com.academy.mudogroupware.rollcall.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;

public class EtcNoteRequiredException extends BadRequestException {

    public EtcNoteRequiredException() {
        super(RollcallErrorCode.ETC_NOTE_REQUIRED);
    }
}

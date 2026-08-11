package com.academy.mudogroupware.notice.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NoticeErrorCode implements ErrorCode {

    TITLE_REQUIRED(HttpStatus.BAD_REQUEST, "NOTICE_400_1", "공지 제목은 비어 있을 수 없습니다."),
    CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, "NOTICE_400_2", "공지 내용은 비어 있을 수 없습니다."),

    NOT_AUTHOR_UPDATE(HttpStatus.FORBIDDEN, "NOTICE_403_2", "작성자 본인만 공지사항을 수정할 수 있습니다."),
    NOT_AUTHOR_DELETE(HttpStatus.FORBIDDEN, "NOTICE_403_3", "작성자 본인만 공지사항을 삭제할 수 있습니다."),

    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTICE_404_1", "공지사항을 찾을 수 없습니다."),
    AUTHOR_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTICE_404_2", "사용자를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

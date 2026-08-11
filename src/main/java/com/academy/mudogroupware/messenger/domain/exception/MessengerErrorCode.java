package com.academy.mudogroupware.messenger.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MessengerErrorCode implements ErrorCode {

    PARTICIPANT_REQUIRED(HttpStatus.BAD_REQUEST, "MESSENGER_400_1", "참여자를 최소 1명 이상 지정해야 합니다."),
    INVITEE_REQUIRED(HttpStatus.BAD_REQUEST, "MESSENGER_400_2", "초대할 참여자를 최소 1명 이상 선택해야 합니다."),
    SELF_INVITE_ONLY(HttpStatus.BAD_REQUEST, "MESSENGER_400_3", "본인 외에 최소 1명 이상 초대해야 합니다."),
    GROUP_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "MESSENGER_400_4", "그룹 채팅방은 이름을 지정해야 합니다."),
    MESSAGE_CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, "MESSENGER_400_5", "메시지 내용은 비어 있을 수 없습니다."),
    FILE_ID_REQUIRED(HttpStatus.BAD_REQUEST, "MESSENGER_400_6", "첨부파일(fileId)이 지정되지 않았습니다."),
    INVALID_PARTICIPANT(HttpStatus.BAD_REQUEST, "MESSENGER_400_7", "존재하지 않는 참여자가 포함되어 있습니다."),
    INVALID_CURSOR(HttpStatus.BAD_REQUEST, "MESSENGER_400_11",
            "cursorCreatedAt과 cursorMessageId는 함께 전달하거나 함께 생략해야 합니다."),
    INVALID_PAGE_SIZE(HttpStatus.BAD_REQUEST, "MESSENGER_400_12", "메시지 조회 size는 1 이상 100 이하여야 합니다."),

    TASK_CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, "MESSENGER_400_9", "업무지시 내용은 비어 있을 수 없습니다."),
    ASSIGNEE_REQUIRED(HttpStatus.BAD_REQUEST, "MESSENGER_400_10", "담당자를 최소 1명 이상 지정해야 합니다."),
    INVALID_ASSIGNEE(HttpStatus.BAD_REQUEST, "MESSENGER_400_13", "유효하지 않은 담당자가 포함되어 있습니다."),
    NOT_TEXT_MESSAGE(HttpStatus.BAD_REQUEST, "MESSENGER_400_14", "TEXT 메시지만 수정할 수 있습니다."),
    MESSAGE_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "MESSENGER_400_15", "이미 삭제된 메시지입니다."),
    TASK_CARD_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "MESSENGER_400_16", "이미 삭제된 업무지시입니다."),
    INVALID_TASK_CARD_CURSOR(HttpStatus.BAD_REQUEST, "MESSENGER_400_17",
            "cursorCreatedAt과 cursorCardId는 함께 전달하거나 함께 생략해야 합니다."),
    INVALID_TASK_CARD_PAGE_SIZE(HttpStatus.BAD_REQUEST, "MESSENGER_400_18",
            "업무지시 카드 조회 size는 1 이상 100 이하여야 합니다."),

    NOT_ROOM_MEMBER(HttpStatus.FORBIDDEN, "MESSENGER_403_1", "채팅방 참여자가 아닙니다."),
    NOT_TASK_ASSIGNEE(HttpStatus.FORBIDDEN, "MESSENGER_403_2", "해당 업무지시의 담당자가 아닙니다."),
    NOT_TASK_CARD_OWNER(HttpStatus.FORBIDDEN, "MESSENGER_403_3", "본인이 등록한 업무지시만 수정/삭제할 수 있습니다."),

    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "MESSENGER_404_1", "채팅방을 찾을 수 없습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MESSENGER_404_2", "사용자를 찾을 수 없습니다."),
    TASK_CARD_NOT_FOUND(HttpStatus.NOT_FOUND, "MESSENGER_404_3", "업무지시 카드를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

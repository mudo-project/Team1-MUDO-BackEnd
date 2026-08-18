package com.academy.mudogroupware.messenger.presentation.api.common;

import com.academy.mudogroupware.global.presentation.api.common.ResponseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MessengerResponseCode implements ResponseCode {

    CHAT_ROOM_CREATED("MESSENGER_201_1", "채팅방 생성에 성공했습니다."),
    CHAT_ROOM_LIST_RETRIEVED("MESSENGER_200_1", "채팅방 목록 조회에 성공했습니다."),
    CHAT_ROOM_MEMBERS_RETRIEVED("MESSENGER_200_2", "채팅방 참여자 조회에 성공했습니다."),
    MESSAGE_SENT("MESSENGER_201_2", "메시지 전송에 성공했습니다."),
    MESSAGE_LIST_RETRIEVED("MESSENGER_200_3", "메시지 목록 조회에 성공했습니다."),
    TASK_CARD_CREATED("MESSENGER_201_3", "업무지시 카드 등록에 성공했습니다."),
    TASK_CARD_LIST_RETRIEVED("MESSENGER_200_4", "업무지시 카드 목록 조회에 성공했습니다."),
    MY_TASK_CARD_LIST_RETRIEVED("MESSENGER_200_5", "내 업무지시 카드 목록 조회에 성공했습니다.");

    private final String code;
    private final String message;
}

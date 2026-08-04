package com.academy.mudogroupware.messenger.domain.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.academy.mudogroupware.messenger.domain.model.ChatMessage;

public interface ChatMessageRepository {

    ChatMessage save(ChatMessage chatMessage);

    // createdAt 내림차순(최신순)으로 size + 1건을 반환한다. 호출측(application)이 size + 1건째의
    // 존재 여부로 hasNext를 판단한 뒤 size건으로 잘라내는 방식으로, 채팅 기록이 무한히 쌓이는
    // 특성 때문에 approval/notice의 전체 List 반환 컨벤션과 달리 messenger 메시지만 페이지네이션한다.
    List<ChatMessage> findByChatRoomId(Long chatRoomId, int page, int size);

    // after가 null이면 한 번도 읽지 않은 상태이므로 전체 메시지 수를 반환한다.
    long countUnread(Long chatRoomId, LocalDateTime after);
}

package com.academy.mudogroupware.messenger.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.academy.mudogroupware.messenger.domain.model.ChatMessage;

public interface ChatMessageRepository {

    ChatMessage save(ChatMessage chatMessage);

    // 채팅방별 가장 최근(created_at 기준) 메시지 1건. 목록조회의 미리보기 텍스트에 사용한다.
    Map<Long, ChatMessage> findLatestByChatRoomIds(List<Long> chatRoomIds);

    // created_at DESC, message_id DESC 정렬로 (cursorCreatedAt, cursorMessageId)보다 앞선 size + 1건을 반환한다.
    // cursor가 둘 다 null이면 최신 메시지부터 조회한다. 호출측(application)이 size + 1건째의 존재 여부로
    // hasNext를 판단한 뒤 size건으로 잘라내는 방식으로, 채팅 기록이 무한히 쌓이는 특성 때문에
    // approval/notice의 전체 List 반환 컨벤션과 달리 messenger 메시지만 페이지네이션한다. offset 기반 page가
    // 아니라 cursor를 쓰는 이유는, 조회 중 새 메시지가 쌓여도 페이지 간 중복·누락이 생기지 않기 때문이다.
    List<ChatMessage> findByChatRoomId(Long chatRoomId, LocalDateTime cursorCreatedAt, Long cursorMessageId,
                                        int size);

    // 요청자가 참여 중인 채팅방들의 안읽은 메시지 수를 한 번에 집계한다(방 개수만큼 쿼리하는 N+1 방지).
    Map<Long, Long> countUnreadByRequester(Long userId, List<Long> chatRoomIds);
}

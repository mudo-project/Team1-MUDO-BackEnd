# Messenger Changelog

## 2026-08-06 · 메시지·업무지시 카드 수정/삭제 실시간 반영

- 메시지를 수정하거나 삭제하면 같은 채팅방 멤버 전체에게 실시간으로 반영됩니다.
- 업무지시 카드 등록자 본인이 카드 내용(제목/마감일/담당자)을 수정하거나 삭제할 수 있고, 같은 채팅방 멤버 전체에게 실시간으로 반영됩니다.
- 담당자 목록 변경은 추가/삭제분만 반영해, 다른 담당자가 동시에 완료 처리하더라도 서로의 변경이 유실되지 않습니다.
- 이미 삭제된 업무지시 카드를 다시 수정하려 하면 오류로 막고, 다시 삭제를 요청하면 조용히 무시(멱등 처리)합니다.

자세한 내용은 [REVISION.md](REVISION.md)를 참고해주세요.

## 2026-08-06 · 업무지시 카드 실시간 반영

- 업무지시 카드를 등록하면 같은 채팅방의 멤버 전체(등록자 본인 포함)에게 실시간으로 카드가 보입니다.
- 담당자가 업무지시를 완료 처리하면 같은 채팅방의 멤버 전체(완료 처리자 본인 포함)에게 실시간으로 완료 현황(진행률)이 갱신됩니다.
- 메시지 전송과 마찬가지로 채팅방을 보고 있을 때만 실시간이며, 업무 탭(받은업무/전달한업무)은 별도 알림 없이 들어갈 때 다시 조회합니다.

자세한 내용은 [REVISION.md](REVISION.md)를 참고해주세요.

## 2026-08-05 - Realtime and message controls

- Reused existing DM rooms to prevent duplicate 1:1 conversations.
- Added sender-only text message editing.
- Added sender-only soft delete for messages.
- Added per-message `unreadCount` for KakaoTalk-style read number display.
- Added WebSocket broadcasts for message sent and room read events on `/topic/messenger/rooms/{roomId}`.
- Kept notification work out of a new package; messenger uses existing global WebSocket infrastructure.

## 2026-08-05 · 코드 리뷰 보완

- 채팅방과 메시지 생성 시각이 항상 한국 시간 기준으로 저장되도록 고쳤습니다.
- 내가 보낸 메시지가 내 unread badge에 포함되지 않도록 고쳤습니다.
- 퇴사/비활성 사용자가 채팅방 초대 대상이나 참여자 조회 결과로 통과하지 않도록 보완했습니다.
- 메시지 목록 조회 크기는 최대 100개로 제한됩니다.
- 참여자/담당자 ID 목록에 비어 있거나 잘못된 값이 들어오면 명확한 오류로 막습니다.
- 채팅방 멤버와 업무지시 담당자 조회 시 불필요한 추가 조회가 늘어나는 문제를 줄였습니다.

자세한 내용은 [REVISION.md](REVISION.md)를 참고해주세요.


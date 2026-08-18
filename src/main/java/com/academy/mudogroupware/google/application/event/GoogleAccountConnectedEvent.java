package com.academy.mudogroupware.google.application.event;

// 계정 연동 성공(최초 연결·재연동·계정 교체 공통) 시 발행된다. 예전에는 이 이벤트가 "계정이
// 바뀌었는지"를 boolean으로 미리 계산해서 넘겼는데, 그 계산이 googleAccountConnectionRepository의
// 삭제 시점에 의존해서 연동 해제 후 재연동하면 항상 false로 오판하는 버그가 있었다(연동 해제가 비교
// 대상 자체를 지워버림). 그래서 판단에 필요한 원본 사실(연결된 이메일)만 전달하고, "바뀌었는지"는
// 각 구독자가 자기 도메인 상태와 직접 비교해서 스스로 판단하게 한다.
public record GoogleAccountConnectedEvent(String googleEmail) {
}

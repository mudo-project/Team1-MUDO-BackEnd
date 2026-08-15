package com.academy.mudogroupware.sharedfile.application.usecase;

import com.academy.mudogroupware.sharedfile.application.query.SharedFileItemView;

// 이름 변경과 이동을 하나의 작업 단위로 처리한다. name·newParentId 둘 다 null이 아닐 수 있고, 각각
// null이면(또는 공백이면) 해당 필드는 변경하지 않는다 — 최소 하나는 호출자(Controller)가 보장한다.
public interface UpdateSharedFileItemUseCase {

    SharedFileItemView update(String itemId, String name, String newParentId);
}

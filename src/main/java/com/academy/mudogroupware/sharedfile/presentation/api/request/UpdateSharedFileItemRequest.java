package com.academy.mudogroupware.sharedfile.presentation.api.request;

import io.swagger.v3.oas.annotations.media.Schema;

// 이름 변경과 이동을 하나의 PATCH로 받는다. name만 있으면 이름 변경만, parentId만 있으면 이동만,
// 둘 다 있으면 순서대로 둘 다 적용한다. 둘 다 없는 요청은 Controller가 거부한다.
public record UpdateSharedFileItemRequest(
        @Schema(description = "새 이름. 변경하지 않으려면 생략합니다.", example = "10월 수업계획.docx")
        String name,
        @Schema(description = "이동할 상위 폴더 ID. 이동하지 않으려면 생략합니다.", example = "1AbCdEfGhIjKlMnOpQrStUvWxYz")
        String parentId
) {

    public boolean hasAnyChange() {
        return (name != null && !name.isBlank()) || (parentId != null && !parentId.isBlank());
    }
}

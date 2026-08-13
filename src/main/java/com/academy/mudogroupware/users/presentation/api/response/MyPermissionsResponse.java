package com.academy.mudogroupware.users.presentation.api.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public record MyPermissionsResponse(
        @Schema(description = "이 계정이 최종적으로 갖는 권한 코드 전체 목록(합성 권한 포함). "
                + "서버가 @PreAuthorize로 실제 검사하는 것과 동일한 값이다.",
                example = "[\"ACCOUNT:MANAGE\", \"ROLE:MANAGE\"]")
        List<String> permissions
) {
    public static MyPermissionsResponse from(List<String> permissions) {
        return new MyPermissionsResponse(permissions);
    }
}

package com.academy.mudogroupware.workspace.presentation.api.response.workspace;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Set;
import lombok.Builder;

@Builder
public record WorkspaceMemberAddResponse(
    @Schema(description = "새로 추가된 참여자 사용자 번호 목록 (이미 참여 중이던 사용자는 제외)", example = "[12, 25]")
        List<Long> addedMemberIds
) {

  public static WorkspaceMemberAddResponse from(Set<Long> addedMemberIds) {
    return WorkspaceMemberAddResponse.builder().addedMemberIds(addedMemberIds.stream().toList()).build();
  }
}

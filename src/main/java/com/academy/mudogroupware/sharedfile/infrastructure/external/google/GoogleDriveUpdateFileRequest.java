package com.academy.mudogroupware.sharedfile.infrastructure.external.google;

import com.fasterxml.jackson.annotation.JsonInclude;

// files.update(PATCH) 요청 본문. rename은 name만, trash는 trashed만 채워서 보내고
// 나머지 필드는 NON_NULL로 생략한다(move는 본문 없이 쿼리 파라미터만 사용해 이 DTO를 쓰지 않는다).
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GoogleDriveUpdateFileRequest(String name, Boolean trashed) {
}

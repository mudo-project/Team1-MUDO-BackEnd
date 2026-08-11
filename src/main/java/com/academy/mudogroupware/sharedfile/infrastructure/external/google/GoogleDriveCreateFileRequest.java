package com.academy.mudogroupware.sharedfile.infrastructure.external.google;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

// files.create 요청 본문. parents가 null이면 필드 자체를 생략해(NON_NULL) Drive 최상위에 생성되게 한다.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GoogleDriveCreateFileRequest(String name, String mimeType, List<String> parents) {
}

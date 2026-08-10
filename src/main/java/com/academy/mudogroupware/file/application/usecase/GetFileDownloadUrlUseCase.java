package com.academy.mudogroupware.file.application.usecase;

import java.util.List;
import java.util.Map;

public interface GetFileDownloadUrlUseCase {

    // academyId는 요청자 인증 정보에서 가져온다(클라이언트가 값을 넣지 않음).
    // 다른 학원 소속 fileId는 존재해도 찾을 수 없는 것으로 취급한다(FILE_404_1) — IDOR 방지.
    String getDownloadUrl(Long fileId, Long academyId);

    // 존재하지 않거나 다른 학원 소속인 fileId는 결과 Map에서 조용히 빠진다(전체 요청을 실패시키지 않음).
    // 메시지 목록처럼 여러 첨부파일을 한 번에 표시해야 하는 화면에서 fileId 개수만큼
    // 반복 호출하지 않도록 배치로 조회한다.
    Map<Long, String> getDownloadUrls(List<Long> fileIds, Long academyId);
}

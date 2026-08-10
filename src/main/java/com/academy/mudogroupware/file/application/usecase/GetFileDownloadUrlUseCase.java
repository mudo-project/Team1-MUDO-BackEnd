package com.academy.mudogroupware.file.application.usecase;

import java.util.List;
import java.util.Map;

public interface GetFileDownloadUrlUseCase {

    String getDownloadUrl(Long fileId);

    // 존재하지 않는 fileId는 결과 Map에서 조용히 빠진다(전체 요청을 실패시키지 않음).
    // 메시지 목록처럼 여러 첨부파일을 한 번에 표시해야 하는 화면에서 fileId 개수만큼
    // 반복 호출하지 않도록 배치로 조회한다.
    Map<Long, String> getDownloadUrls(List<Long> fileIds);
}

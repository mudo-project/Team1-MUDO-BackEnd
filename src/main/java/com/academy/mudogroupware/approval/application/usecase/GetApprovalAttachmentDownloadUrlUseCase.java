package com.academy.mudogroupware.approval.application.usecase;

import com.academy.mudogroupware.approval.application.command.GetApprovalAttachmentDownloadUrlCommand;

public interface GetApprovalAttachmentDownloadUrlUseCase {

    // 결재 문서의 신청자 또는 결재선 참여자만 그 문서에 속한 fileId의 다운로드 URL을 받을 수 있다.
    // file 모듈의 GET /api/files/{fileId}/download-url은 인증된 사용자면 fileId만으로 누구나
    // 호출할 수 있으므로, 이 UseCase가 결재 문서 접근 권한(신청자/결재선 참여자) + 첨부파일 소유
    // 문서 일치를 먼저 검증한 뒤에만 실제 presigned URL 발급을 위임한다.
    String getDownloadUrl(GetApprovalAttachmentDownloadUrlCommand command);
}

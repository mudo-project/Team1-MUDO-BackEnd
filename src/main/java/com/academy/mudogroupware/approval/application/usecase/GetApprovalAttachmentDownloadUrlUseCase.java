package com.academy.mudogroupware.approval.application.usecase;

import com.academy.mudogroupware.approval.application.command.GetApprovalAttachmentDownloadUrlCommand;

public interface GetApprovalAttachmentDownloadUrlUseCase {

    // 결재 문서의 신청자 또는 결재선 참여자만 그 문서에 속한 fileId의 다운로드 URL을 받을 수 있다.
    // file 모듈의 GET /api/files/{fileId}/download-url은 academyId만 검증하므로,
    // 같은 학원 소속이기만 하면 결재선과 무관한 사람도 기밀 첨부파일을 열람할 수 있는 문제가 있었다.
    // 이 UseCase가 결재 문서 접근 권한(신청자/결재선 참여자) + 첨부파일 소유 문서 일치를 먼저 검증한 뒤에만
    // 실제 presigned URL 발급을 위임한다.
    String getDownloadUrl(GetApprovalAttachmentDownloadUrlCommand command);
}

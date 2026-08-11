package com.academy.mudogroupware.sharedfile.domain.model;

// 배포 단위(학원)당 하나만 존재하는 공유파일 시스템 루트의 현재 상태를 표현한다.
// 파일·폴더 목록은 담지 않고, Drive 루트 폴더 ID와 READY/FAILED 상태만 가진다.
public final class SharedFileRoot {

    private SharedFileRootStatus status;
    private String googleRootFolderId;

    private SharedFileRoot(SharedFileRootStatus status, String googleRootFolderId) {
        this.status = status;
        this.googleRootFolderId = googleRootFolderId;
    }

    // Drive에 루트 폴더 생성이 성공했을 때 사용한다.
    public static SharedFileRoot ready(String googleRootFolderId) {
        requireNonBlankFolderId(googleRootFolderId);
        return new SharedFileRoot(SharedFileRootStatus.READY, googleRootFolderId);
    }

    // 최초 생성 또는 재생성이 실패했을 때 사용한다. 폴더 ID는 갖지 않는다.
    public static SharedFileRoot failed() {
        return new SharedFileRoot(SharedFileRootStatus.FAILED, null);
    }

    // Drive에서 루트 폴더의 404(실제 삭제)가 확인됐을 때 FAILED로 전환하고 이전 폴더 ID를 지운다.
    public void markFailed() {
        this.status = SharedFileRootStatus.FAILED;
        this.googleRootFolderId = null;
    }

    // 계정 교체 또는 FAILED 루트 재생성이 성공했을 때, 새 Drive 폴더 ID로 READY 전환한다.
    public void replaceWith(String googleRootFolderId) {
        requireNonBlankFolderId(googleRootFolderId);
        this.status = SharedFileRootStatus.READY;
        this.googleRootFolderId = googleRootFolderId;
    }

    public boolean isReady() {
        return status == SharedFileRootStatus.READY;
    }

    public SharedFileRootStatus getStatus() {
        return status;
    }

    public String getGoogleRootFolderId() {
        return googleRootFolderId;
    }

    private static void requireNonBlankFolderId(String googleRootFolderId) {
        if (googleRootFolderId == null || googleRootFolderId.isBlank()) {
            throw new IllegalArgumentException("googleRootFolderId must not be null or blank");
        }
    }
}

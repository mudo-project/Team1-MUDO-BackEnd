package com.academy.mudogroupware.sharedfile.domain.model;

// 배포 단위(학원)당 하나만 존재하는 공유파일 시스템 루트의 현재 상태를 표현한다.
// 파일·폴더 목록은 담지 않고, Drive 루트 폴더 ID·현재 연결된 Google 계정 이메일·READY/FAILED 상태만 가진다.
// connectedGoogleEmail은 "지금 이 루트 폴더가 어느 계정 소유인지"를 이 도메인 스스로 판단할 수 있게 해서,
// google 도메인이 계산해준 값(예: 계정 변경 여부)에 의존하지 않게 한다.
public final class SharedFileRoot {

    private SharedFileRootStatus status;
    private String googleRootFolderId;
    private String connectedGoogleEmail;
    // 영속성 계층에서 읽어온 낙관적 락 버전. 아직 저장된 적 없는 인스턴스(ready/failed)는 null이며,
    // SharedFileRootPersistenceAdapter는 이 값으로 insert(null)와 update(non-null)를 구분한다.
    private final Long version;

    private SharedFileRoot(SharedFileRootStatus status, String googleRootFolderId, String connectedGoogleEmail,
            Long version) {
        this.status = status;
        this.googleRootFolderId = googleRootFolderId;
        this.connectedGoogleEmail = connectedGoogleEmail;
        this.version = version;
    }

    // Drive에 루트 폴더 생성이 성공했을 때 사용한다. 아직 저장되지 않은 인스턴스이므로 version은 null이다.
    public static SharedFileRoot ready(String googleRootFolderId, String connectedGoogleEmail) {
        requireNonBlankFolderId(googleRootFolderId);
        return new SharedFileRoot(SharedFileRootStatus.READY, googleRootFolderId, connectedGoogleEmail, null);
    }

    // 최초 생성 또는 재생성이 실패했을 때 사용한다. 폴더 ID·연결 이메일 모두 갖지 않는다.
    // 아직 저장되지 않은 인스턴스이므로 version은 null이다.
    public static SharedFileRoot failed() {
        return new SharedFileRoot(SharedFileRootStatus.FAILED, null, null, null);
    }

    // 영속성 계층에서 읽은 상태를 그대로 복원한다. version을 보존해야 이후 저장 시 낙관적 락 충돌을
    // 감지할 수 있으므로, SharedFileRootPersistenceAdapter의 조회 경로에서만 사용한다.
    public static SharedFileRoot restore(SharedFileRootStatus status, String googleRootFolderId,
            String connectedGoogleEmail, Long version) {
        return new SharedFileRoot(status, googleRootFolderId, connectedGoogleEmail, version);
    }

    // Drive에서 루트 폴더의 404(실제 삭제)가 확인됐을 때 FAILED로 전환하고 이전 폴더 ID·연결 이메일을 지운다.
    public void markFailed() {
        this.status = SharedFileRootStatus.FAILED;
        this.googleRootFolderId = null;
        this.connectedGoogleEmail = null;
    }

    // 계정 교체 또는 FAILED 루트 재생성이 성공했을 때, 새 Drive 폴더 ID·연결 이메일로 READY 전환한다.
    public void replaceWith(String googleRootFolderId, String connectedGoogleEmail) {
        requireNonBlankFolderId(googleRootFolderId);
        this.status = SharedFileRootStatus.READY;
        this.googleRootFolderId = googleRootFolderId;
        this.connectedGoogleEmail = connectedGoogleEmail;
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

    public String getConnectedGoogleEmail() {
        return connectedGoogleEmail;
    }

    public Long getVersion() {
        return version;
    }

    private static void requireNonBlankFolderId(String googleRootFolderId) {
        if (googleRootFolderId == null || googleRootFolderId.isBlank()) {
            throw new IllegalArgumentException("googleRootFolderId must not be null or blank");
        }
    }
}

package com.academy.mudogroupware.sharedfile.infrastructure.persistence;

import com.academy.mudogroupware.global.infrastructure.persistence.BaseTimeEntity;
import com.academy.mudogroupware.sharedfile.domain.model.SharedFileRootStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// shared_file_root의 유일한 행(PK=1)에 대한 JPA 매핑. V3.1.8 마이그레이션이 PK를 1로 고정한 CHECK
// 제약을 두므로, 이 Entity도 항상 SINGLETON_ID로만 생성·조회한다.
// @Version은 두 트랜잭션이 이 행을 동시에 갱신할 때 낙관적 락 충돌(OptimisticLockingFailureException)로
// 검출되게 한다 — Google 계정을 짧은 시간에 두 번 연결(더블클릭 등)해도 먼저 성공한 결과가 나중 실패로 덮이지 않게 한다.
@Entity
@Table(name = "shared_file_root")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SharedFileRootEntity extends BaseTimeEntity {

    public static final int SINGLETON_ID = 1;

    @Id
    @Column(name = "shared_file_root_id")
    private Integer id;

    @Column(name = "google_root_folder_id")
    private String googleRootFolderId;

    // 이 루트 폴더가 지금 어느 Google 계정 소유인지. 연동 해제→재연동 시 계정이 실제로 바뀌었는지를
    // (다른 도메인이 계산해준 값이 아니라) 이 테이블 스스로 판단하기 위한 값이다.
    @Column(name = "connected_google_email")
    private String connectedGoogleEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SharedFileRootStatus status;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    private SharedFileRootEntity(SharedFileRootStatus status, String googleRootFolderId,
            String connectedGoogleEmail) {
        this.id = SINGLETON_ID;
        this.status = status;
        this.googleRootFolderId = googleRootFolderId;
        this.connectedGoogleEmail = connectedGoogleEmail;
    }

    // 행이 아직 없을 때(최초 연결) 새로 만든다.
    public static SharedFileRootEntity create(SharedFileRootStatus status, String googleRootFolderId,
            String connectedGoogleEmail) {
        return new SharedFileRootEntity(status, googleRootFolderId, connectedGoogleEmail);
    }

    // 이미 있는 행을 갱신할 때 쓴다. 호출자가 조회 시점에 들고 있던 version을 그대로 실어 detached
    // 엔티티를 만들면, save()가 이를 merge할 때 Hibernate가 DB의 현재 version과 비교해 다르면
    // 낙관적 락 충돌(OptimisticLockException)을 던진다. save() 안에서 다시 조회해 그 자리에서
    // 수정하면(예전 방식) 비교 대상이 항상 최신 버전이 되어 버려 충돌 감지가 무력화되므로 이 방식을 쓴다.
    public static SharedFileRootEntity forUpdate(Long version, SharedFileRootStatus status,
            String googleRootFolderId, String connectedGoogleEmail) {
        SharedFileRootEntity entity = new SharedFileRootEntity(status, googleRootFolderId, connectedGoogleEmail);
        entity.version = version;
        return entity;
    }
}

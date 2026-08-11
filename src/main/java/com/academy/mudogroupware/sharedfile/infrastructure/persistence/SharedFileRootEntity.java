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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SharedFileRootStatus status;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    private SharedFileRootEntity(SharedFileRootStatus status, String googleRootFolderId) {
        this.id = SINGLETON_ID;
        this.status = status;
        this.googleRootFolderId = googleRootFolderId;
    }

    // 행이 아직 없을 때(최초 연결) 새로 만든다.
    public static SharedFileRootEntity create(SharedFileRootStatus status, String googleRootFolderId) {
        return new SharedFileRootEntity(status, googleRootFolderId);
    }

    // 이미 있는 행의 상태·폴더 ID를 갱신한다(재연결·계정교체·재생성).
    public void update(SharedFileRootStatus status, String googleRootFolderId) {
        this.status = status;
        this.googleRootFolderId = googleRootFolderId;
    }
}

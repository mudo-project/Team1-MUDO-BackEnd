package com.academy.mudogroupware.sharedfile.infrastructure.persistence;

import java.time.LocalDateTime;

import com.academy.mudogroupware.global.infrastructure.persistence.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// google_email당 한 행. 학원이 Google 계정을 A -> B -> A처럼 갈아탈 때, 각 계정이 마지막으로 쓰던
// 루트 폴더 ID를 기억해뒀다가 그 계정으로 돌아오면 재사용하기 위한 이력.
// PK는 이메일 자체가 아니라 이 프로젝트 컨벤션(workspace.active_name처럼 대리키 + 별도 UNIQUE 제약)을
// 따르는 대리키(Long)이고, google_email은 UNIQUE 제약으로만 유일성을 보장한다.
@Entity
@Table(name = "shared_file_root_connection_history",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_shared_file_root_connection_history_google_email", columnNames = "google_email"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SharedFileRootHistoryEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shared_file_root_connection_history_id")
    private Long id;

    @Column(name = "google_email", nullable = false)
    private String googleEmail;

    @Column(name = "google_root_folder_id", nullable = false)
    private String googleRootFolderId;

    @Column(name = "last_connected_at", nullable = false)
    private LocalDateTime lastConnectedAt;

    private SharedFileRootHistoryEntity(String googleEmail, String googleRootFolderId,
            LocalDateTime lastConnectedAt) {
        this.googleEmail = googleEmail;
        this.googleRootFolderId = googleRootFolderId;
        this.lastConnectedAt = lastConnectedAt;
    }

    // 이 이메일의 이력이 아직 없을 때(최초 연결) 새로 만든다.
    public static SharedFileRootHistoryEntity create(String googleEmail, String googleRootFolderId,
            LocalDateTime lastConnectedAt) {
        return new SharedFileRootHistoryEntity(googleEmail, googleRootFolderId, lastConnectedAt);
    }

    // 같은 이메일로 다시 연결됐을 때, 조회해온 기존 행을 최신 폴더 정보로 갱신한다.
    public void updateFolder(String googleRootFolderId, LocalDateTime lastConnectedAt) {
        this.googleRootFolderId = googleRootFolderId;
        this.lastConnectedAt = lastConnectedAt;
    }
}

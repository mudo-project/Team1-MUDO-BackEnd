package com.academy.mudogroupware.sharedfile.infrastructure.persistence;

import java.time.LocalDateTime;

import com.academy.mudogroupware.global.infrastructure.persistence.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// google_email당 한 행. 학원이 Google 계정을 A -> B -> A처럼 갈아탈 때, 각 계정이 마지막으로 쓰던
// 루트 폴더 ID를 기억해뒀다가 그 계정으로 돌아오면 재사용하기 위한 이력. PK가 자연키(이메일)라
// JpaRepository.save()가 findById 후 merge하는 방식으로 동작해 별도 존재 확인 없이 upsert가 된다.
@Entity
@Table(name = "shared_file_root_connection_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SharedFileRootConnectionHistoryEntity extends BaseTimeEntity {

    @Id
    @Column(name = "google_email")
    private String googleEmail;

    @Column(name = "google_root_folder_id", nullable = false)
    private String googleRootFolderId;

    @Column(name = "last_connected_at", nullable = false)
    private LocalDateTime lastConnectedAt;

    public SharedFileRootConnectionHistoryEntity(String googleEmail, String googleRootFolderId,
            LocalDateTime lastConnectedAt) {
        this.googleEmail = googleEmail;
        this.googleRootFolderId = googleRootFolderId;
        this.lastConnectedAt = lastConnectedAt;
    }
}

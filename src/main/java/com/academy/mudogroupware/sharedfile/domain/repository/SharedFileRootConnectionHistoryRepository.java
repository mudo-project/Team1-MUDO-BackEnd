package com.academy.mudogroupware.sharedfile.domain.repository;

import java.time.LocalDateTime;
import java.util.Optional;

// 학원이 Google 계정을 여러 번 갈아탈 때(A -> B -> A), 계정(이메일)별로 마지막에 쓰던 루트 폴더 ID를
// 기억해두는 이력 저장소. shared_file_root(현재 활성 상태 1건만 보관하는 싱글턴)와 달리 이 저장소는
// 이메일마다 별도 행을 유지해서, 이미 지나간 계정의 폴더 정보도 잃지 않는다.
public interface SharedFileRootConnectionHistoryRepository {

    Optional<String> findGoogleRootFolderIdByEmail(String googleEmail);

    // 같은 이메일로 다시 호출하면 기존 행을 갱신한다(덮어쓰기) — 이 저장소는 "가장 최근에 그 계정이
    // 쓰던 폴더"만 기억하면 충분하고, 과거 폴더 이력을 여러 건 쌓아둘 필요는 없다.
    void upsert(String googleEmail, String googleRootFolderId, LocalDateTime connectedAt);
}

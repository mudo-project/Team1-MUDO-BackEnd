package com.academy.mudogroupware.sharedfile.domain.repository;

import java.util.Optional;

import com.academy.mudogroupware.sharedfile.domain.model.SharedFileRoot;

// shared_file_root 단일 행(고정 ID 1)에 대한 저장·조회 계약. 배포 단위당 최대 한 건만 존재한다.
public interface SharedFileRootRepository {

    // 행이 없으면 새로 만들고, 있으면 상태·폴더 ID를 갱신한다.
    SharedFileRoot save(SharedFileRoot root);

    // Google 계정 연결 전에는 행이 없으므로 빈 값을 돌려준다.
    Optional<SharedFileRoot> find();
}

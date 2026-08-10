package com.academy.mudogroupware.file.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FileMetadataJpaRepository extends JpaRepository<FileMetadataEntity, Long> {

    // 다른 학원 사용자가 fileId를 알아내도(추측/유출) 다운로드 URL을 받지 못하도록
    // academyId까지 함께 매칭해야 한다. approval 첨부파일 요약 경로(findById)는 결재
    // 문서 자체가 이미 학원 스코프이므로 그대로 두고, 이 두 메서드는 file 모듈이
    // 직접 노출하는 다운로드 URL API 전용이다.
    Optional<FileMetadataEntity> findByIdAndAcademyId(Long id, Long academyId);

    List<FileMetadataEntity> findAllByIdInAndAcademyId(List<Long> ids, Long academyId);
}

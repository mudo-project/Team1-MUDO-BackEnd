package com.academy.mudogroupware.notice.domain.repository;

import java.util.List;
import java.util.Optional;

import com.academy.mudogroupware.notice.domain.model.Notice;

public interface NoticeRepository {

    Notice save(Notice notice);

    Optional<Notice> findById(Long id);

    List<Notice> findAll(Long academyId, String titleKeyword);

    void deleteById(Long id);
}

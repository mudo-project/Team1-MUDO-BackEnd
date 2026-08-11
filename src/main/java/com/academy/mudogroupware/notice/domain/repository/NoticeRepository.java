package com.academy.mudogroupware.notice.domain.repository;

import java.util.Optional;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.notice.domain.model.Notice;

public interface NoticeRepository {

    Notice save(Notice notice);

    Optional<Notice> findById(Long id);

    PageResult<Notice> findAll(String titleKeyword, int page, int size);

    void deleteById(Long id);
}

package com.academy.mudogroupware.lecture.application.usecase;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.lecture.application.query.LectureDetailView;
import com.academy.mudogroupware.lecture.application.query.LectureSummaryView;
import com.academy.mudogroupware.lecture.domain.repository.LectureFilter;

public interface LectureQueryUseCase {

    PageResult<LectureSummaryView> getLectures(Long academyId, LectureFilter filter, int page, int size);

    LectureDetailView getLectureDetail(Long lectureId, Long academyId);
}

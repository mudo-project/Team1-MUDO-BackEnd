package com.academy.mudogroupware.lecture.application.port;

import java.util.List;
import java.util.Map;

/**
 * Consumer: lecture (강의 상세의 수강생 목록, rollcall의 LectureEnrollmentPort 구현에도 재사용)
 * Purpose: 특정 강의에 현재 활성 상태로 등록된 수강생 목록 조회.
 * student 모듈이 자기 Infrastructure에서 이 Port를 구현한다 (docs/MODULES.md 조회 Port 정책).
 */
public interface EnrolledStudentsPort {

    List<EnrolledStudentInfo> findByLectureId(Long lectureId);

    Map<Long, Long> countByLectureIds(List<Long> lectureIds);
}

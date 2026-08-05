package com.academy.mudogroupware.rollcall.application.port;

import java.util.List;
import java.util.Optional;

/**
 * Consumer: rollcall
 * Purpose: 강의 출결부 화면에 필요한 강의 기본 정보와 수강생(학부모 연락처 포함) 목록 조회.
 * lecture 모듈이 자기 Infrastructure에서 이 Port를 구현한다 (docs/MODULES.md 조회 Port 정책).
 */
public interface LectureEnrollmentPort {

    Optional<LectureRef> findLecture(Long lectureId);

    List<EnrolledStudentRef> getEnrolledStudents(Long lectureId);
}

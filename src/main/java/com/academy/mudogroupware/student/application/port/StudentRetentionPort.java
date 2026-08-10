package com.academy.mudogroupware.student.application.port;

import java.time.LocalDateTime;
import java.util.List;

public interface StudentRetentionPort {

    List<Long> findHardDeleteCandidateIds(LocalDateTime threshold, int batchSize);

    // 자식 테이블(수강 이력)을 먼저 지우고 삭제된 건수를 반환한다.
    int deleteEnrollmentsByStudentIds(List<Long> studentIds);

    // 부모 테이블(학생)을 지우고 삭제된 건수를 반환한다. threshold를 다시 검사해
    // 후보 조회와 삭제 사이 데이터가 바뀌는 경우를 한 번 더 방어한다.
    int hardDeleteStudentsByIds(List<Long> studentIds, LocalDateTime threshold);
}

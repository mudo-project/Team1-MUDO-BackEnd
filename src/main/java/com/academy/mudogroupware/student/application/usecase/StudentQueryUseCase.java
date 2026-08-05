package com.academy.mudogroupware.student.application.usecase;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.student.application.query.StudentDetail;
import com.academy.mudogroupware.student.application.query.StudentSummary;

public interface StudentQueryUseCase {

    PageResult<StudentSummary> getStudents(Long academyId, String keyword, int page, int size);

    StudentDetail getStudentDetail(Long academyId, Long studentId);
}

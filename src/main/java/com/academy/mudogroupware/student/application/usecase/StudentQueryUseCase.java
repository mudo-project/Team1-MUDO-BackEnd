package com.academy.mudogroupware.student.application.usecase;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.student.application.query.StudentDetail;
import com.academy.mudogroupware.student.application.query.StudentSummary;
import com.academy.mudogroupware.student.domain.model.StudentSortDirection;

public interface StudentQueryUseCase {

    PageResult<StudentSummary> getStudents(String keyword, int page, int size);

    default PageResult<StudentSummary> getStudents(String keyword, int page, int size,
                                                   StudentSortDirection direction) {
        return getStudents(keyword, page, size);
    }

    StudentDetail getStudentDetail(Long studentId);
}

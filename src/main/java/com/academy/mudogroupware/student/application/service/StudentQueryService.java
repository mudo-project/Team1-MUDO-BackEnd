package com.academy.mudogroupware.student.application.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.student.application.port.LectureCatalogInfo;
import com.academy.mudogroupware.student.application.port.LectureCatalogPort;
import com.academy.mudogroupware.student.application.query.EnrollmentSummary;
import com.academy.mudogroupware.student.application.query.StudentDetail;
import com.academy.mudogroupware.student.application.query.StudentSummary;
import com.academy.mudogroupware.student.application.usecase.StudentQueryUseCase;
import com.academy.mudogroupware.student.domain.exception.StudentErrorCode;
import com.academy.mudogroupware.student.domain.exception.StudentException;
import com.academy.mudogroupware.student.domain.model.Enrollment;
import com.academy.mudogroupware.student.domain.model.Student;
import com.academy.mudogroupware.student.domain.repository.EnrollmentRepository;
import com.academy.mudogroupware.student.domain.repository.StudentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentQueryService implements StudentQueryUseCase {

    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LectureCatalogPort lectureCatalogPort;

    @Override
    public PageResult<StudentSummary> getStudents(String keyword, int page, int size) {
        PageResult<Student> result = studentRepository.findAll(keyword, page, size);
        List<Long> studentIds = result.content().stream().map(Student::getId).toList();
        Map<Long, Long> activeEnrollmentCounts = enrollmentRepository.countActiveByStudentIds(studentIds);

        return result.map(student -> toSummary(student,
                activeEnrollmentCounts.getOrDefault(student.getId(), 0L).intValue()));
    }

    @Override
    public StudentDetail getStudentDetail(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentException(StudentErrorCode.STUDENT_NOT_FOUND));

        List<Enrollment> enrollments = enrollmentRepository.findActiveByStudentId(studentId);
        List<Long> lectureIds = enrollments.stream().map(Enrollment::getLectureId).distinct().toList();
        Map<Long, LectureCatalogInfo> lectures = lectureCatalogPort.findByIds(lectureIds);

        return new StudentDetail(
                student.getId(),
                student.getName(),
                student.getGrade(),
                student.getSchool(),
                student.getPhone(),
                student.getParentPhone(),
                student.getNote(),
                student.getCreatedAt(),
                student.getUpdatedAt(),
                enrollments.stream().map(enrollment -> toEnrollmentSummary(enrollment, lectures)).toList()
        );
    }

    private StudentSummary toSummary(Student student, int activeEnrollmentCount) {
        return new StudentSummary(student.getId(), student.getName(), student.getGrade(), student.getSchool(),
                student.getPhone(), student.getParentPhone(), activeEnrollmentCount);
    }

    private EnrollmentSummary toEnrollmentSummary(Enrollment enrollment, Map<Long, LectureCatalogInfo> lectures) {
        LectureCatalogInfo lecture = lectures.get(enrollment.getLectureId());
        return new EnrollmentSummary(
                enrollment.getId(),
                enrollment.getLectureId(),
                lecture != null ? lecture.lectureName() : null,
                lecture != null ? lecture.teacherName() : null,
                lecture != null ? lecture.scheduleText() : null,
                lecture != null ? lecture.priceType() : null,
                lecture != null ? lecture.priceAmount() : null,
                enrollment.getEnrolledAt()
        );
    }
}

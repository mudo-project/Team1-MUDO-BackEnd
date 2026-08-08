package com.academy.mudogroupware.student.infrastructure.lecture;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.lecture.application.port.EnrolledStudentInfo;
import com.academy.mudogroupware.lecture.application.port.EnrolledStudentsPort;
import com.academy.mudogroupware.student.domain.model.Enrollment;
import com.academy.mudogroupware.student.domain.model.Student;
import com.academy.mudogroupware.student.domain.repository.EnrollmentRepository;
import com.academy.mudogroupware.student.domain.repository.StudentRepository;

import lombok.RequiredArgsConstructor;

/**
 * Consumer: lecture (강의 상세 수강생 목록, rollcall의 LectureEnrollmentPort 구현에도 재사용됨)
 * Purpose: 특정 강의에 현재 활성 상태로 등록된 수강생 목록(학생명/학년/학부모 연락처)을 조회한다.
 */
@Component
@RequiredArgsConstructor
public class EnrolledStudentsPortAdapter implements EnrolledStudentsPort {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;

    @Override
    public List<EnrolledStudentInfo> findByLectureId(Long academyId, Long lectureId) {
        List<Enrollment> enrollments = enrollmentRepository.findActiveByLectureId(academyId, lectureId);
        List<Long> studentIds = enrollments.stream().map(Enrollment::getStudentId).distinct().toList();

        Map<Long, Student> studentsById = studentRepository.findAllById(studentIds).stream()
                .collect(Collectors.toMap(Student::getId, Function.identity()));

        return studentIds.stream()
                .map(studentsById::get)
                .filter(Objects::nonNull)
                .map(student -> new EnrolledStudentInfo(student.getId(), student.getName(),
                        student.getGrade().name(), student.getParentPhone()))
                .toList();
    }

    @Override
    public Map<Long, Long> countByLectureIds(Long academyId, List<Long> lectureIds) {
        return enrollmentRepository.countActiveByLectureIds(academyId, lectureIds);
    }
}

package com.academy.mudogroupware.lecture.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.lecture.domain.model.Enrollment;
import com.academy.mudogroupware.lecture.domain.repository.EnrollmentRepository;
import com.academy.mudogroupware.lecture.domain.repository.LectureRepository;
import com.academy.mudogroupware.lecture.domain.repository.StudentRepository;
import com.academy.mudogroupware.rollcall.application.port.EnrolledStudentRef;
import com.academy.mudogroupware.rollcall.application.port.LectureEnrollmentPort;
import com.academy.mudogroupware.rollcall.application.port.LectureRef;

import lombok.RequiredArgsConstructor;

/**
 * Consumer: rollcall
 * Purpose: 강의 출결부 화면에 필요한 강의 기본 정보와 수강생(학부모 연락처 포함) 목록 조회
 */
@Component
@RequiredArgsConstructor
public class LectureEnrollmentPortAdapter implements LectureEnrollmentPort {

    private final LectureRepository lectureRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;

    @Override
    public Optional<LectureRef> findLecture(Long lectureId) {
        return lectureRepository.findById(lectureId)
                .map(lecture -> new LectureRef(lecture.getId(), lecture.getName(), lecture.getAcademyId()));
    }

    @Override
    public List<EnrolledStudentRef> getEnrolledStudents(Long lectureId) {
        List<Long> studentIds = enrollmentRepository.findByLectureId(lectureId).stream()
                .map(Enrollment::getStudentId)
                .toList();

        return studentRepository.findAllById(studentIds).stream()
                .map(student -> new EnrolledStudentRef(student.getId(), student.getName(),
                        student.getGrade().name(), student.getParentPhone()))
                .toList();
    }
}

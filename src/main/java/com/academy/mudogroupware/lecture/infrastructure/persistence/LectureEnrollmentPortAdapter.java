package com.academy.mudogroupware.lecture.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.lecture.application.port.EnrolledStudentsPort;
import com.academy.mudogroupware.lecture.domain.model.Lecture;
import com.academy.mudogroupware.lecture.domain.repository.LectureRepository;
import com.academy.mudogroupware.rollcall.application.port.EnrolledStudentRef;
import com.academy.mudogroupware.rollcall.application.port.LectureEnrollmentPort;
import com.academy.mudogroupware.rollcall.application.port.LectureRef;

import lombok.RequiredArgsConstructor;

/**
 * Consumer: rollcall
 * Purpose: 강의 출결부 화면에 필요한 강의 기본 정보와 수강생(학부모 연락처 포함) 목록 조회.
 * 수강생 목록 자체는 student 모듈이 소유하므로, lecture가 정의한 EnrolledStudentsPort를 거쳐 조회한다.
 */
@Component
@RequiredArgsConstructor
public class LectureEnrollmentPortAdapter implements LectureEnrollmentPort {

    private final LectureRepository lectureRepository;
    private final EnrolledStudentsPort enrolledStudentsPort;

    @Override
    public Optional<LectureRef> findLecture(Long lectureId) {
        return lectureRepository.findById(lectureId)
                .map(lecture -> new LectureRef(lecture.getId(), lecture.getName()));
    }

    @Override
    public List<EnrolledStudentRef> getEnrolledStudents(Long lectureId) {
        Optional<Lecture> lecture = lectureRepository.findById(lectureId);
        if (lecture.isEmpty()) {
            return List.of();
        }

        return enrolledStudentsPort.findByLectureId(lectureId).stream()
                .map(student -> new EnrolledStudentRef(student.studentId(), student.name(), student.grade(),
                        student.parentPhone()))
                .toList();
    }
}

package com.academy.mudogroupware.lecture.application.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.lecture.application.query.LectureDetailView;
import com.academy.mudogroupware.lecture.application.query.LectureSummaryView;
import com.academy.mudogroupware.lecture.application.query.ScheduleView;
import com.academy.mudogroupware.lecture.application.query.StudentSummaryView;
import com.academy.mudogroupware.lecture.application.usecase.LectureQueryUseCase;
import com.academy.mudogroupware.lecture.domain.exception.LectureAccessDeniedException;
import com.academy.mudogroupware.lecture.domain.exception.LectureNotFoundException;
import com.academy.mudogroupware.lecture.domain.model.Classroom;
import com.academy.mudogroupware.lecture.domain.model.Enrollment;
import com.academy.mudogroupware.lecture.domain.model.Lecture;
import com.academy.mudogroupware.lecture.domain.model.Student;
import com.academy.mudogroupware.lecture.domain.model.Subject;
import com.academy.mudogroupware.lecture.domain.model.Term;
import com.academy.mudogroupware.lecture.domain.repository.ClassroomRepository;
import com.academy.mudogroupware.lecture.domain.repository.EnrollmentRepository;
import com.academy.mudogroupware.lecture.domain.repository.LectureFilter;
import com.academy.mudogroupware.lecture.domain.repository.LectureRepository;
import com.academy.mudogroupware.lecture.domain.repository.StudentRepository;
import com.academy.mudogroupware.lecture.domain.repository.SubjectRepository;
import com.academy.mudogroupware.lecture.domain.repository.TermRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LectureQueryService implements LectureQueryUseCase {

    private final LectureRepository lectureRepository;
    private final TermRepository termRepository;
    private final SubjectRepository subjectRepository;
    private final ClassroomRepository classroomRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;

    @Override
    public PageResult<LectureSummaryView> getLectures(Long academyId, LectureFilter filter, int page, int size) {
        PageResult<Lecture> result = lectureRepository.findAll(academyId, filter, page, size);
        List<Lecture> lectures = result.content();

        Map<Long, String> termNames = idToName(
                termRepository.findAllById(distinctIds(lectures, Lecture::getTermId)),
                Term::getId, Term::getName);
        Map<Long, String> subjectNames = idToName(
                subjectRepository.findAllById(distinctIds(lectures, Lecture::getSubjectId)),
                Subject::getId, Subject::getName);
        Map<Long, String> classroomNames = idToName(
                classroomRepository.findAllById(distinctIds(lectures, Lecture::getClassroomId)),
                Classroom::getId, Classroom::getName);
        Map<Long, Integer> studentCounts = lectures.stream()
                .collect(Collectors.toMap(Lecture::getId, l -> enrollmentRepository.findByLectureId(l.getId()).size()));

        return result.map(lecture -> new LectureSummaryView(
                lecture.getId(),
                lecture.getName(),
                lecture.getGrade(),
                termNames.get(lecture.getTermId()),
                subjectNames.get(lecture.getSubjectId()),
                lecture.getTeacherId(),
                classroomNames.get(lecture.getClassroomId()),
                toScheduleViews(lecture),
                studentCounts.getOrDefault(lecture.getId(), 0)));
    }

    @Override
    public LectureDetailView getLectureDetail(Long lectureId, Long academyId) {
        Lecture lecture = lectureRepository.findById(lectureId).orElseThrow(LectureNotFoundException::new);
        if (!lecture.getAcademyId().equals(academyId)) {
            throw new LectureAccessDeniedException();
        }

        String termName = singleName(termRepository.findAllById(List.of(lecture.getTermId())), Term::getName);
        String subjectName = singleName(subjectRepository.findAllById(List.of(lecture.getSubjectId())),
                Subject::getName);
        String classroomName = singleName(classroomRepository.findAllById(List.of(lecture.getClassroomId())),
                Classroom::getName);

        List<Enrollment> enrollments = enrollmentRepository.findByLectureId(lectureId);
        List<Student> students = studentRepository.findAllById(
                enrollments.stream().map(Enrollment::getStudentId).toList());
        List<StudentSummaryView> studentViews = students.stream()
                .map(s -> new StudentSummaryView(s.getId(), s.getName(), s.getGrade()))
                .toList();

        return new LectureDetailView(lecture.getId(), lecture.getName(), lecture.getGrade(), termName, subjectName,
                lecture.getTeacherId(), classroomName, lecture.getFeeType(), lecture.getFeeAmount(),
                toScheduleViews(lecture), studentViews, lecture.getCreatedAt());
    }

    private List<ScheduleView> toScheduleViews(Lecture lecture) {
        return lecture.getSchedules().stream()
                .map(s -> new ScheduleView(s.getDayOfWeek(), s.getStartTime(), s.getEndTime()))
                .toList();
    }

    private List<Long> distinctIds(List<Lecture> lectures, Function<Lecture, Long> idExtractor) {
        return lectures.stream().map(idExtractor).distinct().toList();
    }

    private <T> Map<Long, String> idToName(List<T> items, Function<T, Long> idFn, Function<T, String> nameFn) {
        return items.stream().collect(Collectors.toMap(idFn, nameFn));
    }

    private <T> String singleName(List<T> items, Function<T, String> nameFn) {
        return items.stream().findFirst().map(nameFn).orElse(null);
    }
}

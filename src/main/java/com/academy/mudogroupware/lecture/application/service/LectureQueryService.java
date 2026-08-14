package com.academy.mudogroupware.lecture.application.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.lecture.application.port.EnrolledStudentInfo;
import com.academy.mudogroupware.lecture.application.port.EnrolledStudentsPort;
import com.academy.mudogroupware.lecture.application.port.TeacherDirectoryPort;
import com.academy.mudogroupware.lecture.application.port.TeacherInfo;
import com.academy.mudogroupware.lecture.application.query.LectureDetailView;
import com.academy.mudogroupware.lecture.application.query.LectureSummaryView;
import com.academy.mudogroupware.lecture.application.query.ScheduleView;
import com.academy.mudogroupware.lecture.application.query.StudentSummaryView;
import com.academy.mudogroupware.lecture.application.usecase.LectureQueryUseCase;
import com.academy.mudogroupware.lecture.domain.exception.LectureNotFoundException;
import com.academy.mudogroupware.lecture.domain.model.Classroom;
import com.academy.mudogroupware.lecture.domain.model.Lecture;
import com.academy.mudogroupware.lecture.domain.model.Subject;
import com.academy.mudogroupware.lecture.domain.model.Term;
import com.academy.mudogroupware.lecture.domain.repository.ClassroomRepository;
import com.academy.mudogroupware.lecture.domain.repository.LectureFilter;
import com.academy.mudogroupware.lecture.domain.repository.LectureRepository;
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
    private final EnrolledStudentsPort enrolledStudentsPort;
    private final TeacherDirectoryPort teacherDirectoryPort;

    @Override
    public PageResult<LectureSummaryView> getLectures(LectureFilter filter, int page, int size) {
        PageResult<Lecture> result = lectureRepository.findAll(filter, page, size);
        List<Lecture> lectures = result.content();

        Map<Long, String> termNames = idToName(findAllTerms(distinctNonNullIds(lectures, Lecture::getTermId)),
                Term::getId, Term::getName);
        Map<Long, String> subjectNames = idToName(
                findAllSubjects(distinctIdsNeedingFallback(lectures, Lecture::getSubjectName, Lecture::getSubjectId)),
                Subject::getId, Subject::getName);
        Map<Long, String> classroomNames = idToName(
                findAllClassrooms(distinctNonNullIds(lectures, Lecture::getClassroomId)),
                Classroom::getId, Classroom::getName);
        Map<Long, TeacherInfo> teachers = findTeachers(
                distinctIdsNeedingFallback(lectures, Lecture::getTeacherName, Lecture::getTeacherId));
        Map<Long, Long> studentCounts = enrolledStudentsPort.countByLectureIds(
                distinctNonNullIds(lectures, Lecture::getId));

        return result.map(lecture -> new LectureSummaryView(
                lecture.getId(),
                lecture.getName(),
                lecture.getClassType(),
                lecture.getGrade(),
                termNames.get(lecture.getTermId()),
                subjectName(lecture, subjectNames),
                lecture.getTeacherId(),
                teacherName(lecture, teachers),
                lecture.getClassroomCode(),
                classroomName(lecture, classroomNames),
                toScheduleViews(lecture),
                studentCounts.getOrDefault(lecture.getId(), 0L).intValue()));
    }

    @Override
    public LectureDetailView getLectureDetail(Long lectureId) {
        Lecture lecture = lectureRepository.findById(lectureId).orElseThrow(LectureNotFoundException::new);

        String termName = lecture.getTermId() != null
                ? singleName(termRepository.findAllById(List.of(lecture.getTermId())), Term::getName)
                : null;
        Map<Long, String> subjectNames = idToName(findAllSubjects(
                distinctIdsNeedingFallback(List.of(lecture), Lecture::getSubjectName, Lecture::getSubjectId)),
                Subject::getId, Subject::getName);
        Map<Long, String> classroomNames = idToName(findAllClassrooms(
                distinctNonNullIds(List.of(lecture), Lecture::getClassroomId)), Classroom::getId, Classroom::getName);
        Map<Long, TeacherInfo> teachers = findTeachers(
                distinctIdsNeedingFallback(List.of(lecture), Lecture::getTeacherName, Lecture::getTeacherId));

        List<EnrolledStudentInfo> enrolledStudents = enrolledStudentsPort.findByLectureId(lectureId);
        List<StudentSummaryView> studentViews = enrolledStudents.stream()
                .map(s -> new StudentSummaryView(s.studentId(), s.name(), s.grade()))
                .toList();

        return new LectureDetailView(lecture.getId(), lecture.getName(), lecture.getClassType(), lecture.getGrade(),
                termName, subjectName(lecture, subjectNames), lecture.getTeacherId(), teacherName(lecture, teachers),
                lecture.getClassroomCode(), classroomName(lecture, classroomNames),
                lecture.getFeeType(), lecture.getFeeAmount(),
                toScheduleViews(lecture), studentViews, lecture.getCreatedAt());
    }

    @Override
    public List<String> getTeacherNames() {
        return lectureRepository.findDistinctTeacherNames();
    }

    private List<ScheduleView> toScheduleViews(Lecture lecture) {
        return lecture.getSchedules().stream()
                .map(s -> new ScheduleView(s.getDayOfWeek(), s.getStartTime(), s.getEndTime()))
                .toList();
    }

    private List<Long> distinctNonNullIds(List<Lecture> lectures, Function<Lecture, Long> idExtractor) {
        return lectures.stream().map(idExtractor).filter(Objects::nonNull).distinct().toList();
    }

    private List<Long> distinctIdsNeedingFallback(List<Lecture> lectures, Function<Lecture, String> storedValue,
                                                  Function<Lecture, Long> idExtractor) {
        return lectures.stream()
                .filter(lecture -> !hasText(storedValue.apply(lecture)))
                .map(idExtractor)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private <T> Map<Long, String> idToName(List<T> items, Function<T, Long> idFn, Function<T, String> nameFn) {
        return items.stream().collect(Collectors.toMap(idFn, nameFn));
    }

    private <T> String singleName(List<T> items, Function<T, String> nameFn) {
        return items.stream().findFirst().map(nameFn).orElse(null);
    }

    private List<Term> findAllTerms(List<Long> termIds) {
        return termIds.isEmpty() ? List.of() : termRepository.findAllById(termIds);
    }

    private List<Subject> findAllSubjects(List<Long> subjectIds) {
        return subjectIds.isEmpty() ? List.of() : subjectRepository.findAllById(subjectIds);
    }

    private List<Classroom> findAllClassrooms(List<Long> classroomIds) {
        return classroomIds.isEmpty() ? List.of() : classroomRepository.findAllById(classroomIds);
    }

    private Map<Long, TeacherInfo> findTeachers(List<Long> teacherIds) {
        return teacherIds.isEmpty() ? Map.of() : teacherDirectoryPort.findTeachers(teacherIds);
    }

    private String teacherName(Lecture lecture, Map<Long, TeacherInfo> teachers) {
        if (hasText(lecture.getTeacherName())) {
            return lecture.getTeacherName();
        }
        TeacherInfo teacher = lecture.getTeacherId() != null ? teachers.get(lecture.getTeacherId()) : null;
        return teacher != null ? teacher.name() : null;
    }

    private String subjectName(Lecture lecture, Map<Long, String> subjectNames) {
        return hasText(lecture.getSubjectName()) ? lecture.getSubjectName() : subjectNames.get(lecture.getSubjectId());
    }

    private String classroomName(Lecture lecture, Map<Long, String> classroomNames) {
        String legacyName = lecture.getClassroomId() != null ? classroomNames.get(lecture.getClassroomId()) : null;
        return legacyName != null ? legacyName : lecture.getClassroomCode();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

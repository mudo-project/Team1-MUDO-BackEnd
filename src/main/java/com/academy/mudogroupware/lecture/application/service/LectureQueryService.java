package com.academy.mudogroupware.lecture.application.service;

import java.util.List;
import java.util.Map;
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

        Map<Long, String> termNames = idToName(
                termRepository.findAllById(distinctIds(lectures, Lecture::getTermId)),
                Term::getId, Term::getName);
        Map<Long, String> subjectNames = idToName(
                subjectRepository.findAllById(distinctIds(lectures, Lecture::getSubjectId)),
                Subject::getId, Subject::getName);
        Map<Long, String> classroomNames = idToName(
                classroomRepository.findAllById(distinctIds(lectures, Lecture::getClassroomId)),
                Classroom::getId, Classroom::getName);
        Map<Long, TeacherInfo> teachers = teacherDirectoryPort.findTeachers(
                distinctIds(lectures, Lecture::getTeacherId));
        Map<Long, Long> studentCounts = enrolledStudentsPort.countByLectureIds(
                distinctIds(lectures, Lecture::getId));

        return result.map(lecture -> new LectureSummaryView(
                lecture.getId(),
                lecture.getName(),
                lecture.getGrade(),
                termNames.get(lecture.getTermId()),
                subjectNames.get(lecture.getSubjectId()),
                lecture.getTeacherId(),
                teacherName(teachers, lecture.getTeacherId()),
                classroomNames.get(lecture.getClassroomId()),
                toScheduleViews(lecture),
                studentCounts.getOrDefault(lecture.getId(), 0L).intValue()));
    }

    @Override
    public LectureDetailView getLectureDetail(Long lectureId) {
        Lecture lecture = lectureRepository.findById(lectureId).orElseThrow(LectureNotFoundException::new);

        String termName = singleName(termRepository.findAllById(List.of(lecture.getTermId())), Term::getName);
        String subjectName = singleName(subjectRepository.findAllById(List.of(lecture.getSubjectId())),
                Subject::getName);
        String classroomName = singleName(classroomRepository.findAllById(List.of(lecture.getClassroomId())),
                Classroom::getName);
        Map<Long, TeacherInfo> teachers = teacherDirectoryPort.findTeachers(List.of(lecture.getTeacherId()));

        List<EnrolledStudentInfo> enrolledStudents = enrolledStudentsPort.findByLectureId(lectureId);
        List<StudentSummaryView> studentViews = enrolledStudents.stream()
                .map(s -> new StudentSummaryView(s.studentId(), s.name(), s.grade()))
                .toList();

        return new LectureDetailView(lecture.getId(), lecture.getName(), lecture.getGrade(), termName, subjectName,
                lecture.getTeacherId(), teacherName(teachers, lecture.getTeacherId()), classroomName,
                lecture.getFeeType(), lecture.getFeeAmount(),
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

    private String teacherName(Map<Long, TeacherInfo> teachers, Long teacherId) {
        TeacherInfo teacher = teachers.get(teacherId);
        return teacher != null ? teacher.name() : null;
    }
}

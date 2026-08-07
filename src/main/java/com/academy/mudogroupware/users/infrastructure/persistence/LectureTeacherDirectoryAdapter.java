package com.academy.mudogroupware.users.infrastructure.persistence;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.lecture.application.port.TeacherDirectoryPort;
import com.academy.mudogroupware.lecture.application.port.TeacherInfo;
import com.academy.mudogroupware.users.domain.model.User;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LectureTeacherDirectoryAdapter implements TeacherDirectoryPort {

    private final UserRepository userRepository;

    /**
     * Consumer: lecture
     * Purpose: Resolve teacher names for lecture list/detail responses.
     */
    @Override
    public Map<Long, TeacherInfo> findTeachers(Long academyId, List<Long> teacherIds) {
        if (teacherIds == null || teacherIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(new LinkedHashSet<>(teacherIds)).stream()
                .filter(user -> academyId.equals(user.getAcademyId()))
                .map(this::toTeacherInfo)
                .collect(Collectors.toMap(TeacherInfo::userId, Function.identity(), (left, right) -> left));
    }

    private TeacherInfo toTeacherInfo(User user) {
        return new TeacherInfo(user.getId(), user.getName(), user.getRoleId(), user.getStatus().name());
    }
}

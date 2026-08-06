package com.academy.mudogroupware.users.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.lecture.application.port.TeacherInfo;
import com.academy.mudogroupware.users.domain.model.User;
import com.academy.mudogroupware.users.domain.model.UserStatus;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

class LectureTeacherDirectoryAdapterTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final LectureTeacherDirectoryAdapter adapter = new LectureTeacherDirectoryAdapter(userRepository);

    @Test
    void returnsTeachersOnlyInRequestedAcademy() {
        User included = user(10L, 1L, "Teacher Kim", 3L, UserStatus.ACTIVE);
        User otherAcademy = user(20L, 2L, "Other Teacher", 4L, UserStatus.ACTIVE);
        when(userRepository.findAllById(Set.of(10L, 20L))).thenReturn(List.of(included, otherAcademy));

        Map<Long, TeacherInfo> result = adapter.findTeachers(1L, List.of(10L, 20L));

        assertThat(result).containsOnlyKeys(10L);
        assertThat(result.get(10L))
                .isEqualTo(new TeacherInfo(10L, "Teacher Kim", 3L, UserStatus.ACTIVE.name()));
    }

    @Test
    void returnsEmptyMapWithoutCallingRepositoryForEmptyIds() {
        assertThat(adapter.findTeachers(1L, List.of())).isEmpty();

        verifyNoInteractions(userRepository);
    }

    private User user(Long id, Long academyId, String name, Long roleId, UserStatus status) {
        return User.restore(id, academyId, "user" + id, "pw", name, "010", "user" + id + "@example.com",
                roleId, status, false, false, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
    }
}

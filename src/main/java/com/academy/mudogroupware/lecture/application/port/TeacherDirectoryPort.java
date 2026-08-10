package com.academy.mudogroupware.lecture.application.port;

import java.util.List;
import java.util.Map;

public interface TeacherDirectoryPort {

    Map<Long, TeacherInfo> findTeachers(Long academyId, List<Long> teacherIds);
}

package com.academy.mudogroupware.revenuereport.application.port;

import java.util.List;
import java.util.Map;

public interface EnrollmentLectureLookupPort {

    Map<Long, Long> findLectureIdsByEnrollmentIds(List<Long> enrollmentIds);
}

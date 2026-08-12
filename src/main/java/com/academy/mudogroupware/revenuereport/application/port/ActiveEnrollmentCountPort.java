package com.academy.mudogroupware.revenuereport.application.port;

import java.util.List;
import java.util.Map;

public interface ActiveEnrollmentCountPort {

    Map<Long, Long> countActiveByLectureIds(List<Long> lectureIds);
}

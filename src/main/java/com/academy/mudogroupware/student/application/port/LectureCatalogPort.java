package com.academy.mudogroupware.student.application.port;

import java.util.List;
import java.util.Map;

public interface LectureCatalogPort {

    Map<Long, LectureCatalogInfo> findByIds(Long academyId, List<Long> lectureIds);
}

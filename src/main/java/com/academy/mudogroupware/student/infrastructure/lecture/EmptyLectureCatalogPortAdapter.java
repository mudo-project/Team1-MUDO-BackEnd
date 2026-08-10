package com.academy.mudogroupware.student.infrastructure.lecture;

import java.util.List;
import java.util.Map;

import com.academy.mudogroupware.student.application.port.LectureCatalogInfo;
import com.academy.mudogroupware.student.application.port.LectureCatalogPort;

public class EmptyLectureCatalogPortAdapter implements LectureCatalogPort {

    @Override
    public Map<Long, LectureCatalogInfo> findByIds(List<Long> lectureIds) {
        return Map.of();
    }
}

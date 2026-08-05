package com.academy.mudogroupware.student.infrastructure.lecture;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.academy.mudogroupware.student.application.port.LectureCatalogPort;

@Configuration
public class StudentLecturePortConfig {

    @Bean
    @ConditionalOnMissingBean(LectureCatalogPort.class)
    public LectureCatalogPort emptyLectureCatalogPortAdapter() {
        return new EmptyLectureCatalogPortAdapter();
    }
}

package com.academy.mudogroupware.lecture.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.lecture.application.command.DeleteLectureCommand;
import com.academy.mudogroupware.lecture.application.usecase.DeleteLectureUseCase;
import com.academy.mudogroupware.lecture.domain.exception.LectureNotFoundException;
import com.academy.mudogroupware.lecture.domain.repository.LectureRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DeleteLectureService implements DeleteLectureUseCase {

    private final LectureRepository lectureRepository;
    private final Clock clock;

    @Override
    public void deleteLecture(DeleteLectureCommand command) {
        lectureRepository.findById(command.lectureId())
                .orElseThrow(LectureNotFoundException::new);
        lectureRepository.deleteById(command.lectureId(), LocalDateTime.now(clock));
    }
}

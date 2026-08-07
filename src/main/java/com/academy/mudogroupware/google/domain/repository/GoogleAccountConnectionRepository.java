package com.academy.mudogroupware.google.domain.repository;

import java.util.Optional;

import com.academy.mudogroupware.google.domain.model.GoogleAccountConnection;

public interface GoogleAccountConnectionRepository {

    GoogleAccountConnection save(GoogleAccountConnection connection);

    Optional<GoogleAccountConnection> findByAcademyId(Long academyId);

    void deleteByAcademyId(Long academyId);
}

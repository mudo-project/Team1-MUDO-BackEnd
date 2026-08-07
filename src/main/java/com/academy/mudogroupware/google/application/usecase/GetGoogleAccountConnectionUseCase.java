package com.academy.mudogroupware.google.application.usecase;

import java.util.Optional;

import com.academy.mudogroupware.google.application.query.GoogleAccountConnectionView;

public interface GetGoogleAccountConnectionUseCase {

    Optional<GoogleAccountConnectionView> getConnection(Long academyId);
}

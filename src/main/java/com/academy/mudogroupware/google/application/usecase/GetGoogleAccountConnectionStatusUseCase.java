package com.academy.mudogroupware.google.application.usecase;

import com.academy.mudogroupware.google.domain.model.GoogleConnectionStatus;

public interface GetGoogleAccountConnectionStatusUseCase {

    GoogleConnectionStatus getStatus();
}

package com.academy.mudogroupware.google.application.port;

import java.util.Optional;

public interface GoogleConnectionUserDirectoryPort {

    Optional<GoogleConnectionUserInfo> findByUserId(Long userId);
}

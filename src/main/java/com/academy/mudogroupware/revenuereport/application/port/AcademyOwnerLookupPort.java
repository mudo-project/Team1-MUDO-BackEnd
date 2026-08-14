package com.academy.mudogroupware.revenuereport.application.port;

import java.util.Optional;

public interface AcademyOwnerLookupPort {
    Optional<Long> findAcademyOwnerUserId();
}

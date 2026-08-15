package com.academy.mudogroupware.users.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.revenuereport.application.port.AcademyOwnerLookupPort;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AcademyOwnerLookupAdapter implements AcademyOwnerLookupPort {

    private final UserRepository userRepository;

    /**
     * Consumer: revenuereport
     *
     * <p>Purpose: 매출 리포트 생성 알림을 받을 원장(ACADEMY:OWNER) 계정 조회
     */
    @Override
    public Optional<Long> findAcademyOwnerUserId() {
        return userRepository.findAcademyOwnerId();
    }
}

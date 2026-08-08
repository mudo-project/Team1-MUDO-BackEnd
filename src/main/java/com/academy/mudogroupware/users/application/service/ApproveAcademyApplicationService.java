package com.academy.mudogroupware.users.application.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.global.domain.auth.AccountType;
import com.academy.mudogroupware.global.domain.auth.AdminScope;
import com.academy.mudogroupware.users.application.command.ApproveAcademyApplicationCommand;
import com.academy.mudogroupware.users.application.result.ApproveAcademyApplicationResult;
import com.academy.mudogroupware.users.application.usecase.ApproveAcademyApplicationUseCase;
import com.academy.mudogroupware.users.domain.event.AcademyApplicationApprovedEvent;
import com.academy.mudogroupware.users.domain.exception.AcademyApplicationNotFoundException;
import com.academy.mudogroupware.users.domain.model.Academy;
import com.academy.mudogroupware.users.domain.model.AcademyApplication;
import com.academy.mudogroupware.users.domain.model.User;
import com.academy.mudogroupware.users.domain.repository.AcademyApplicationRepository;
import com.academy.mudogroupware.users.domain.repository.AcademyRepository;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ApproveAcademyApplicationService implements ApproveAcademyApplicationUseCase {

    private static final String TEMP_PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%";
    private static final int TEMP_PASSWORD_LENGTH = 12;

    private final AcademyApplicationRepository academyApplicationRepository;
    private final AcademyRepository academyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Override
    public ApproveAcademyApplicationResult approve(ApproveAcademyApplicationCommand command) {
        AcademyApplication application = academyApplicationRepository.findById(command.applicationId())
                .orElseThrow(AcademyApplicationNotFoundException::new);
        application.ensurePending();

        LocalDateTime now = LocalDateTime.now(clock);
        academyApplicationRepository.markApproved(command.applicationId(), command.reviewerId(), now);

        Academy academy = academyRepository.save(
                Academy.create(application.getAcademyName(), application.getBusinessNo(), application.getId(), now));

        String temporaryPassword = generateTemporaryPassword();
        User user = userRepository.save(User.create(academy.getId(), application.getRequestedLoginId(),
                passwordEncoder.encode(temporaryPassword), application.getRepresentativeName(),
                application.getRepresentativePhone(), application.getRepresentativeEmail(), AccountType.ADMIN,
                AdminScope.ACADEMY, now));

        academyRepository.assignUser(academy.getId(), user.getId(), now);

        eventPublisher.publishEvent(
                new AcademyApplicationApprovedEvent(academy.getId(), user.getId(), application.getId()));

        return new ApproveAcademyApplicationResult(academy.getId(), user.getId(), temporaryPassword);
    }

    private String generateTemporaryPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder builder = new StringBuilder(TEMP_PASSWORD_LENGTH);
        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            builder.append(TEMP_PASSWORD_CHARS.charAt(random.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        return builder.toString();
    }
}

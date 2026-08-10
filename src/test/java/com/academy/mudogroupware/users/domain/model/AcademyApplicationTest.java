package com.academy.mudogroupware.users.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class AcademyApplicationTest {

    @Test
    void submitCreatesPendingApplicationWithoutBusinessInfo() {
        LocalDateTime now = LocalDateTime.now();

        AcademyApplication application = AcademyApplication.submit(
                "academy01", "테스트학원", "홍길동", "hong@example.com", "010-0000-0000", Plan.FREE, now);

        assertThat(application.getId()).isNull();
        assertThat(application.getRequestedLoginId()).isEqualTo("academy01");
        assertThat(application.getAcademyName()).isEqualTo("테스트학원");
        assertThat(application.getRepresentativeName()).isEqualTo("홍길동");
        assertThat(application.getRepresentativeEmail()).isEqualTo("hong@example.com");
        assertThat(application.getRepresentativePhone()).isEqualTo("010-0000-0000");
        assertThat(application.getPlan()).isEqualTo(Plan.FREE);
        assertThat(application.getBusinessNo()).isNull();
        assertThat(application.getBusinessLicenseFileId()).isNull();
        assertThat(application.getStatus()).isEqualTo(AcademyApplicationStatus.PENDING);
        assertThat(application.getRejectReason()).isNull();
        assertThat(application.getReviewedByUserId()).isNull();
        assertThat(application.getReviewedAt()).isNull();
        assertThat(application.getCreatedAt()).isEqualTo(now);
        assertThat(application.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    void restoreRoundTripsPlan() {
        AcademyApplication application = AcademyApplication.restore(
                1L, "academy01", "테스트학원", "123-45-67890", "홍길동", "a@a.com", "010-0000-0000",
                Plan.PAID, null, AcademyApplicationStatus.PENDING, null, null, null,
                LocalDateTime.now(), LocalDateTime.now());

        assertThat(application.getPlan()).isEqualTo(Plan.PAID);
    }
}

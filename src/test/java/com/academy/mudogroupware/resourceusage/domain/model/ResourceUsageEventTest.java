package com.academy.mudogroupware.resourceusage.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class ResourceUsageEventTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 15, 10, 0);

    @Test
    void createsS3StorageEvent() {
        ResourceUsageEvent event = ResourceUsageEvent.s3Storage("file-register", 1024L, NOW);

        assertThat(event.getResourceType()).isEqualTo(ResourceUsageType.S3_STORAGE);
        assertThat(event.getAmount()).isEqualTo(1024L);
        assertThat(event.getFeature()).isEqualTo("file-register");
    }

    @Test
    void createsMailEvent() {
        ResourceUsageEvent event = ResourceUsageEvent.mail("payroll-statement", 1L, NOW);

        assertThat(event.getResourceType()).isEqualTo(ResourceUsageType.MAIL);
        assertThat(event.getAmount()).isEqualTo(1L);
    }
}

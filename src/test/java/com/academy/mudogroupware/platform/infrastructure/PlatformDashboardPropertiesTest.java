package com.academy.mudogroupware.platform.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlatformDashboardPropertiesTest {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void rejectsZeroConnectTimeoutBecauseZeroMeansNoTimeoutToHttpClient() {
    PlatformDashboardProperties properties = new PlatformDashboardProperties();
    properties.setPrometheusConnectTimeoutMs(0);

    Set<ConstraintViolation<PlatformDashboardProperties>> violations = validator.validate(properties);

    assertThat(violations).isNotEmpty();
  }

  @Test
  void rejectsNegativeReadTimeout() {
    PlatformDashboardProperties properties = new PlatformDashboardProperties();
    properties.setPrometheusReadTimeoutMs(-1);

    Set<ConstraintViolation<PlatformDashboardProperties>> violations = validator.validate(properties);

    assertThat(violations).isNotEmpty();
  }

  @Test
  void acceptsDefaultValues() {
    PlatformDashboardProperties properties = new PlatformDashboardProperties();

    Set<ConstraintViolation<PlatformDashboardProperties>> violations = validator.validate(properties);

    assertThat(violations).isEmpty();
  }
}

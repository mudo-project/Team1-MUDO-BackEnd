package com.academy.mudogroupware.workspace.presentation.api.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class CreateWorkspaceRequestTest {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void rejectsNullMemberId() {
    CreateWorkspaceRequest request =
        new CreateWorkspaceRequest("개발팀", Collections.singletonList(null));

    assertThat(validator.validate(request))
        .extracting(violation -> violation.getMessage())
        .contains("참여자 번호는 필수입니다.");
  }
}

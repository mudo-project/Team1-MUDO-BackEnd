package com.academy.mudogroupware.workspace.presentation.api.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class RenameWorkspaceRequestTest {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void rejectsBlankName() {
    RenameWorkspaceRequest request = new RenameWorkspaceRequest("   ");

    assertThat(validator.validate(request))
        .extracting(violation -> violation.getMessage())
        .contains("워크스페이스 이름은 필수입니다.");
  }

  @Test
  void rejectsNameLongerThan100Characters() {
    RenameWorkspaceRequest request = new RenameWorkspaceRequest("가".repeat(101));

    assertThat(validator.validate(request))
        .extracting(violation -> violation.getMessage())
        .contains("워크스페이스 이름은 100자 이하여야 합니다.");
  }

  @Test
  void allowsNameThatIsExactly100CharactersAfterTrim() {
    RenameWorkspaceRequest request = new RenameWorkspaceRequest("  " + "가".repeat(100) + "  ");

    assertThat(validator.validate(request)).isEmpty();
    assertThat(request.name()).hasSize(100);
  }
}

package com.academy.mudogroupware.workspace.presentation.api.request.workspace;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class AddWorkspaceMembersRequestTest {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void rejectsNullMemberId() {
    AddWorkspaceMembersRequest request =
        new AddWorkspaceMembersRequest(Collections.singletonList(null));

    assertThat(validator.validate(request))
        .extracting(violation -> violation.getMessage())
        .contains("참여자 번호는 필수입니다.");
  }

  @Test
  void rejectsNullMemberIdsList() {
    AddWorkspaceMembersRequest request = new AddWorkspaceMembersRequest(null);

    assertThat(validator.validate(request))
        .extracting(violation -> violation.getMessage())
        .contains("참여자 번호 목록은 필수입니다.");
  }

  @Test
  void rejectsEmptyMemberIdsList() {
    AddWorkspaceMembersRequest request = new AddWorkspaceMembersRequest(List.of());

    assertThat(validator.validate(request))
        .extracting(violation -> violation.getMessage())
        .contains("참여자 번호 목록은 필수입니다.");
  }

  @Test
  void rejectsZeroMemberId() {
    AddWorkspaceMembersRequest request = new AddWorkspaceMembersRequest(List.of(0L));

    assertThat(validator.validate(request))
        .extracting(violation -> violation.getMessage())
        .contains("참여자 번호는 양수여야 합니다.");
  }

  @Test
  void rejectsNegativeMemberId() {
    AddWorkspaceMembersRequest request = new AddWorkspaceMembersRequest(List.of(-1L));

    assertThat(validator.validate(request))
        .extracting(violation -> violation.getMessage())
        .contains("참여자 번호는 양수여야 합니다.");
  }
}

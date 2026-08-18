package com.academy.mudogroupware.timetable.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GradeTest {

  @Test
  void commonGradeHasKoreanLabel() {
    assertThat(Grade.COMMON.label()).isEqualTo("공통");
  }
}

package com.academy.mudogroupware.platform.presentation.api.response;

import com.academy.mudogroupware.platform.domain.model.AcademyRuntime;

public record AcademyResponse(String code) {
  public static AcademyResponse from(AcademyRuntime academy) {
    return new AcademyResponse(academy.code());
  }
}

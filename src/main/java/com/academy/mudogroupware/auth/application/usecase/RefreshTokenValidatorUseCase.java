package com.academy.mudogroupware.auth.application.usecase;

import com.academy.mudogroupware.global.domain.auth.RefreshTokenClaims;

public interface RefreshTokenValidatorUseCase {

  RefreshTokenClaims validateStored(String token);
}

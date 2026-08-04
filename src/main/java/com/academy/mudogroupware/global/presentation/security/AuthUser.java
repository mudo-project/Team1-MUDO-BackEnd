package com.academy.mudogroupware.global.presentation.security;

import java.security.Principal;

public record AuthUser(Long userId, String username, Long academyId, Long roleId, String roleName)
    implements Principal {
  @Override
  public String getName() {
    return username;
  }
}

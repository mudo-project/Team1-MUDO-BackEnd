package com.academy.mudogroupware.platform.presentation.api.response;

import com.academy.mudogroupware.platform.domain.model.TenantDirectoryEntry;

public record TenantDirectoryResponse(String code, String apiHost) {
  public static TenantDirectoryResponse from(TenantDirectoryEntry entry) {
    return new TenantDirectoryResponse(entry.code(), entry.apiHost());
  }
}

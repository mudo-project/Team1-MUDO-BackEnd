package com.academy.mudogroupware.platform.presentation.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.platform.application.service.TenantDirectoryQueryService;
import com.academy.mudogroupware.platform.domain.model.TenantDirectoryEntry;
import com.academy.mudogroupware.platform.presentation.api.response.TenantDirectoryResponse;
import org.junit.jupiter.api.Test;

class TenantDirectoryControllerTest {

  @Test
  void resolveReturnsPresentationResponseNotDomainModel() {
    TenantDirectoryQueryService queryService = mock(TenantDirectoryQueryService.class);
    when(queryService.resolve("academy-d"))
        .thenReturn(new TenantDirectoryEntry("academy-d", "sidea-test.ieum.store"));
    TenantDirectoryController controller = new TenantDirectoryController(queryService);

    var response = controller.resolve("academy-d");

    assertThat(response.getBody().data()).isInstanceOf(TenantDirectoryResponse.class);
    TenantDirectoryResponse body = response.getBody().data();
    assertThat(body.code()).isEqualTo("academy-d");
    assertThat(body.apiHost()).isEqualTo("sidea-test.ieum.store");
  }
}

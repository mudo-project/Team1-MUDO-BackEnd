package com.academy.mudogroupware.platform.presentation.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.platform.application.service.PlatformDashboardQueryService;
import com.academy.mudogroupware.platform.domain.model.AcademyApiCallMetrics;
import com.academy.mudogroupware.platform.domain.model.ApiCallMetric;
import com.academy.mudogroupware.platform.domain.model.DashboardPeriod;
import com.academy.mudogroupware.platform.domain.model.DashboardScope;
import com.academy.mudogroupware.platform.domain.model.OperationalMetrics;
import com.academy.mudogroupware.platform.presentation.api.response.AcademyApiCallFrequencyResponse;
import com.academy.mudogroupware.platform.presentation.api.response.OperationalMetricsResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlatformDashboardControllerTest {

  @Test
  void operationalMetricsReturnsPresentationResponseNotDomainModel() {
    PlatformDashboardQueryService queryService = mock(PlatformDashboardQueryService.class);
    PlatformDashboardController controller = new PlatformDashboardController(queryService);
    OperationalMetrics domain = new OperationalMetrics(
        DashboardScope.ALL,
        null,
        DashboardPeriod.LAST_HOUR,
        List.of(new ApiCallMetric("ACCOUNT_ISSUANCE", 3L)),
        120.5,
        1.2,
        new OperationalMetrics.RdsConnectionBudget(10, 100, 10.0),
        List.of(new OperationalMetrics.EcsHostHeadroom(
            "mudo-prod-cluster", "i-1", 2048, 1913, 1024, 900, List.of("academy-a"))));
    when(queryService.operationalMetrics(DashboardScope.ALL, null, DashboardPeriod.LAST_HOUR))
        .thenReturn(domain);

    var response = controller.operationalMetrics(DashboardScope.ALL, null, DashboardPeriod.LAST_HOUR);

    assertThat(response.getBody().data()).isInstanceOf(OperationalMetricsResponse.class);
    OperationalMetricsResponse body = (OperationalMetricsResponse) response.getBody().data();
    assertThat(body.scope()).isEqualTo(DashboardScope.ALL);
    assertThat(body.apiCallMetrics()).hasSize(1);
    assertThat(body.rdsConnectionBudget().safeBudget()).isEqualTo(100);
    assertThat(body.ecsHostHeadrooms()).hasSize(1);
    assertThat(body.ecsHostHeadrooms().get(0).academyCodes()).containsExactly("academy-a");
  }

  @Test
  void apiCallFrequencyReturnsPerAcademyResponseList() {
    PlatformDashboardQueryService queryService = mock(PlatformDashboardQueryService.class);
    PlatformDashboardController controller = new PlatformDashboardController(queryService);
    when(queryService.apiCallFrequency(DashboardScope.ALL, null, DashboardPeriod.LAST_HOUR))
        .thenReturn(List.of(
            new AcademyApiCallMetrics("academy-a", List.of(new ApiCallMetric("ACCOUNT_ISSUANCE", 3L))),
            new AcademyApiCallMetrics("academy-b", List.of())));

    var response = controller.apiCallFrequency(DashboardScope.ALL, null, DashboardPeriod.LAST_HOUR);

    List<AcademyApiCallFrequencyResponse> body = response.getBody().data();
    assertThat(body).hasSize(2);
    assertThat(body.get(0).academyCode()).isEqualTo("academy-a");
    assertThat(body.get(0).apiCallMetrics()).hasSize(1);
    assertThat(body.get(1).academyCode()).isEqualTo("academy-b");
    assertThat(body.get(1).apiCallMetrics()).isEmpty();
  }
}

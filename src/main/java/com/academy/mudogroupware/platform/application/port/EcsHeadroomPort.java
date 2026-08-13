package com.academy.mudogroupware.platform.application.port;

import com.academy.mudogroupware.platform.domain.model.AcademyRuntime;
import com.academy.mudogroupware.platform.domain.model.OperationalMetrics.EcsHostHeadroom;
import java.util.List;

public interface EcsHeadroomPort {
  List<EcsHostHeadroom> findHeadrooms(List<AcademyRuntime> academies);
}

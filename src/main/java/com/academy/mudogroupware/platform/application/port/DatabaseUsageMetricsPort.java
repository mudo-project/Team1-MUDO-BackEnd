package com.academy.mudogroupware.platform.application.port;

import java.util.Collection;

public interface DatabaseUsageMetricsPort {
  long databaseBytes(Collection<String> academyCodes);
}

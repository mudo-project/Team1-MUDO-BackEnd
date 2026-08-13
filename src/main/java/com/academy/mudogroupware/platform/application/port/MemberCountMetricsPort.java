package com.academy.mudogroupware.platform.application.port;

import java.util.Collection;

public interface MemberCountMetricsPort {
  long activeMemberCount(Collection<String> academyCodes);
}

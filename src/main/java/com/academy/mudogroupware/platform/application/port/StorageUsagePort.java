package com.academy.mudogroupware.platform.application.port;

import com.academy.mudogroupware.platform.domain.model.AcademyRuntime;

public interface StorageUsagePort {
  long s3Bytes(AcademyRuntime academy);
}

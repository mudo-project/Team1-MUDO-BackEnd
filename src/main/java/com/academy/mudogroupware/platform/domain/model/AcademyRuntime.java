package com.academy.mudogroupware.platform.domain.model;

public record AcademyRuntime(
    String code,
    String ecsCluster,
    String ecsService,
    String rdsIdentifier,
    int rdsMaxConnections,
    double rdsAppConnectionRatio,
    String staffBucket,
    String financeBucket,
    String s3Prefix) {}

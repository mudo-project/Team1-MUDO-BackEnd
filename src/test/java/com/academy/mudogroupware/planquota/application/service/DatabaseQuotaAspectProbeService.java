package com.academy.mudogroupware.planquota.application.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DatabaseQuotaAspectTest 전용 프로브. 애스펙트 포인트컷이
 * com.academy.mudogroupware..service..*(..) 패턴이라, 실제 대상 서비스들과
 * 동일하게 ..service.. 패키지 아래에 둬야 포인트컷이 실제로 걸리는지 검증할 수 있다.
 */
@Component
public class DatabaseQuotaAspectProbeService {

    @Transactional
    public String writeSomething() {
        return "done";
    }

    @Transactional(readOnly = true)
    public String readSomething() {
        return "done";
    }
}

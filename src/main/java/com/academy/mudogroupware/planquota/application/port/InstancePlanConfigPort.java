package com.academy.mudogroupware.planquota.application.port;

/**
 * 이 인스턴스에 배포된 플랜 설정값을 노출하는 포트다. {@code CurrentPlanProvider}가
 * infrastructure의 설정 바인딩 구현({@code InstanceMetadataProperties})을 직접
 * 의존하지 않도록, 필요한 원시값만 좁게 제공한다.
 */
public interface InstancePlanConfigPort {
    String rawPlan();
}

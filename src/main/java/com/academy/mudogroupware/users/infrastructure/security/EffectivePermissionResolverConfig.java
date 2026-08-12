package com.academy.mudogroupware.users.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.academy.mudogroupware.global.domain.auth.EffectivePermissionResolver;
import com.academy.mudogroupware.global.domain.auth.PlatformAdminPermissionPort;
import com.academy.mudogroupware.global.domain.auth.RolePermissionLookupPort;

/**
 * EffectivePermissionResolver는 도메인 클래스라 @Component를 붙이지 않는다.
 * SecurityConfig 대신 여기서 등록하는 이유: SecurityConfig는 대부분의 @WebMvcTest가
 * @Import해서 쓰는데, 그 테스트들은 JwtAuthenticationConverter를 @MockitoBean으로
 * 갈아끼워서 실제 포트 의존성을 해석할 필요가 없다. 이 빈을 SecurityConfig 안에 두면
 * users 도메인과 무관한 슬라이스 테스트까지 RolePermissionLookupPort/
 * PlatformAdminPermissionPort의 실제 구현체를 요구하게 되어 컨텍스트 로딩이 깨진다.
 */
@Configuration
public class EffectivePermissionResolverConfig {

    @Bean
    EffectivePermissionResolver effectivePermissionResolver(
            RolePermissionLookupPort rolePermissionLookupPort,
            PlatformAdminPermissionPort platformAdminPermissionPort) {
        return new EffectivePermissionResolver(rolePermissionLookupPort, platformAdminPermissionPort);
    }
}

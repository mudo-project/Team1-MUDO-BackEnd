# 구글 연동 모듈 권한 정의

## `ACADEMY:OWNER` — 구글 계정 연동 관리

| 항목 | 설명 |
|------|------|
| **코드** | `ACADEMY:OWNER` (합성 authority, `permission` 테이블 시드 아님) |
| **설명** | 학원 명의 구글 계정 연동을 시작·재연결·상태 조회/확인·해제할 수 있습니다. |
| **적용 대상** | 원장(`account_type=ADMIN` + `admin_scope=ACADEMY`) 본인 계정만. 위임 불가 |
| **구현 상태** | ✅ 구현 완료 (합성 로직 완료, `@PreAuthorize` 적용 완료) |
| **적용 API** | `POST /api/google/connections/authorize-url`, `GET /api/google/connections`, `POST /api/google/connections/check`, `DELETE /api/google/connections` |
| **비고** | `GET /api/google/connections/callback`은 구글이 브라우저를 리다이렉트하는 대상이라 이 권한과 무관하게 `permitAll`이다(신원 확인은 `state` 서명으로 대체). |

**관련 코드:**
- `JwtAuthenticationConverter.toAuthentication()` — `account_type=ADMIN` + `admin_scope=ACADEMY`인 계정에게 `ACADEMY:OWNER` authority를 합성해 부여(`PLATFORM:SUPER_ADMIN`과 동일한 방식)
- `GoogleAccountConnectionController.startConnection()` — `@PreAuthorize("hasAuthority('ACADEMY:OWNER')")`
- `GoogleAccountConnectionController.getConnection()` — `@PreAuthorize("hasAuthority('ACADEMY:OWNER')")`
- `GoogleAccountConnectionController.checkConnection()` — `@PreAuthorize("hasAuthority('ACADEMY:OWNER')")`
- `GoogleAccountConnectionController.disconnect()` — `@PreAuthorize("hasAuthority('ACADEMY:OWNER')")`

**테스트:**
- `GoogleAccountConnectionControllerTest` — 슬라이스(`@WebMvcTest`)에서 성공 경로 검증 (`@PreAuthorize`는 이 슬라이스에서 평가되지 않음)
- `GoogleAccountConnectionSecurityIntegrationTest` — 전체 컨텍스트(`@SpringBootTest`)에서 `ACADEMY:OWNER` 없이 403이 반환되는지, `/callback`이 `permitAll`인지 검증
- `JwtAuthenticationConverterTest` — `account_type=ADMIN` + `admin_scope=ACADEMY` 계정이 역할(role) 기반 권한과 함께 `ACADEMY:OWNER`도 받는지 검증

---

## 권한 부여 방식

- `CALENDAR:MANAGE`와 달리 `permission`/`role_permission` 테이블을 전혀 거치지 않는다. `users.account_type`/`admin_scope` 값에서 로그인 시점에 직접 합성되므로 **위임이 구조적으로 불가능**하다.
- 구글 계정 연동은 학원 명의의 민감한 인증 정보(리프레시 토큰)를 다루는 작업이라, 원장이 다른 구성원에게 권한을 넘겨줄 수 있는 캘린더식 위임 모델을 두지 않기로 결정했다.
- 별도 마이그레이션이나 역할-권한 매핑이 필요 없다 — 원장 계정(`account_type=ADMIN`, `admin_scope=ACADEMY`)으로 로그인하면 즉시 적용된다.

## 권한 모듈 연동 체크리스트

- [x] `JwtAuthenticationConverter`에 `ACADEMY:OWNER` 합성 로직 추가
- [x] `GoogleAccountConnectionController`의 관리 API 4곳에 `@PreAuthorize` 적용
- [x] 전체 컨텍스트 통합 테스트로 403 응답 검증
- [x] `JwtAuthenticationConverterTest`로 authority 합성 로직 단위 검증

---

## 참고 문서

- 구현 상세: [`README.md`](README.md) § 주의 사항
- API 명세: [`GOOGLE_API.md`](GOOGLE_API.md) § 인증 및 권한
- 참고 템플릿: `calendar/docs/CALENDAR_PERMISSIONS.md`, `workspace/docs/WORKSPACE_PERMISSIONS.md`

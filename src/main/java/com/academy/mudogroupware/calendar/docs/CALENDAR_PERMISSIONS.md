# 캘린더 모듈 권한 정의

## `CALENDAR:MANAGE` — 캘린더 일정 작성/수정/삭제

| 항목 | 설명 |
|------|------|
| **코드** | `CALENDAR:MANAGE` |
| **설명** | 학원 공용 캘린더 일정을 작성·수정·삭제할 수 있습니다. |
| **적용 대상** | 원장 및 원장이 권한을 부여한 구성원 |
| **구현 상태** | ✅ 구현 완료 (시드 완료, `@PreAuthorize` 적용 완료) |
| **적용 API** | `POST /api/calendars`, `PATCH /api/calendars/{eventId}`, `DELETE /api/calendars/{eventId}` |
| **비고** | 조회(`GET /api/calendars`, `GET /api/calendars/{eventId}`)는 이 권한과 무관하게 같은 학원 소속 인증 사용자 전체에게 열려 있다. |

**관련 코드:**
- `CalendarController.createEvent()` — `@PreAuthorize("hasAuthority('CALENDAR:MANAGE')")`
- `CalendarController.updateEvent()` — `@PreAuthorize("hasAuthority('CALENDAR:MANAGE')")`
- `CalendarController.deleteEvent()` — `@PreAuthorize("hasAuthority('CALENDAR:MANAGE')")`
- 권한 시드: `db/migration/be5/V5.1.2__add_calendar_manage_permission.sql`

**테스트:**
- `CalendarControllerTest` — 슬라이스(`@WebMvcTest`)에서 성공 경로 검증 (`@PreAuthorize`는 이 슬라이스에서 평가되지 않음)
- `CalendarControllerPermissionIntegrationTest` — 전체 컨텍스트(`@SpringBootTest`)에서 `CALENDAR:MANAGE` 없이 403이 반환되는지 검증

---

## 권한 부여 방식

- `CALENDAR:MANAGE`는 캘린더 도메인이 별도의 위임 로직을 갖지 않는다. 기존 역할(role)-권한(permission) 매핑 관리 기능을 통해 원장이 특정 역할에 이 권한을 부여/회수한다.
- 이 권한이 시드된 이후, 학원마다 실제로 어떤 역할에 `CALENDAR:MANAGE`를 매핑할지는 학원별 운영 데이터이므로 마이그레이션이 자동으로 할당하지 않는다. **권한 시드 직후에는 별도로 역할에 매핑하기 전까지 모든 작성/수정/삭제 요청이 403으로 거부된다.**

## 권한 모듈 연동 체크리스트

- [x] `CALENDAR:MANAGE` 코드 시드
- [x] `CalendarController`의 작성/수정/삭제 API에 `@PreAuthorize` 적용
- [x] 전체 컨텍스트 통합 테스트로 403 응답 검증
- [ ] 학원별 역할에 `CALENDAR:MANAGE` 실제 매핑 (운영 데이터, 배포 후 관리자가 수행)

---

## 참고 문서

- 구현 상세: [`BUSINESS_RULES.md`](BUSINESS_RULES.md) § 접근 권한
- API 명세: [`CALENDAR_API.md`](CALENDAR_API.md) § 인증 및 권한
- 참고 템플릿: `workspace/docs/WORKSPACE_PERMISSIONS.md`

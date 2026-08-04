# ARCHITECTURE.md

## 목적

이 문서는 Spring Server의 구조와 의존성 규칙을 정의한다.

본 서버는 하나의 Spring Boot 애플리케이션으로 배포되는 모듈형 모놀리스이다. 도메인별 담당자는 자신의 도메인 패키지를 소유하고, 다른 도메인의 내부 구현을 직접 수정하거나 참조하지 않는다.

## 목표

- 비즈니스 규칙을 중심에 둔다.
- HTTP, Spring, JPA, DB, 외부 API는 구현 세부사항으로 분리한다.
- 변경 영향 범위를 줄이고 도메인 간 결합을 낮춘다.

## 도메인 소유권

- 각 도메인은 자신의 패키지 내부 코드와 데이터 모델만 수정한다.
- 다른 도메인의 내부 Service, JPA Entity, Repository, Adapter를 직접 수정하거나 참조하지 않는다.
- 타 도메인의 기능 또는 데이터가 필요하면 공개 Port를 통해 요청한다.
- 공개 Port가 없으면 대상 도메인에 필요한 계약의 추가를 요청한다.
- 공통화가 필요하더라도 특정 도메인 규칙을 임의로 Global 영역으로 이동하지 않는다.

## 공유 플랫폼 영역

- `global/`은 도메인 모듈이 아닌 공유 플랫폼 영역이다.
- 인증 정보 추출, Security Context 처리, 공통 예외 응답, 공통 설정처럼 도메인에 종속되지 않는 기술 책임만 둔다.
- 도메인 비즈니스 규칙, 도메인 데이터와 상태, JPA Entity, Repository, 도메인별 Application API를 소유하지 않는다.
- `global/`은 도메인 모듈을 참조하거나 의존하지 않는다. 도메인 모듈은 공통 기술 기능이 필요한 경우에만 `global/`에 의존할 수 있다.

## 구조

프로젝트는 레이어드 구조를 기본으로 한다. 외부 시스템 연동, 영속성 구현 교체 가능성, 또는 도메인 간 공개 계약이 필요한 도메인에 한하여 Port/Adapter 구조를 적용한다. 단순한 내부 기능만 가진 도메인은 레이어드 구조를 유지한다.

```text
presentation / adapter-in
            ↓
       application
            ↓
         domain
            ↑
infrastructure / adapter-out
```

의존성은 기본적으로 안쪽을 향한다.

```text
presentation → application → domain
infrastructure → application 또는 domain
```

## 계층 책임

| 계층 | 역할 | 포함하면 안 되는 것 |
| --- | --- | --- |
| presentation / adapter-in | HTTP 요청·응답, 요청 검증, 인증 정보 전달, Command 생성, UseCase 호출 | 도메인 규칙, DB 직접 접근 |
| application | 유스케이스 조립, 트랜잭션, Port 호출, 도메인 객체 호출, 이벤트 발행, 실행 권한 판단 | HTTP 세부사항, JPA Entity 직접 의존, 복잡한 규칙 구현 |
| domain | 도메인 모델, Aggregate, 상태 변경, 비즈니스 규칙, 도메인 예외 | Spring, JPA, HTTP, 외부 API 의존 |
| infrastructure / adapter-out | JPA, DB, 외부 API, 메시징, 파일 저장소, Port 구현 | Presentation 책임, 도메인 규칙 판단 |

## Command, Query와 API DTO

- 상태를 변경하는 Application Service는 `<Domain>Service`로 두고 Command를 입력으로 사용한다.
- 조회 전용 Application Service는 `application.query` 하위의 `<Domain>QueryService`로 분리한다. Query Service는 상태를 변경하지 않는다.
- Request와 Response는 HTTP API 계약이므로 `presentation.api.request`, `presentation.api.response`에 둔다.
- Controller는 Request를 Command로 변환해 상태 변경 Service를 호출한다.
- Query Service는 Domain Model을 반환하고, Controller가 `Response.from(domain)`으로 HTTP 응답 DTO를 생성한다.
- Application과 Domain은 Presentation의 Request, Response, Swagger 어노테이션, `GlobalApiResponse`를 직접 참조하지 않는다.
- 조회 반환 타입에 `Result`, `View` 접미사를 사용하지 않는다. HTTP 응답은 Presentation의 `Response`로 표현한다.

## 인증과 인가

- 인증 정보 추출과 Security Context 처리는 Presentation 또는 Global 영역에서 담당한다.
- 유스케이스 실행 권한 판단은 Application 또는 Domain Policy가 담당한다.
- Controller에 사용자 소유권, 역할, 접근 가능 여부 등 비즈니스 권한 규칙을 두지 않는다.
- 외부 시스템이 전달한 사용자 식별 정보 또는 권한 정보는 권한 판단의 최종 근거로 사용하지 않는다.

## 트랜잭션

- 트랜잭션의 시작과 종료는 Application 계층이 담당한다.
- 하나의 유스케이스는 필요한 상태 변경을 하나의 트랜잭션 경계 안에서 처리한다.
- 외부 API 호출, 메시지 발행 등 네트워크 I/O를 DB 트랜잭션에 장시간 포함하지 않는다.
- 외부 연동 실패를 이유로 다른 도메인의 상태를 직접 변경하지 않는다.

## Command와 Event

- Command는 실행 요청이며 실패할 수 있다.
- Event는 이미 발생한 사실이며 과거형으로 표현한다.
- 모든 상태 변경을 Event로 만들지 않는다.
- 후속 처리, 비동기 처리, 다른 도메인의 반응이 필요한 경우에만 Event를 사용한다.
- 상태 변경 이벤트는 트랜잭션이 성공적으로 완료된 뒤 발행한다.
- 이벤트 소비자는 중복 수신 가능성을 고려하여 멱등하게 처리한다.

```text
CreateOrderCommand
OrderCreatedEvent
```

## Port와 Adapter

- Application 또는 Domain은 외부 구현체가 아닌 Port에 의존한다.
- Infrastructure Adapter는 Port를 구현하여 DB, 외부 API, 다른 도메인과 연결한다.
- 외부 구현 기술이 변경되어도 Port 계약이 유지되면 핵심 비즈니스 로직은 변경하지 않는다.

```text
Application Service
→ Outbound Port
→ Infrastructure Adapter
→ DB / External API / Other Context
```

## 도메인 간 연동

- 다른 도메인의 내부 Service, JPA Entity, Repository를 직접 참조하지 않는다.
- 필요한 정보는 Port를 통해 요청한다.
- 도메인 간 전달 값은 식별자, Projection, Port 전용 Result처럼 최소한으로 유지한다.
- 상대 도메인의 내부 모델을 응답 객체에 노출하지 않는다.
- 도메인 간 연동을 위해 Entity 연관관계나 공유 Repository를 만들지 않는다.

## Domain Model과 JPA Entity

- Domain Model은 비즈니스 규칙을 표현한다.
- JPA Entity는 DB 저장 구조를 표현한다.
- 두 모델의 변환은 Persistence Adapter 내부에서 수행한다.
- Domain은 JPA 어노테이션과 영속성 기술을 알지 못한다.
- 생성일·수정일·삭제일이 필요한 JPA Entity는 직접 필드를 선언하지 않고, `global.infrastructure.persistence`의 `CreatedAtEntity`(생성일) / `BaseTimeEntity`(생성일+수정일) / `SoftDeleteTimeEntity`(생성일+수정일+삭제일)를 상속한다. 시간대 정책은 [DATABASE.md](DATABASE.md)를 따른다.

### Mapper 규칙

- Domain Model과 JPA Entity 간 변환은 MapStruct를 사용한다.
- Mapper는 `infrastructure.persistence` 하위에 둔다.
- 모든 Mapper는 `global.infrastructure.mapper.MapStructConfig`를 사용한다.
- `MapStructConfig`는 `componentModel = "spring"`, `unmappedTargetPolicy = ReportingPolicy.ERROR`로 설정한다.
- Domain Model은 private 생성자와 public Builder를 사용한다.
- JPA Entity는 Builder를 제공한다.
- Application과 Domain 계층은 JPA Entity를 직접 참조하지 않는다.

## Policy 사용 기준

- Policy는 별도 필수 계층이 아니다.
- 여러 객체에서 재사용되는 독립 규칙이 있을 때만 사용한다.
- Aggregate 내부에서 자연스럽게 처리할 수 있는 규칙은 불필요하게 Policy로 분리하지 않는다.
- Domain Policy는 Spring, JPA, HTTP에 의존하지 않는다.
- 재시도, 타임아웃, 외부 API 호출 제한은 Infrastructure 책임으로 둔다.

## 예외 처리

- 도메인 규칙 위반은 도메인 예외와 ErrorCode로 표현한다.
- Domain Exception은 HTTP 상태 코드에 의존하지 않는다.
- HTTP 상태 코드와 API 오류 응답 변환은 Presentation 또는 Global Exception Handler가 담당한다.
- 외부 연동 실패는 기술 예외를 그대로 노출하지 않고 애플리케이션 예외로 변환한다.

## 조회 전용 예외

- 관리자 화면, 통계, 대시보드처럼 여러 도메인의 데이터를 조합하는 읽기 전용 기능은 Read Model로 분리할 수 있다.
- 읽기 전용 통계는 필요에 따라 전용 Query Adapter 또는 Repository를 직접 조회할 수 있다.
- 조회 전용 로직은 다른 도메인의 상태 변경, Entity 수정, 비즈니스 규칙 수행 책임을 가지지 않는다.
- 조회 로직은 Command 처리 흐름으로 확산하지 않는다.

## 코드 리뷰 체크리스트

1. 비즈니스 규칙과 기술 세부사항이 분리되어 있는가?
2. 의존성이 Application과 Domain 방향을 침범하지 않는가?
3. Application Service가 도메인 규칙을 과도하게 직접 판단하지 않는가?
4. Domain이 Spring, JPA, HTTP, 외부 API를 알고 있지 않은가?
5. 다른 도메인의 내부 모델을 직접 참조하거나 수정하지 않는가?
6. Port가 구현 기술이 아닌 필요한 기능을 표현하는가?
7. Policy가 실제 독립 규칙인지, 불필요한 추상화인지 검토했는가?
8. 트랜잭션이 외부 네트워크 I/O에 과도하게 묶여 있지 않은가?
9. 이벤트가 트랜잭션 완료 후 발행되며, 소비자가 멱등하게 처리되는가?
10. 조회 전용 예외가 Command 로직으로 확산되지 않는가?

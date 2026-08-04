# ERROR_HANDLING.md

## 목적

도메인별 오류 코드와 예외를 일관되게 관리하고, 클라이언트에 안정적인 오류 응답을 제공한다.

## 책임 분리

```text
<Domain>ErrorCode
  └─ HTTP 상태, 오류 코드, 기본 메시지 정의

<Domain> 예외 클래스
  └─ 도메인 의미와 오류 발생 맥락 정의

global.domain.common.exception
  └─ HTTP 성격별 기반 예외와 공통 ErrorCode 계약 제공

GlobalExceptionHandler
  └─ ApplicationException의 ErrorCode를 공통 HTTP 오류 응답으로 변환
```

`GlobalExceptionHandler`는 도메인별 예외를 알 필요가 없다. 모든 도메인 예외는
`ApplicationException`을 상속하고 `ErrorCode`를 제공해야 한다.

## ErrorCode

각 도메인은 자신의 오류 코드를 `<Domain>ErrorCode` enum으로 소유한다.

```java
@Getter
@RequiredArgsConstructor
public enum WorkspaceErrorCode implements ErrorCode {

    INVALID_MEMBER(
            HttpStatus.BAD_REQUEST,
            "WORKSPACE_400_1",
            "선택할 수 없는 참여자가 포함되어 있습니다."
    ),

    NAME_CONFLICT(
            HttpStatus.CONFLICT,
            "WORKSPACE_409_1",
            "워크스페이스 이름을 생성할 수 없습니다."
    ),

    WORKSPACE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "WORKSPACE_404_1",
            "워크스페이스를 찾을 수 없습니다."
    );

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
```

오류 코드 형식은 `<DOMAIN>_<HTTP_STATUS>_<SEQUENCE>`를 사용한다.

```text
WORKSPACE_400_1
NOTICE_404_1
APPROVAL_409_1
```

## 커스텀 예외

도메인 규칙 위반은 공통 예외를 직접 사용하지 않고, 의미가 드러나는 도메인 예외로 표현한다.

```java
public class InvalidWorkspaceMemberException extends BadRequestException {

    public InvalidWorkspaceMemberException() {
        super(WorkspaceErrorCode.INVALID_MEMBER);
    }
}
```

식별자나 추가 정보가 필요한 경우에는 `addContext`로 오류 맥락을 기록한다.

```java
public class WorkspaceNotFoundException extends NotFoundException {

    public WorkspaceNotFoundException(Long workspaceId) {
        super(WorkspaceErrorCode.WORKSPACE_NOT_FOUND);
        addContext("workspaceId", workspaceId);
    }
}
```

## 공통 예외 기반 클래스

도메인 ErrorCode를 사용할 수 있도록 아래 공통 예외는 기존 생성자를 유지한 채
`protected` 생성자를 추가한다.

```java
protected ExceptionType(ErrorCode errorCode)
protected ExceptionType(ErrorCode errorCode, String message)
```

적용 대상은 다음과 같다.

```text
BadRequestException
NotFoundException
ConflictException
ForbiddenException
```

기존 `String` 또는 기본 생성자는 삭제하지 않는다. 따라서 기존 도메인의 호출부는
즉시 변경하지 않아도 호환된다.

## HTTP 상태 선택

| 기반 예외 | 사용 시점 |
| --- | --- |
| `BadRequestException` | 도메인 입력값 또는 상태가 유효하지 않음 |
| `NotFoundException` | 요청한 도메인 리소스를 찾을 수 없음 |
| `ConflictException` | 현재 상태 또는 동시성 충돌로 요청을 수행할 수 없음 |
| `ForbiddenException` | 인증은 되었지만 도메인 권한 또는 접근 권한이 없음 |

## 도메인 적용 요청 양식

다른 도메인의 담당자에게 아래 형식으로 변경을 요청한다.

```text
[대상 도메인]
<notice | approval 등>

[변경 요청]
1. <Domain>ErrorCode implements ErrorCode enum 추가
2. 도메인 의미가 드러나는 커스텀 예외 추가
3. 기존 공통 예외 직접 호출을 도메인 예외로 교체

[공통 선행 변경]
BadRequestException, NotFoundException, ConflictException, ForbiddenException에
ErrorCode 기반 protected 생성자가 필요합니다.

[주의 사항]
- GlobalExceptionHandler는 변경하지 않습니다.
- 기존 공통 예외 생성자는 유지합니다.
- 다른 도메인의 Entity, Repository, Service를 직접 참조하지 않습니다.
```

## 현재 적용 상태

| 도메인 | 상태 |
| --- | --- |
| `workspace` | 생성 기능 구현 시 도입 예정 |
| `notice` | 담당자 적용 요청 예정 |
| `approval` | 담당자 적용 요청 예정 |
| `auth` | `AuthErrorCode`, `AuthException` 사용 중이며 세부 예외 분리는 추후 검토 |

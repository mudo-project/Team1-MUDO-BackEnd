# Google 연동 도메인

## 책임과 범위

- 학원 명의의 구글 계정을 OAuth로 연결해, 학원당 하나의 구글 계정 연동 상태(연결/재연결/교체/해제/확인)를 관리한다.
- 이번 범위는 **계정 연동 관리**까지다. 템플릿 카테고리 관리, 템플릿 업로드/생성/수정/임베딩(구글 드라이브·독스·시트 실제 사용)은 포함하지 않는다(추후 별도 이슈).

## 담당자

- 담당자번호 `be5` (캘린더와 동일 담당자).

## 주요 데이터와 상태

- `GoogleAccountConnection`: 테넌트 DB당 1건. 구글 이메일, 연결한 관리자, 부여받은 scope, 암호화된 리프레시 토큰, 연결 일시, 구글 토큰 응답의 `refresh_token_expires_in`이 실제로 반환된 경우에만 저장하는 리프레시 토큰 만료 시각, 마지막 확인 일시, 마지막 확인 실패 여부를 가진다. 연결 시점에 임의 만료일을 계산하지 않는다.
- 상태(`GoogleConnectionStatus`)는 저장하지 않고 조회 시점에 계산한다: 행이 없으면 `NOT_CONNECTED`, 마지막 실제 토큰 확인이 실패했으면 `FAILED`, 구글이 반환한 실제 만료 시각이 지났으면 `EXPIRED`, 실제 만료 시각이 3일 이내면 `EXPIRING`, 그 외에는 `CONNECTED`다. 실제 만료 정보가 없으면 만료일을 추정하지 않고 `CONNECTED`로 표시한다.
- 리프레시 토큰은 평문으로 저장하지 않는다. `GoogleTokenCipher`(AES-GCM, 전용 시크릿 `GOOGLE_TOKEN_ENCRYPTION_KEY`)로 암호화해 저장하고, 조회 시 복호화한다. JWT 서명 키와 분리해, 하나가 노출돼도 다른 하나는 영향받지 않는다.

## 외부에 공개하는 Application API

- `StartGoogleAccountConnectionUseCase`, `CompleteGoogleAccountConnectionUseCase`,
  `GetGoogleAccountConnectionUseCase`, `GetGoogleAccountConnectionStatusUseCase`,
  `CheckGoogleAccountConnectionUseCase`, `DisconnectGoogleAccountUseCase`,
  `GetGoogleAccessTokenUseCase`.
- `GetGoogleAccessTokenUseCase`는 API 엔드포인트로 노출되지 않는다 — 템플릿 기능처럼 Drive/Docs/Sheets를
  직접 호출해야 하는 다른 도메인이 자바 코드에서 직접 호출하는 용도다.
- 현재 다른 도메인이 소비하는 Port는 없다.

## 의존성

- 외부 시스템: 구글 OAuth 2.0(`accounts.google.com`, `oauth2.googleapis.com`, `www.googleapis.com/oauth2/v3/userinfo`).
  `GoogleOAuthPort`(application) → `GoogleOAuthAdapter`(infrastructure)로 연결한다.
- users 도메인의 사용자 이름은 `GoogleConnectionUserDirectoryPort`를 통해서만 조회한다. Google은 users Entity·Repository를 직접 참조하지 않으며, 상태 요약 조회는 사용자 조회 없이 Google 연결 행만 사용한다.

## 필요한 환경 변수

| 변수 | 설명 | 로컬 기본값 |
| --- | --- | --- |
| `GOOGLE_CLIENT_ID` | 구글 Cloud Console OAuth 클라이언트 ID | 없음(비어 있으면 앱은 뜨지만 구글 연동 API 호출 시점에 실패) |
| `GOOGLE_CLIENT_SECRET` | 구글 OAuth 클라이언트 시크릿 | 없음(비어 있으면 앱은 뜨지만 구글 연동 API 호출 시점에 실패) |
| `GOOGLE_REDIRECT_URI` | 구글 콘솔에 등록한 콜백 URI. 이 서버의 `GET /api/google/connections/callback` 절대 경로여야 한다 | 없음(비어 있으면 앱은 뜨지만 구글 연동 API 호출 시점에 실패) |
| `GOOGLE_OAUTH_FRONTEND_REDIRECT_URI` | 콜백 처리 후 브라우저를 돌려보낼 프론트엔드 결과 페이지 | `/`(설정 전 임시값) |
| `GOOGLE_OAUTH_SCOPE` | 요청할 OAuth scope(공백 구분) | `openid https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/drive.file`(계정 식별 + 서비스가 생성·관리하는 Drive 파일 접근) |
| `GOOGLE_TOKEN_ENCRYPTION_KEY` | 리프레시 토큰 암호화 전용 키. `JWT_SECRET`과 별도 값이어야 한다 | 없음(필수 설정, 없으면 앱 시작 실패) |

`.env.local`은 로컬 전용 파일이며 git 추적에서 제외되어 있다(`git rm --cached`로 제외됨, `.gitignore`의 `.env.*` 규칙 적용). 실제 구글 Cloud 프로젝트에서 발급받은 시크릿 값을 각자 이 파일에 채워 넣어 쓰며, git에는 절대 커밋되지 않는다.

## 주의 사항

- `GoogleAccountConnectionController`의 `authorize-url`/`GET`/`check`/`disconnect`는 **원장(academy 관리자) 계정만** 호출할 수 있다. 자세한 권한 코드 정의는 [`GOOGLE_PERMISSIONS.md`](GOOGLE_PERMISSIONS.md) 참고.
- `GET /api/google/connections/callback`은 구글이 브라우저를 리다이렉트하는 대상이라 `Authorization` 헤더가 없다.
  `SecurityConfig`에서 이 경로만 `permitAll`로 열어 뒀고, 위조·재사용은 `state`(HMAC 서명, 10분 유효)로 막는다.
- "재연결"과 "계정 교체"는 별도 엔드포인트가 아니라 `authorize-url?switchAccount=true|false` 하나로 처리한다.
  `switchAccount=true`면 구글 계정 선택 화면을 강제로 띄운다(`prompt=select_account consent`).
- 재연결/계정 교체 성공 시 기존 행을 삭제하고 새 행을 만든다(교체이지 갱신이 아님). 기존 리프레시 토큰의 구글 측 폐기(revoke)는 즉시 실행하지 않고, DB 트랜잭션 커밋이 확정된 뒤 `OldGoogleRefreshTokenRevocationRequestedEvent`로 미룬다 — DB 쓰기가 실패해 롤백돼도 이미 폐기된 토큰이 남는 불일치를 막기 위함이다. 연동 해제도 동일하다.
- 연결 성공(최초 연결/재연결/계정 교체) 시 `GoogleAccountConnectedEvent`를 발행한다. 현재 구독자는 없다 — 공유파일 도메인이 구현되면 AFTER_COMMIT으로 수신해 시스템 루트를 자동 생성할 예정이다.

## 다음 단계(공유파일 기능)를 위한 참고

이번 범위에는 없지만, 나중에 공유 폴더·업로드·문서 생성·미리보기를 구현할 담당자를 위해 어떤 구글 API를 쓰게 될지 미리 정리해둔다. 공유파일은 `drive.file`로 우리 서비스가 생성·관리하는 학원 공용 폴더와 파일만 다룬다.

| 기능 | API | 대표 엔드포인트 |
| --- | --- | --- |
| 파일 목록/검색 | Drive API v3 | `GET https://www.googleapis.com/drive/v3/files?q='{폴더ID}' in parents` |
| 폴더(카테고리) 생성 | Drive API v3 | `POST https://www.googleapis.com/drive/v3/files` (`mimeType: application/vnd.google-apps.folder`) |
| 로컬 파일 업로드 | Drive API v3 | `POST https://www.googleapis.com/upload/drive/v3/files` |
| 새 문서 생성 | Docs API v1 | `POST https://docs.googleapis.com/v1/documents` |
| 새 스프레드시트 생성 | Sheets API v4 | `POST https://sheets.googleapis.com/v4/spreadsheets` |
| 새 프레젠테이션 생성 | Google Slides API | Google Slides 생성 API |

- 미리보기는 우리 서비스 파일 상세 화면에서 제공하고, Google Docs·Sheets·Slides 원본 편집은 `<iframe>`이 아니라 `Google에서 열기`로 새 탭의 실제 Google 편집기를 연다.
- `GOOGLE_OAUTH_SCOPE`의 Google API 데이터 접근 scope는 `drive.file` 하나만 사용한다. `openid`와 정식 이메일 scope(`https://www.googleapis.com/auth/userinfo.email`)는 연결 계정 식별을 위해 함께 요청한다.
- `GetGoogleAccessTokenUseCase.getAccessToken()`이 이 액세스 토큰을 반환한다. 공유파일 도메인은 Google 도메인의 리프레시 토큰이나 Entity를 직접 참조하지 않고, 이 UseCase만 소비해 Drive/Docs/Sheets/Slides API를 호출한다. 연동이 없거나 scope가 부족하거나 실제 만료된 경우에는 예외를 받아 사용자에게 재연결을 안내한다. scope 부족은 연동 상태를 `FAILED`로 바꾸지 않는다.

## 세부 문서

- [GOOGLE_API.md](GOOGLE_API.md): 요청·응답 형식
- [GOOGLE_PERMISSIONS.md](GOOGLE_PERMISSIONS.md): 권한 코드 정의
- [REVISION.md](REVISION.md): 변경 이력

# Google 연동 도메인

## 책임과 범위

- 학원 명의의 구글 계정을 OAuth로 연결해, 학원당 하나의 구글 계정 연동 상태(연결/재연결/교체/해제/확인)를 관리한다.
- 이번 범위는 **계정 연동 관리**까지다. 템플릿 카테고리 관리, 템플릿 업로드/생성/수정/임베딩(구글 드라이브·독스·시트 실제 사용)은 포함하지 않는다(추후 별도 이슈).

## 담당자

- 담당자번호 `be5` (캘린더와 동일 담당자).

## 주요 데이터와 상태

- `GoogleAccountConnection`: 학원(`academyId`)당 1건. 구글 이메일, 연결한 관리자, 부여받은 scope, 암호화된 리프레시 토큰, 연결 일시, 토큰 만료 예정 일시(연결일 + 60일), 마지막 확인 일시, 실패 여부를 가진다.
- 상태(`GoogleConnectionStatus`)는 저장하지 않고 조회 시점에 계산한다: `CONNECTED` / `EXPIRING`(만료 7일 전부터) / `EXPIRED` / `FAILED`. 행이 없으면 "연동 안 됨"이다.
- 리프레시 토큰은 평문으로 저장하지 않는다. `GoogleTokenCipher`(AES-GCM, 키는 `JWT_SECRET` 재사용)로 암호화해 저장하고, 조회 시 복호화한다.

## 외부에 공개하는 Application API

- `StartGoogleAccountConnectionUseCase`, `CompleteGoogleAccountConnectionUseCase`,
  `GetGoogleAccountConnectionUseCase`, `CheckGoogleAccountConnectionUseCase`, `DisconnectGoogleAccountUseCase`.
- 현재 다른 도메인이 소비하는 Port는 없다.

## 의존성

- 외부 시스템: 구글 OAuth 2.0(`accounts.google.com`, `oauth2.googleapis.com`, `www.googleapis.com/oauth2/v3/userinfo`).
  `GoogleOAuthPort`(application) → `GoogleOAuthAdapter`(infrastructure)로 연결한다.
- 다른 도메인 데이터를 조회하지 않는다.

## 필요한 환경 변수

| 변수 | 설명 | 로컬 기본값 |
| --- | --- | --- |
| `GOOGLE_CLIENT_ID` | 구글 Cloud Console OAuth 클라이언트 ID | 없음(필수 설정) |
| `GOOGLE_CLIENT_SECRET` | 구글 OAuth 클라이언트 시크릿 | 없음(필수 설정) |
| `GOOGLE_REDIRECT_URI` | 구글 콘솔에 등록한 콜백 URI. 이 서버의 `GET /api/google/connections/callback` 절대 경로여야 한다 | 없음(필수 설정) |
| `GOOGLE_OAUTH_FRONTEND_REDIRECT_URI` | 콜백 처리 후 브라우저를 돌려보낼 프론트엔드 결과 페이지 | `/`(설정 전 임시값) |
| `GOOGLE_OAUTH_SCOPE` | 요청할 OAuth scope(공백 구분) | `openid email drive.file documents spreadsheets` 조합 |

시크릿 값은 `.env.local`(팀 공용 로컬 파일)에 커밋하지 않는다. 각자 실제 구글 Cloud 프로젝트에서 발급받아 로컬 환경 변수 또는 별도 시크릿 관리 방식으로 주입한다.

## 주의 사항

- `GoogleAccountConnectionController`는 `authorize-url`/`check`/`disconnect`/`GET`에 대해 로그인 여부만 검사한다.
  기능명세서상 "관리자 전용"은 `users.role` 값 체계가 확정된 뒤 `@PreAuthorize`로 반영한다(`calendar` 도메인과 동일한 결정).
- `GET /api/google/connections/callback`은 구글이 브라우저를 리다이렉트하는 대상이라 `Authorization` 헤더가 없다.
  `SecurityConfig`에서 이 경로만 `permitAll`로 열어 뒀고, 위조·재사용은 `state`(HMAC 서명, 10분 유효)로 막는다.
- "재연결"과 "계정 교체"는 별도 엔드포인트가 아니라 `authorize-url?switchAccount=true|false` 하나로 처리한다.
  `switchAccount=true`면 구글 계정 선택 화면을 강제로 띄운다(`prompt=select_account consent`).
- 재연결/계정 교체 성공 시 기존 리프레시 토큰은 구글에 폐기(revoke) 요청한 뒤 기존 행을 삭제하고 새 행을 만든다(교체이지 갱신이 아님).

## 세부 문서

- [GOOGLE_API.md](GOOGLE_API.md): 요청·응답 형식

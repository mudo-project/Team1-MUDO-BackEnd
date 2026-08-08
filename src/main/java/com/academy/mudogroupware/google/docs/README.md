# Google 연동 도메인

## 책임과 범위

- 학원 명의의 구글 계정을 OAuth로 연결해, 학원당 하나의 구글 계정 연동 상태(연결/재연결/교체/해제/확인)를 관리한다.
- 이번 범위는 **계정 연동 관리**까지다. 템플릿 카테고리 관리, 템플릿 업로드/생성/수정/임베딩(구글 드라이브·독스·시트 실제 사용)은 포함하지 않는다(추후 별도 이슈).

## 담당자

- 담당자번호 `be5` (캘린더와 동일 담당자).

## 주요 데이터와 상태

- `GoogleAccountConnection`: 학원(`academyId`)당 1건. 구글 이메일, 연결한 관리자, 부여받은 scope, 암호화된 리프레시 토큰, 연결 일시, 토큰 만료 예정 일시(연결일 + 60일), 마지막 확인 일시, 실패 여부를 가진다.
- 상태(`GoogleConnectionStatus`)는 저장하지 않고 조회 시점에 계산한다: `CONNECTED` / `EXPIRING`(만료 3일 전부터) / `EXPIRED` / `FAILED`. 행이 없으면 "연동 안 됨"이다.
- 리프레시 토큰은 평문으로 저장하지 않는다. `GoogleTokenCipher`(AES-GCM, 전용 시크릿 `GOOGLE_TOKEN_ENCRYPTION_KEY`)로 암호화해 저장하고, 조회 시 복호화한다. JWT 서명 키와 분리해, 하나가 노출돼도 다른 하나는 영향받지 않는다.

## 외부에 공개하는 Application API

- `StartGoogleAccountConnectionUseCase`, `CompleteGoogleAccountConnectionUseCase`,
  `GetGoogleAccountConnectionUseCase`, `CheckGoogleAccountConnectionUseCase`, `DisconnectGoogleAccountUseCase`,
  `GetGoogleAccessTokenUseCase`.
- `GetGoogleAccessTokenUseCase`는 API 엔드포인트로 노출되지 않는다 — 템플릿 기능처럼 Drive/Docs/Sheets를
  직접 호출해야 하는 다른 도메인이 자바 코드에서 직접 호출하는 용도다.
- 현재 다른 도메인이 소비하는 Port는 없다.

## 의존성

- 외부 시스템: 구글 OAuth 2.0(`accounts.google.com`, `oauth2.googleapis.com`, `www.googleapis.com/oauth2/v3/userinfo`).
  `GoogleOAuthPort`(application) → `GoogleOAuthAdapter`(infrastructure)로 연결한다.
- 다른 도메인 데이터를 조회하지 않는다.

## 필요한 환경 변수

| 변수 | 설명 | 로컬 기본값 |
| --- | --- | --- |
| `GOOGLE_CLIENT_ID` | 구글 Cloud Console OAuth 클라이언트 ID | 없음(비어 있으면 앱은 뜨지만 구글 연동 API 호출 시점에 실패) |
| `GOOGLE_CLIENT_SECRET` | 구글 OAuth 클라이언트 시크릿 | 없음(비어 있으면 앱은 뜨지만 구글 연동 API 호출 시점에 실패) |
| `GOOGLE_REDIRECT_URI` | 구글 콘솔에 등록한 콜백 URI. 이 서버의 `GET /api/google/connections/callback` 절대 경로여야 한다 | 없음(비어 있으면 앱은 뜨지만 구글 연동 API 호출 시점에 실패) |
| `GOOGLE_OAUTH_FRONTEND_REDIRECT_URI` | 콜백 처리 후 브라우저를 돌려보낼 프론트엔드 결과 페이지 | `/`(설정 전 임시값) |
| `GOOGLE_OAUTH_SCOPE` | 요청할 OAuth scope(공백 구분) | `openid https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/drive.file https://www.googleapis.com/auth/documents https://www.googleapis.com/auth/spreadsheets`(계정 식별 + 드라이브/독스/시트 접근) |
| `GOOGLE_TOKEN_ENCRYPTION_KEY` | 리프레시 토큰 암호화 전용 키. `JWT_SECRET`과 별도 값이어야 한다 | 없음(필수 설정, 없으면 앱 시작 실패) |

시크릿 값은 `.env.local`(팀 공용 로컬 파일)에 커밋하지 않는다. 각자 실제 구글 Cloud 프로젝트에서 발급받아 로컬 환경 변수 또는 별도 시크릿 관리 방식으로 주입한다.

## 주의 사항

- `GoogleAccountConnectionController`의 `authorize-url`/`GET`/`check`/`disconnect`는 **원장(academy 관리자) 계정만** 호출할 수 있다. 자세한 권한 코드 정의는 [`GOOGLE_PERMISSIONS.md`](GOOGLE_PERMISSIONS.md) 참고.
- `GET /api/google/connections/callback`은 구글이 브라우저를 리다이렉트하는 대상이라 `Authorization` 헤더가 없다.
  `SecurityConfig`에서 이 경로만 `permitAll`로 열어 뒀고, 위조·재사용은 `state`(HMAC 서명, 10분 유효)로 막는다.
- "재연결"과 "계정 교체"는 별도 엔드포인트가 아니라 `authorize-url?switchAccount=true|false` 하나로 처리한다.
  `switchAccount=true`면 구글 계정 선택 화면을 강제로 띄운다(`prompt=select_account consent`).
- 재연결/계정 교체 성공 시 기존 리프레시 토큰은 구글에 폐기(revoke) 요청한 뒤 기존 행을 삭제하고 새 행을 만든다(교체이지 갱신이 아님).

## 다음 단계(템플릿 기능)를 위한 참고

이번 범위에는 없지만, 나중에 템플릿 카테고리·업로드·생성·수정·임베딩을 구현할 담당자를 위해 어떤 구글 API를 쓰게 될지 미리 정리해둔다.

| 기능 | API | 대표 엔드포인트 |
| --- | --- | --- |
| 파일 목록/검색 | Drive API v3 | `GET https://www.googleapis.com/drive/v3/files?q='{폴더ID}' in parents` |
| 폴더(카테고리) 생성 | Drive API v3 | `POST https://www.googleapis.com/drive/v3/files` (`mimeType: application/vnd.google-apps.folder`) |
| 기존 파일 업로드+변환 | Drive API v3 | `POST https://www.googleapis.com/upload/drive/v3/files` (multipart, docx/xlsx → 구글 형식 변환) |
| 새 문서 생성 | Docs API v1 | `POST https://docs.googleapis.com/v1/documents` |
| 새 스프레드시트 생성 | Sheets API v4 | `POST https://sheets.googleapis.com/v4/spreadsheets` |

- **미리보기·수정은 별도 API 호출이 아니다.** `https://docs.google.com/document/d/{fileId}/edit?embedded=true` (스프레드시트는 `spreadsheets/d/...`) URL을 프론트가 `<iframe>`으로 띄우면, 편집 내용은 구글 서버로 직접 저장된다. 우리 백엔드는 이 URL만 내려주면 된다.
- `GOOGLE_OAUTH_SCOPE`는 `openid email` + `drive.file`/`documents`/`spreadsheets`까지 이미 요청한다. 기존에 `openid email`만으로 연동된 계정은 저장된 `scope`가 이 요구사항을 충족하지 못해 `GET /api/google/connections` 조회 시 `status=FAILED`로 나타나며, 프론트의 "재연결" 버튼(`authorize-url` 재호출)으로 재동의받으면 해소된다.
- **주의:** 구글은 `email` scope를 요청해도 토큰 응답의 `scope` 필드에는 항상 정식 URL(`https://www.googleapis.com/auth/userinfo.email`)로 돌려준다. `deriveStatus`의 scope 비교는 문자열 비교라서, `GOOGLE_OAUTH_SCOPE`도 짧은 이름(`email`)이 아니라 이 정식 URL로 요청해야 비교가 항상 실패하는 문제를 피할 수 있다(`GoogleOAuthProperties.DEFAULT_SCOPE` 참고).
- `GetGoogleAccessTokenUseCase.getAccessToken(academyId)`가 이 액세스 토큰(1시간 유효)을 반환한다. 템플릿 기능을 만들 도메인은 이 UseCase를 직접 호출해 Drive/Docs/Sheets API 호출에 재사용하면 된다(매 호출마다 새로 발급받는 흐름). 연동이 없거나(`GoogleAccountNotConnectedException`) scope가 부족하거나 만료됐으면(`GoogleAccountConnectionInvalidException`) 예외를 던지므로, 호출하는 도메인은 이 두 예외를 사용자에게 "구글 연동이 필요합니다/재연결이 필요합니다"로 안내하면 된다.
- 이 기능은 `google` 도메인의 리프레시 토큰을 그대로 갖다 쓰는 게 아니라, `MODULES.md`의 도메인 간 조회 Port 정책에 따라 `template`(가칭) 도메인이 필요한 Port를 정의하고 `google` 도메인 담당자 동의하에 Adapter로 구현하는 방식을 검토한다.

## 세부 문서

- [GOOGLE_API.md](GOOGLE_API.md): 요청·응답 형식
- [GOOGLE_PERMISSIONS.md](GOOGLE_PERMISSIONS.md): 권한 코드 정의

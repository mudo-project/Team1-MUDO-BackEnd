# 공유파일 모듈

## 책임과 범위

학원 구성원이 함께 사용하는 Google Drive 기반 공용 자료함과 파일을 관리한다.

```text
이음 그룹웨어 - 공유파일                 ← 학원당 하나의 시스템 루트(자동 생성·보호)
├─ 수업 운영                             ← 일반 폴더
│  └─ 하위 폴더·파일
├─ 행정                                  ← 일반 폴더
│  └─ 하위 폴더·파일
└─ 강사 공용                              ← 일반 폴더
   └─ 하위 폴더·파일
```

- 이 배포 단위의 학원 공용 시스템 루트 폴더 이름은 `이음 그룹웨어 - 공유파일`로 고정한다.
- 시스템 루트는 Google 계정 연동이 성공한 직후 자동 생성한다.
- 사용자는 시스템 루트 아래에 원하는 깊이로 일반 폴더를 여러 개 만들 수 있다. 첫 화면의 루트 바로 아래 폴더는 UI에서 공용 자료함으로 부를 수 있지만, 백엔드에서는 별도 유형이 아닌 일반 폴더다.
- 시스템 루트 하위 폴더·파일은 Google Drive를 단일 원본으로 사용한다. 애플리케이션 DB에 전체 파일·폴더 목록을 복제하지 않는다.
- 애플리케이션 DB에는 이 배포 단위의 시스템 루트 Google 폴더 ID와 생성·복구 상태만 저장한다.
- Google Docs·Sheets·Slides는 Google 원본 편집기를 새 탭으로 연다. 자체 문서·스프레드시트 편집기는 범위에 포함하지 않는다.

Google 계정 교체 시 새 계정에 새 시스템 루트를 자동 생성한다. 이전 계정의 파일은 자동 이전하지 않으며, 이전 Google Drive에 그대로 남는다.

## 시스템 루트 설정 모델

`shared_file_root`는 이 배포 단위의 현재 시스템 루트 하나만 관리한다. 파일·폴더 목록을 저장하는 테이블이 아니다.

| 컬럼 | 규칙 | 의미 |
| --- | --- | --- |
| `shared_file_root_id` | 고정값 `1` PK | 단일 시스템 루트 설정 식별자 |
| `google_root_folder_id` | NULL 허용 | Drive가 발급한 현재 시스템 루트 폴더 ID. 생성 실패 시 `NULL` |
| `status` | `READY`, `FAILED` | 시스템 루트 사용 가능 상태 |
| `created_at`, `updated_at` | 공통 시간 컬럼 | 설정 생성·최근 변경 시점 |

- Google 계정 연결 전에는 `shared_file_root` 행이 없다.
- 이 서비스는 배포 단위와 DB가 학원별로 분리된 단일 테넌트 구조다. 신규 공유파일 코드와 테이블은 `academy_id` 조회·검증 조건을 두지 않는다.
- 시스템 루트 설정 행은 ID `1` 한 건만 허용한다.
- Google 계정 테이블과 FK를 두지 않는다. 공유파일은 현재 배포 단위의 루트를 관리하고, Google 모듈에는 접근 토큰만 요청한다. 기존 Google 공개 UseCase가 `academyId`를 요구하는 동안에는 그 계약 호출값으로만 전달하며, 공유파일의 테넌시 검증에는 사용하지 않는다.
- 이전 루트 ID·재생성 이력·상세 API 오류는 저장하지 않는다. 이전 계정의 파일은 Google Drive에서 관리하며, 상세 오류는 로그에 남긴다.

## 생성·상태 전이

Google 계정 연결 성공 직후 시스템 루트를 자동 생성한다. 이 처리에는 같은 Spring 애플리케이션 내부 이벤트를 사용한다.

```text
Google OAuth 연결 DB 저장
  → GoogleAccountConnectedEvent 발행
  → 공유파일 모듈이 AFTER_COMMIT으로 수신
  → Drive 시스템 루트 생성 또는 갱신
  → shared_file_root를 READY 또는 FAILED로 저장
```

| 상황 | 처리 |
| --- | --- |
| 최초 루트 생성 성공 | Google 폴더 ID와 `READY` 저장 |
| 최초 생성 또는 재생성 실패 | 폴더 ID를 `NULL`로 두고 `FAILED` 저장 |
| Google 계정 교체 | 기존 폴더 ID를 사용하지 않고 새 계정에 루트를 생성. 성공 시 새 ID와 `READY`, 실패 시 `FAILED` |
| Drive가 루트 폴더의 `404 not found`를 반환 | 루트가 실제로 없어진 것으로 보고 폴더 ID를 `NULL`로 갱신하고 `FAILED` 전환 |
| `ROOT_MANAGE` 권한자가 복구 성공 | 새 폴더 ID와 `READY` 저장 |

Google API의 일시 오류와 `401`·`403`은 루트 상태를 변경하지 않고 해당 요청만 실패 처리한다. `401`·`403`은 토큰, scope, Google 정책 등 원인이 다양해 루트 삭제를 의미하지 않는다.

## 권한

### `SHAREDFILE:MANAGE`

공유파일의 일반 업무 권한이다. 권한 보유자는 다음을 수행할 수 있다.

- 공유파일 탭 진입, 폴더·파일 목록 및 검색 결과 조회, 미리보기, 다운로드
- 시스템 루트 하위 경로에 폴더·파일 생성, 로컬 파일 업로드, Google Docs·Sheets·Slides 생성
- 시스템 루트 하위 폴더·파일의 이름 변경, 시스템 루트 내부 이동, 휴지통 삭제

시스템 루트 폴더 자체의 이름 변경·이동·삭제·재생성은 이 권한에 포함하지 않는다.

### `SHAREDFILE:ROOT_MANAGE`

시스템 루트의 **복구·재생성** 권한이다.

- 연동 직후 자동 생성이 실패했을 때 생성 재시도
- Google Drive에서 시스템 루트가 삭제된 것이 `404 not found`로 확인되었을 때 새 시스템 루트 생성

일반 구성원에게는 `SHAREDFILE:MANAGE`만 부여한다. 시스템 루트 복구를 위임받은 구성원에게는 두 권한을 모두 부여한다.

## Google 연동 경계

공유파일 모듈은 Google 모듈의 `GetGoogleAccessTokenUseCase`만 사용해 접근 토큰을 받는다.

- `GoogleAccountConnection` Entity, 리프레시 토큰, Google Repository를 직접 참조하지 않는다.
- Google 계정이 미연결·만료·실패 상태이거나 `drive.file` 권한이 없으면, 관리자에게 Google 계정 재연결을 안내한다.
- Drive API에서 받은 파일·폴더 ID는 Google Drive 내 객체의 식별자다. 시스템 루트 ID는 DB에 저장해 이후 자료함·파일을 그 아래에 생성하고 조회할 때 사용한다.
- `SHAREDFILE:MANAGE`가 없는 사용자는 Drive API 호출 전에 서비스 권한 검증에서 거부한다. 이 경우 시스템 루트 상태는 변경하지 않는다.
- 폴더·파일 ID를 받는 모든 변경 요청은 대상과 부모 폴더가 시스템 루트 하위인지 확인한다. 시스템 루트 밖의 Drive 객체는 조작하지 않는다.

## Drive 연동 구조와 기능 단위

공유파일 애플리케이션 서비스는 `GetGoogleAccessTokenUseCase`로 학원 Google 접근 토큰을 받고, `SharedFileDrivePort`를 통해서만 Google Drive를 호출한다.

```text
Controller
  → 공유파일 UseCase
    → 서비스 권한·시스템 루트·경로 검증
    → GetGoogleAccessTokenUseCase
    → SharedFileDrivePort
    → GoogleDriveAdapter
    → Google Drive API
```

`GoogleDriveAdapter`만 Drive API의 URL, 검색 쿼리, MIME type, 응답 형식을 안다. 공유파일 UseCase는 폴더 생성·파일 이동 같은 업무 동작만 호출한다.

| 기능 단위 | 권한 | 책임 |
| --- | --- | --- |
| 시스템 루트 자동 생성·갱신 | 내부 이벤트 | Google 계정 연결 성공 후 시스템 루트를 생성하고 상태를 저장 |
| 시스템 루트 재생성 | `SHAREDFILE:ROOT_MANAGE` | `FAILED` 루트의 생성 재시도 |
| 폴더 목록 조회 | `SHAREDFILE:MANAGE` | 현재 폴더의 직접 하위 폴더·파일 조회 |
| 파일·폴더 상세 조회 | `SHAREDFILE:MANAGE` | 이름, 형식, 수정 시각, 미리보기·다운로드 정보 조회 |
| 시스템 루트 전체 검색 | `SHAREDFILE:MANAGE` | 이름으로 후보를 검색하고 루트 하위 결과만 반환. 파일·폴더 전체 검색 및 유형 필터 지원 |
| 폴더 생성 | `SHAREDFILE:MANAGE` | 시스템 루트 하위의 지정 부모 폴더에 일반 폴더 생성 |
| 로컬 파일 업로드 | `SHAREDFILE:MANAGE` | 시스템 루트 하위의 지정 부모 폴더에 파일 업로드 |
| Google 파일 생성 | `SHAREDFILE:MANAGE` | Docs·Sheets·Slides 중 선택한 빈 파일을 지정 폴더에 생성 |
| 이름 변경 | `SHAREDFILE:MANAGE` | 시스템 루트 하위 폴더·파일의 이름 변경 |
| 이동 | `SHAREDFILE:MANAGE` | 시스템 루트 하위 폴더·파일을 같은 루트 내부의 다른 폴더로 이동 |
| 휴지통 삭제 | `SHAREDFILE:MANAGE` | 시스템 루트 하위 폴더·파일을 Google Drive 휴지통으로 이동 |
| 미리보기·다운로드 | `SHAREDFILE:MANAGE` | Google 새 탭 미리보기 링크를 제공하거나 다운로드용 파일 데이터를 조회 |

모든 폴더·파일 동작은 시스템 루트 자체를 변경하지 않는다. 시스템 루트는 목록의 시작점으로만 사용하며, 이름 변경·이동·삭제 대상이 될 수 없다.

## 다음 설계 항목

- HTTP API URI, 요청·응답 DTO, 오류 코드
- 업로드 파일 크기·허용 형식·다운로드 응답 정책

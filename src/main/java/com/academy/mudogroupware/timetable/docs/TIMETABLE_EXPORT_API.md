# 시간표 세트 내보내기 API

## Endpoint

`GET /api/timetables/{timetableSetId}/export`

## 인증 및 권한

- `Authorization: Bearer {accessToken}` 헤더가 필요하다. 같은 학원 소속 인증 사용자라면 누구나 호출할 수 있다(`TIMETABLE:MANAGE` 불필요).

## Query Parameter

| name | type | required | description |
| --- | --- | --- | --- |
| `format` | Enum | true | `EXCEL`/`PDF`/`PNG` 중 하나 |
| `colorClass` | String | true | 수업종류 `CLASS`의 배경색, 6자리 16진수(예: `FFCC00`) |
| `colorSpecial` | String | true | 수업종류 `SPECIAL`의 배경색 |
| `colorClinic` | String | true | 수업종류 `CLINIC`의 배경색 |
| `colorStanding` | String | true | 수업종류 `STANDING`의 배경색 |
| `colorExam` | String | true | 수업종류 `EXAM`의 배경색 |

## Success Response

HTTP `200 OK`

- 본문: 파일 바이트(`byte[]`), `GlobalApiResponse` 래핑 없음
- 헤더: `Content-Type`(포맷별), `Content-Disposition: attachment; filename="timetable_{id}.{ext}"`

| format | Content-Type | 파일명 확장자 |
| --- | --- | --- |
| `EXCEL` | `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` | `.xlsx` |
| `PDF` | `application/pdf` | `.pdf` |
| `PNG` | `image/png` | `.png` |

## Error Response

| HTTP 상태 | code | 발생 조건 |
| --- | --- | --- |
| `400 Bad Request` | `COMMON_400_1` | `format` 파라미터가 유효하지 않은 enum 값인 경우 |
| `400 Bad Request` | `TIMETABLE_400_5` | 색상 파라미터가 6자리 16진수(RRGGBB) 형식이 아닌 경우 |
| `401 Unauthorized` | `COMMON_401_1` | Access Token이 없거나 유효하지 않은 경우 |
| `404 Not Found` | `TIMETABLE_404_1` | 시간표 세트가 존재하지 않거나 다른 학원 소속인 경우 |
| `500 Internal Server Error` | `COMMON_500_1` | 처리되지 않은 서버 오류(필수 파라미터 자체가 누락된 경우도 현재는 500으로 응답한다 — `MissingServletRequestParameterException` 전역 핸들러 부재, 후속 이슈로 별도 등록) |

## Business Rules

- 화면의 필터·밀도 상태와 무관하게 세트 전체 슬롯을 요일(월~일)→시작 시각 순으로 정렬해 내보낸다.
- 레이아웃은 표(리스트) 형태다: 요일 / 시간(시작~종료) / 강의실 / 수업종류 / 강사 / 과목 / 학년.
- 수업종류별 배경색은 요청 파라미터로 지정한 값을 그대로 적용하며, 백엔드가 팔레트를 계산하지 않는다.
- 구글 스프레드시트로 저장은 이번 범위가 아니다(후속 이슈).

# 시간표 세트 내보내기 API

## Endpoint

`GET /api/timetables/{timetableSetId}/export`

## 인증 및 권한

- `Authorization: Bearer {accessToken}` 헤더가 필요하다. 같은 학원 소속 인증 사용자라면 누구나 호출할 수 있다(`TIMETABLE:MANAGE` 불필요).

## Query Parameter

| name | type | required | description |
| --- | --- | --- | --- |
| `format` | Enum | true | `EXCEL`/`PDF`/`PNG` 중 하나 |
| `colorCriterion` | Enum | true | 배경색을 어떤 기준으로 칠할지. `CLASSROOM`/`TEACHER`/`GRADE` 중 하나 |
| `colorMap` | String(JSON) | true | `colorCriterion` 기준값(강의실 코드/강사명/학년) → 6자리 16진수 색상의 JSON 객체. 예: `{"601":"FFCC00","602":"00AACC"}` |
| `density` | Enum | false (기본값 `NORMAL`) | 행 높이·글자 크기. `COMPACT`/`NORMAL`/`SPACIOUS` |
| `dayOfWeek` | Enum | false | 특정 요일만 내보냄. 생략 시 전체 요일(`EXCEL`/`PNG`만 적용, `PDF`는 항상 전체) |
| `floor` | String | false | 특정 층(`TimetableClassroom.floor`)의 강의실만 내보냄. 생략 시 전체 층(`EXCEL`/`PNG`만 적용, `PDF`는 항상 전체) |
| `classType` | Enum | false | 특정 수업종류(`CLASS`/`SPECIAL`/`CLINIC`/`STANDING`/`EXAM`)만 내보냄. 생략 시 전체(`EXCEL`/`PNG`만 적용, `PDF`는 항상 전체) |

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
| `400 Bad Request` | `COMMON_400_1` | 필수 쿼리 파라미터(`format`, `colorCriterion`, `colorMap`)가 누락되었거나 enum 값이 유효하지 않은 경우 |
| `400 Bad Request` | `TIMETABLE_400_5` | `colorMap`의 색상 값이 6자리 16진수(RRGGBB)가 아니거나, `colorMap` 자체가 올바른 JSON이 아닌 경우 |
| `400 Bad Request` | `TIMETABLE_400_6` | 내보내기 결과 이미지(PNG)가 허용 크기를 초과하는 경우 |
| `401 Unauthorized` | `COMMON_401_1` | Access Token이 없거나 유효하지 않은 경우 |
| `404 Not Found` | `TIMETABLE_404_1` | 시간표 세트가 존재하지 않거나 다른 학원 소속인 경우 |
| `500 Internal Server Error` | `COMMON_500_1` | 처리되지 않은 서버 오류 |

## Business Rules

- `EXCEL`/`PNG`는 화면에서 선택한 `dayOfWeek`/`floor`/`classType` 필터를 그대로 반영해 내보낸다(파라미터를 생략하면 해당 조건은 전체로 취급).
- `PDF`는 인쇄용 고정 산출물이므로 필터 파라미터가 있어도 항상 세트 전체 슬롯을 내보낸다.
- 세 포맷 모두 슬롯을 요일(월~일)→시작 시각 순으로 정렬한다.
- 레이아웃은 표(리스트) 형태다: 요일 / 시간(시작~종료) / 강의실 / 수업종류 / 강사 / 과목 / 학년.
- 배경색은 `colorCriterion`으로 지정한 기준(강의실 코드/강사명/학년)의 값을 키로 `colorMap`에서 조회해 적용한다. `colorMap`에 없는 값이거나 해당 슬롯의 기준 필드가 비어 있으면 흰색(기본값)으로 표시한다. 백엔드가 팔레트를 계산하지 않고 프론트가 지정한 값을 그대로 쓴다.
- `density`는 세 포맷 모두에 적용되며 행 높이·글자 크기를 조절한다(`COMPACT` < `NORMAL` < `SPACIOUS`).
- 구글 스프레드시트로 저장은 이번 범위가 아니다(후속 이슈로도 계획 없음 — 화면 설계상 별도 기능으로 확정).

# 학생 관리 API 흐름

> 기준일: 2026-08-05

## 화면 진입

```text
사용자 로그인
→ 학생 관리 탭 진입
→ GET /api/students?page=0&size=30
→ 오른쪽 학생 목록 패널 표시
→ 첫 번째 학생 또는 사용자가 선택한 학생으로 GET /api/students/{studentId}
→ 왼쪽 학생 상세 패널 표시
```

학생 관리 화면은 페이지 이동형 상세가 아니라, 오른쪽 목록에서 선택한 학생 정보를 왼쪽 상세 패널에 갱신하는 구조다.

## 학생 검색

```text
검색어 입력
→ GET /api/students?keyword=민수&page=0&size=30
→ 이름 가나다순 결과 반환
→ 오른쪽 목록 패널 갱신
→ 행 선택 시 왼쪽 상세 패널 갱신
```

검색은 학생 이름 기준으로 시작한다. 전화번호/학교 검색은 필요 시 확장한다.

## 학생 등록

```text
학생 등록 버튼 클릭
→ 이름/학년/학교/전화번호/학부모 전화번호/특이사항 입력
→ POST /api/students
→ 생성된 studentId 반환
→ GET /api/students/{studentId}
→ 왼쪽 상세 패널에 신규 학생 표시
```

학생은 `users` 계정이 아니므로 로그인 계정 생성 흐름은 없다.

## 학생 수정

```text
왼쪽 상세 패널에서 수정
→ PATCH /api/students/{studentId}
→ 204 No Content
→ GET /api/students/{studentId}
→ 상세 패널 갱신
```

## 수강 등록

```text
왼쪽 상세 패널에서 + 강의 등록
→ 강의 검색/선택
→ "결제하기" 클릭
→ POST /api/students/{studentId}/enrollments
→ enrollmentId 반환
→ GET /api/students/{studentId}
→ 현재 수강 중 강의 목록 갱신
```

"결제하기" 버튼은 추후 결제 연동을 고려한 화면명이다. 현재 백엔드는 실제 결제 없이 학생과 강의를 연결하는 수강 등록만 처리한다.

## 수강 종료

```text
현재 수강 강의에서 종료 처리
→ PATCH /api/students/{studentId}/enrollments/{enrollmentId}/end
→ 204 No Content
→ GET /api/students/{studentId}
→ 종료된 강의는 현재 수강 목록에서 제외
```

## 모듈 경계

```text
student
→ Student, Enrollment 소유
→ 학생 목록/상세/수강 등록 API 제공
→ lecture Entity/Repository 직접 참조 금지

lecture
→ 강의 정보 소유
→ 추후 공개 Port로 강의 검색/기본 정보 제공 필요

rollcall
→ 출결 기록 소유
→ 추후 student 공개 Port로 수강생 명단 조회 필요
```

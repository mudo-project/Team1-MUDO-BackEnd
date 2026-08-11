# Dataimport FastAPI AI Engine 1차 확장 설계

기준일: 2026-08-11

## 목표

학원 초기 도입 시 기존 CSV/XLSX 학생, 강의, 수강 관계 파일을 더 정확하게 해석하기 위해 AI 분석 책임을 FastAPI로 분리한다.

단, AI가 직접 DB에 저장하지 않고 Spring Boot가 권한, 검증, 초안 저장, 최종 확정을 계속 담당한다.

## 타깃 고객 판단

이 기능의 1차 타깃은 자체 전산팀과 자체 그룹웨어를 보유한 초대형 학원이 아니다.

수십 명에서 1천 명 안팎의 학생을 관리하고, 기존 엑셀/수기 장부에서 그룹웨어로 전환하려는 중소~중형 학원을 대상으로 한다.

따라서 1차에서는 5천~1만 명급 초대용량 분산 처리보다 실제 도입 편의성과 안전한 데이터 이관을 우선한다.

## 1차 범위

- Spring Boot API 흐름 유지: 업로드, 초안 조회, 초안 수정, 확정, 결과 조회
- FastAPI는 AI 분석 엔진으로만 사용
- FastAPI가 반환한 분석 결과는 Spring의 `ImportAnalysisPort` 뒤에서 처리
- FastAPI 미설정 또는 실패 시 기존 Gemini 분석 또는 deterministic parser로 fallback
- 최종 저장은 기존 `student`, `lecture` UseCase를 통해 수행
- 프론트는 FastAPI를 직접 호출하지 않고 Spring Boot만 호출

## 제외 범위

- Redis, Kafka, SQS 등 별도 큐
- 업로드 직후 비동기 Job 상태 polling
- 진행률 퍼센트
- PDF, 이미지, OCR
- FastAPI의 DB 직접 저장
- 초대용량 청크 처리
- 실패 행만 부분 재시도

## 기술 판단 근거

초기부터 큐와 비동기 분석을 넣으면 EC2 small 제약과 프로젝트 기간에 비해 운영 복잡도가 커진다.

반면 FastAPI 분석 엔진만 분리하면 AI 기술 깊이를 보여주면서도 기존 Spring 도메인 규칙과 저장 안정성을 유지할 수 있다.

이 구조는 발표에서 다음 포인트를 설명할 수 있다.

- Spring Boot와 FastAPI의 역할 분리
- AI 결과를 바로 저장하지 않는 human-in-the-loop 구조
- AI 실패 시 서비스가 실패하지 않는 fallback 구조
- 학생/강의/수강 관계를 표준 초안 모델로 정규화
- 개인정보와 운영 데이터를 Spring이 최종 통제
- 향후 대용량/OCR 필요 시 비동기 Job으로 확장 가능한 Port 경계

## 아키텍처

```text
Frontend
  -> Spring Boot /api/data-imports/onboarding/files
      -> CSV/XLSX parser
      -> FastAPI AI engine (optional)
      -> Gemini direct analyzer fallback (optional)
      -> deterministic parser fallback
      -> draft 저장
  -> Spring Boot /draft
  -> Spring Boot /confirm
      -> student/lecture 공개 UseCase로 실제 저장
```

## FastAPI 계약

Spring은 FastAPI에 이미 파싱한 sheet 목록을 JSON으로 보낸다.

```text
POST {DATA_IMPORT_AI_BASE_URL}{DATA_IMPORT_AI_PATH}
Header: X-Data-Import-Ai-Key: {DATA_IMPORT_AI_API_KEY} // 운영 필수, localhost 개발은 선택
```

요청 행에는 학생 연락처와 보호자 연락처 같은 개인정보가 포함될 수 있다. 로컬 개발에서는 `http://localhost:8000`을 허용하지만, 운영 FastAPI URL은 HTTPS여야 하며 `X-Data-Import-Ai-Key` 인증 값을 반드시 검증해야 한다.

기본 path:

```text
/api/import/analyze
```

요청은 파일 원본이 아니라 role, fileName, headers, rows를 포함한 구조화 데이터다.

```json
{
  "sheets": [
    {
      "role": "STUDENT",
      "fileName": "students.csv",
      "headers": ["학생명", "학년"],
      "rows": [
        {
          "rowNumber": 2,
          "values": {
            "학생명": "김민수",
            "학년": "고1"
          }
        }
      ]
    }
  ]
}
```

FastAPI는 표준 컬럼 매핑 또는 보정된 row 값을 반환한다.

```json
{
  "sheets": [
    {
      "role": "STUDENT",
      "fileName": "students.csv",
      "headerMappings": {
        "학생명": "name",
        "학년": "grade"
      },
      "rows": [
        {
          "rowNumber": 2,
          "values": {
            "name": "김민수",
            "grade": "HIGH_1"
          }
        }
      ]
    }
  ]
}
```

Spring은 FastAPI가 반환한 알 수 없는 컬럼명은 무시하고, 이후 필수값/상태/수강 관계 검증을 다시 수행한다.

## 환경 변수

```text
DATA_IMPORT_AI_BASE_URL=http://localhost:8000
DATA_IMPORT_AI_PATH=/api/import/analyze
DATA_IMPORT_AI_API_KEY=선택값
DATA_IMPORT_AI_CONNECT_TIMEOUT_MS=2000
DATA_IMPORT_AI_READ_TIMEOUT_MS=8000
```

`DATA_IMPORT_AI_BASE_URL`이 없으면 FastAPI 호출은 비활성화된다.

## 실패 정책

- FastAPI URL 미설정: 기존 Gemini 또는 parser로 진행
- FastAPI 응답 오류: warning log 후 기존 Gemini 또는 parser로 진행
- FastAPI 응답이 비어 있음: 기존 sheet 유지
- FastAPI가 알 수 없는 canonical header를 반환: 해당 매핑 무시
- AI 결과도 Spring의 필수값, 상태, 수강 관계 검증을 다시 통과해야 함

## 기업 질문 대응 문장

```text
저희는 초대형 학원의 자체 전산 시스템을 대체하기보다, 엑셀 기반으로 운영하던 중소~중형 학원이 그룹웨어로 전환할 때 초기 데이터 이관 부담을 줄이는 것을 1차 목표로 잡았습니다.

그래서 처음부터 큐 기반 초대용량 비동기 처리까지 도입하지 않고, FastAPI를 AI 분석 엔진으로 분리해 Spring Boot의 권한·검증·초안·저장 구조와 결합했습니다.

AI 결과는 바로 저장하지 않고 사용자가 초안을 검토한 뒤 확정해야 반영되므로, 편의성과 안전성을 함께 가져가는 구조입니다.
```

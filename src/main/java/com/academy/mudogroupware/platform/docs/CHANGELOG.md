# 📚 platform Changelog

## 2026-08-13 · 학원별 API 호출 빈도 비교 추가, 응답 속도 개선 🚀

- `GET /api/platform/api-call-frequency`가 추가됐습니다 — 전체 또는 선택 학원의 주요 업무 API 호출 빈도를 학원별로 나란히 비교해서 볼 수 있습니다. 학원 수가 늘어도 조회 속도가 느려지지 않도록 설계했습니다.
- `operational-metrics` 조회 속도가 개선됐습니다 — 이전에는 Prometheus·ECS 호출이 전부 순차로 나가 느렸는데, 병렬로 처리하도록 바꿨습니다.
- 배포 시 주입되는 학원 목록을 매번 다시 읽지 않고 캐싱하도록 내부를 개선했습니다.

자세한 설계 배경은 [REVISION.md](REVISION.md)를 참고해주세요. 🚀

## 2026-08-13 · 플랫폼 운영 대시보드 최초 추가 👑

- `PLATFORM:SUPER_ADMIN` 계정이 전체/학원별 운영 성능·자원 지표(주요 API 호출 빈도, p95 응답시간, 오류율, RDS 커넥션 예산, ECS 호스트 CPU·메모리 여유)를 조회할 수 있게 됐습니다.
- 학원별 활성 회원 수, DB·S3 데이터 보유량을 조회할 수 있게 됐습니다 — 한 번에 학원 하나씩만 조회하며, 전체 학원 비교는 이번 범위에 포함되지 않았습니다.
- 배포된 학원 목록을 조회할 수 있게 됐습니다.
- 새 도메인 테이블은 추가되지 않았습니다 — Prometheus, ECS API, S3, 자기 자신의 RDS(`information_schema`)를 실시간으로 조회합니다.
- 운영 배포에는 정확히 한 학원(dashboard host)만 이 기능을 활성화하고, 그 Task의 IAM Role에만 ECS·S3 조회 권한을 부여합니다.

자세한 설계 배경은 [REVISION.md](REVISION.md)를 참고해주세요. 👑

# DB 연결 풀 포화 Runbook

1. 해당 학원의 Hikari active/max/pending과 connection timeout을 확인한다.
2. RDS Performance Insights에서 DB Load, 상위 SQL, 잠금 대기를 확인한다.
3. RDS 전체 연결 수와 Cell 연결 예산을 확인해 다른 학원의 영향도 확인한다.
4. 느린 쿼리·긴 트랜잭션을 먼저 해결하고 단순히 Pool만 늘리지 않는다.
5. 긴급 조정 시 RDS 정상 연결 예산과 배포 Surge를 계산한 뒤 Task Definition 값을 변경한다.
6. 변경 후 부하 시험 결과와 변경 근거를 운영 이슈에 기록한다.

운영 DB에 직접 쿼리하지 않는다. 필요한 SQL은 검토 후 승인된 Migration 또는 운영 절차로 실행한다.

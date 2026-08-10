# 애플리케이션 중단·오류율 Runbook

1. ECS Service의 Running/Pending Task 수와 최근 Event를 확인한다.
2. ALB Target Group의 unhealthy 사유와 `/actuator/health` 응답을 확인한다.
3. Grafana Loki에서 해당 `tenant`, `deployment`의 배포 직후 ERROR 로그를 확인한다.
4. OOM, DB 연결 실패, 잘못된 환경변수 또는 이미지 시작 실패를 구분한다.
5. 새 Revision 문제이면 직전 정상 Task Definition Revision으로 롤백한다.
6. 복구 후 ALB 5xx, p95 응답시간, Prometheus `up`을 확인하고 장애 기록을 남긴다.

JWT, 비밀번호, 개인정보 또는 전체 로그 본문은 Slack과 이슈에 복사하지 않는다.

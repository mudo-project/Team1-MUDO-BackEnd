# 모니터링 스택 장애 Runbook

1. CloudWatch에서 모니터링 EC2 StatusCheck와 CPU·디스크 지표를 확인한다.
2. Session Manager로 접속해 Docker 서비스와 `docker compose ps`를 확인한다.
3. Grafana, Prometheus, Loki, Alertmanager 중 실패한 컨테이너의 최근 로그만 확인한다.
4. EBS 사용률, 파일 소유권, 설정 파일 오류와 내부 DNS 연결을 확인한다.
5. Alloy 장애이면 ECS Daemon Service Event와 `/mudo/prod/monitoring` 로그 그룹을 확인한다.
6. 데이터 손상 시 최신 EBS Snapshot으로 별도 복구 인스턴스를 만들고 검증 후 전환한다.

모니터링 서버가 중단돼도 CloudWatch Alarm과 SNS 경로는 계속 동작해야 한다.

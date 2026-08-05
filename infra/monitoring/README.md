# MUDO 운영 모니터링 구성

이 디렉터리는 전용 모니터링 EC2에서 실행할 Grafana, Prometheus, Loki, Alertmanager 설정의 원본이다. 운영 시크릿과 실제 Alertmanager 설정은 저장소에 커밋하지 않는다.

## 고정 이미지 버전

- Grafana `13.1.0`
- Prometheus `v3.12.0`
- Loki `3.7.2`
- Alertmanager `v0.32.1`
- Alloy `v1.18.0` (`../alloy`)

운영 반영 직전 각 공식 릴리스와 컨테이너 이미지 Digest를 다시 확인한다. 검증 없이 `latest`로 변경하지 않는다.

## 서버에서 준비할 파일

1. SSM Parameter Store에서 Grafana 관리자 계정 값을 조회한다.
2. `monitoring.env.example`을 참고해 `.env.monitoring`을 만들고 권한을 `600`으로 설정한다.
3. `alertmanager/alertmanager.template.yml`에 SSM의 Slack Webhook을 주입해 `alertmanager/alertmanager.yml`을 만들고 권한을 `600`으로 설정한다.
4. 두 결과 파일은 Git에 추가하지 않는다.

## 실행 전 확인

```bash
docker compose --env-file .env.monitoring -f docker-compose.monitoring.yml config
docker compose --env-file .env.monitoring -f docker-compose.monitoring.yml pull
docker compose --env-file .env.monitoring -f docker-compose.monitoring.yml up -d
docker compose --env-file .env.monitoring -f docker-compose.monitoring.yml ps
```

`config` 출력에는 시크릿이 포함될 수 있으므로 CI 로그나 이슈에 붙이지 않는다.

## 네트워크 전제조건

- `3000`: 기존 ALB와 모니터링 Target Group에서만 접근
- `9090`, `3100`: ECS 인스턴스 보안 그룹에서만 접근
- `9093`: 외부에서 열지 않음
- 모니터링 EC2에는 Public IP와 SSH 22를 열지 않고 Session Manager로 접속

## 로컬 검증 한계

Compose 문법은 로컬에서 검증할 수 있지만 `/opt/mudo-observability/data`의 UID/GID, ARM64 이미지 실행, SSM 조회, Slack 알림, CloudWatch IAM 권한은 실제 AWS 테스트 인스턴스에서 검증해야 한다.

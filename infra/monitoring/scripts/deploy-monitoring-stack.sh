#!/bin/bash
# SSM RunCommand로 모니터링 EC2에서 실행되는 배포 스크립트.
# 전제: infra/monitoring/의 최신 내용이 이미 /opt/mudo-observability/config에 동기화되어 있다.
set -euo pipefail

REGION="ap-northeast-2"
CONFIG_DIR="/opt/mudo-observability/config"
PARAM_PREFIX="/mudo/prod/monitoring"

cd "$CONFIG_DIR"

MONITORING_DOMAIN=$(aws ssm get-parameter --region "$REGION" \
  --name "${PARAM_PREFIX}/MONITORING_DOMAIN" --query 'Parameter.Value' --output text)
GRAFANA_ADMIN_USER=$(aws ssm get-parameter --region "$REGION" \
  --name "${PARAM_PREFIX}/GRAFANA_ADMIN_USER" --with-decryption --query 'Parameter.Value' --output text)
GRAFANA_ADMIN_PASSWORD=$(aws ssm get-parameter --region "$REGION" \
  --name "${PARAM_PREFIX}/GRAFANA_ADMIN_PASSWORD" --with-decryption --query 'Parameter.Value' --output text)
SLACK_WEBHOOK_URL=$(aws ssm get-parameter --region "$REGION" \
  --name "${PARAM_PREFIX}/SLACK_WEBHOOK_URL" --with-decryption --query 'Parameter.Value' --output text)
PROMETHEUS_RETENTION=$(aws ssm get-parameter --region "$REGION" \
  --name "${PARAM_PREFIX}/PROMETHEUS_RETENTION" --query 'Parameter.Value' --output text)

umask 077

cat > "${CONFIG_DIR}/.env.monitoring" <<EOF
MONITORING_DOMAIN=${MONITORING_DOMAIN}
GRAFANA_ADMIN_USER=${GRAFANA_ADMIN_USER}
GRAFANA_ADMIN_PASSWORD=${GRAFANA_ADMIN_PASSWORD}
PROMETHEUS_RETENTION=${PROMETHEUS_RETENTION}
OBSERVABILITY_DATA_DIR=/opt/mudo-observability/data
EOF
chmod 600 "${CONFIG_DIR}/.env.monitoring"

sed "s#<SLACK_WEBHOOK_FROM_SSM>#${SLACK_WEBHOOK_URL}#" \
  "${CONFIG_DIR}/alertmanager/alertmanager.template.yml" > "${CONFIG_DIR}/alertmanager/alertmanager.yml"
chmod 600 "${CONFIG_DIR}/alertmanager/alertmanager.yml"

for dir in prometheus loki grafana alertmanager; do
  mkdir -p "/opt/mudo-observability/data/${dir}"
done

chown -R 472:472 /opt/mudo-observability/data/grafana
chown -R 65534:65534 /opt/mudo-observability/data/prometheus
chown -R 10001:10001 /opt/mudo-observability/data/loki
chown -R 65534:65534 /opt/mudo-observability/data/alertmanager

docker compose --env-file .env.monitoring -f docker-compose.monitoring.yml pull
docker compose --env-file .env.monitoring -f docker-compose.monitoring.yml up -d
docker compose --env-file .env.monitoring -f docker-compose.monitoring.yml ps

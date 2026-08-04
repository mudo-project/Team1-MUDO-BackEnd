# 운영 적용 전 공식 릴리스와 이미지 Digest를 다시 확인하고 Task Definition에 기록한다.
FROM grafana/alloy:v1.18.0

COPY config.alloy /etc/alloy/config.alloy

ENTRYPOINT ["/bin/alloy", "run", "--storage.path=/var/lib/alloy", "/etc/alloy/config.alloy"]

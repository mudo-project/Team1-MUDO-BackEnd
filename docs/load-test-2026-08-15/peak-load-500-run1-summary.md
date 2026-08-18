# MUDO-Groupware k6 Result - peak-load-500-run1

## Test Summary

| Metric | Value |
| --- | ---: |
| http_reqs | 57292 |
| iterations | 24848 |
| checks success rate | 99.23% |
| http_req_failed | 2.92% |
| data_received bytes | 200216054 |
| data_sent bytes | 24534102 |

## Overall Duration Metrics

| Metric | avg(ms) | min(ms) | med(ms) | p90(ms) | p95(ms) | p99(ms) | max(ms) |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| http_req_duration | 29.12 | 3.07 | 20.87 | 48.75 | 82.27 |  | 1035.58 |
| http_req_waiting | 28.34 | 3.07 | 20.14 | 47.45 | 81.03 |  | 1035.58 |
| http_req_blocked | 0.13 | 0 | 0 | 0 | 0 |  | 135.39 |
| http_req_connecting | 0.10 | 0 | 0 | 0 | 0 |  | 135.39 |

## API Tagged Duration Metrics

| Metric | avg(ms) | min(ms) | med(ms) | p90(ms) | p95(ms) | p99(ms) | max(ms) |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| http_req_duration{expected_response:true} | 28.99 | 3.07 | 20.81 | 48.34 | 82.31 |  | 1035.58 |

## Thresholds And Checks

| Check | Pass | Fail |
| --- | ---: | ---: |
| ws handshake: status is 101 | 2053 | 0 |

## Metric Meaning

| Metric | Meaning |
| --- | --- |
| http_req_duration | 요청 전체 시간입니다. 클라이언트 관점의 총 응답 시간입니다. |
| http_req_waiting | 서버 응답을 기다린 시간입니다. 서버 처리, DB 처리 지연과 가까운 값입니다. |
| http_req_failed | HTTP 실패율입니다. 4xx, 5xx 또는 check 실패가 포함될 수 있습니다. |
| checks | 시나리오에서 정의한 응답 검증 성공률입니다. |
| p95 | 전체 요청 중 95%가 이 시간 이하로 끝났다는 의미입니다. 주요 비교 기준입니다. |
| p99 | tail latency 확인용입니다. 극단적으로 느린 요청을 볼 때 사용합니다. |

## Compare Guide

| Compare Point | What To Look For |
| --- | --- |
| p95 | 개선 전/후 핵심 비교 기준입니다. |
| http_req_waiting | 값이 높으면 서버 내부 처리나 DB 쿼리 병목 가능성이 큽니다. |
| http_req_failed | 실패율이 높으면 성능 개선보다 안정성 문제를 먼저 봐야 합니다. |
| API tagged duration | `type` tag 기준으로 API별 응답 시간을 분리해서 봅니다. |
| 관측 스택 | Prometheus/Grafana/Loki 등을 함께 쓰는 프로젝트라면 서버 지표·로그도 같이 확인하세요. |


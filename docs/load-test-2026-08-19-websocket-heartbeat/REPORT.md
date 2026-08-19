# WebSocket STOMP Heartbeat 도입 전/후 좀비 소켓 부하테스트 리포트

- **대상 이슈**: [#609](https://github.com/mudo-project/Team1-MUDO-BackEnd/issues/609) — WebSocket STOMP heartbeat 설정 추가 (좀비 소켓 FD 누수 방지)
- **대상 PR**: [#610](https://github.com/mudo-project/Team1-MUDO-BackEnd/pull/610) (develop 병합 완료, 커밋 `a264010c`)
- **테스트 스크립트**: `k6/scripts/99-heartbeat-repro-baseline.js` *(`.gitignore`가 `*.js`를 전역 제외하므로 전문을 [부록 A](#부록-a-테스트-스크립트-전문)에 함께 보관한다)*
- **실행 일시**: 2026-08-19 05:34 ~ 05:39 KST
- **환경**: 로컬 (Spring Boot `bootRun` 2개 동시 기동 + MySQL 8.4, 시드 `k6/seed/001` 적용)

## 결론 요약

heartbeat 미적용 서버는 **응답 없는 연결 200개를 60초 내내 단 하나도 회수하지 못했고**, heartbeat 적용 서버는 **동일 조건에서 34~40초 안에 200개 전부를 자동 회수**했다. Toxiproxy 같은 별도 장애 주입 도구 없이 k6만으로 결함과 수정 효과가 명확히 갈렸다.

> **주의**: 이 테스트는 "무응답 클라이언트를 서버가 감지·정리하는가"를 검증한 것이지, 운영 환경에서 실제 FD 누수가 진행 중임을 확인한 것은 아니다. 사전 조사 시점의 운영 서버(academy-a) 실측에서는 `CLOSE_WAIT` 소켓이 0건이었다 (아래 5장 참고).

---

## 1. 배경 — 무엇이 문제였나

`WebSocketConfig`가 `/ws` STOMP 브로커를 `enableSimpleBroker("/topic", "/queue")`만으로 등록하고 heartbeat를 전혀 설정하지 않았다. 이 상태에서는:

- **정상 종료** (탭 닫기·로그아웃): 브라우저/OS가 TCP FIN을 보내므로 커널이 정상적으로 소켓을 정리한다. **문제 없음.**
- **비정상 종료** (네트워크 단절·절전모드 진입·프로세스 강제 종료): FIN/RST가 오지 않는다. 서버는 연결이 죽었는지 확인할 수단이 없어 소켓 FD를 계속 점유한다 → **좀비 소켓 누적 → `ulimit` 도달 시 신규 연결 불가.**

`/ws` 엔드포인트 하나를 메신저·결재·알림·워크스페이스가 공유하므로, 이 결함은 워크스페이스 전용이 아니라 global 인프라 범위다.

## 2. 테스트 설계

### 시나리오

> 피크타임이 끝나고 점심시간에 직원 200명이 동시에 노트북을 덮어 절전모드로 들어간 상황

- k6 VU 200명이 각자 다른 계정(`loadtest0001`~`loadtest0200`)으로 로그인
- `/ws/websocket`에 STOMP `CONNECT`만 수행하고 **아무 토픽도 구독하지 않음**
- 이후 `HOLD_OPEN_MS`(60초) 동안 **아무 프레임도 보내지 않고 idle 유지**

### 왜 이것만으로 "무응답 클라이언트"가 재현되는가

k6 스크립트가 보내는 `CONNECT` 프레임에는 `heart-beat:10000,10000` 헤더가 들어 있다. 즉 클라이언트가 **"나도 10초마다 신호를 보내겠다"고 선언해놓고 실제로는 한 번도 보내지 않는다.** 서버 입장에서는 정확히 "약속한 heartbeat가 끊긴 클라이언트" = 좀비 후보로 보이며, 이는 절전모드로 조용히 사라진 실제 사용자와 동일한 상태다.

### 검증 지점

| 지표 | 확인 방법 |
| --- | --- |
| 서버가 연결을 스스로 끊었는가 | k6 `repro_socket_error` 이벤트(WebSocket close code `1002`) 발생 건수 |
| 얼마나 오래 붙잡고 있었는가 | k6 `ws_session_duration` (설정한 60초보다 짧으면 서버가 먼저 끊은 것) |
| 서버 측 실제 연결 수 | `netstat`으로 해당 포트의 `ESTABLISHED` 개수를 주기적으로 관측 |

## 3. 실행 절차 (재현 방법)

### 3-1. 사전 조건

시드 데이터 `k6/seed/001_role_workspace_users.sql`이 적용되어 있어야 한다 (직원 500명, `loadtest0001`~`loadtest0500`, 비밀번호 공통 `test1234`).

```bash
mysql -h 127.0.0.1 -P 3306 -u mudo -p -e "SELECT COUNT(*) FROM mudo_groupware.users WHERE id BETWEEN 1000 AND 1499;"
```

결과가 `500`이면 준비 완료.

### 3-2. 비교군(heartbeat 미적용) 서버 기동 — 8081 포트

heartbeat 커밋(`a264010c`) **직전** 커밋으로 별도 워크트리를 만들어 띄운다.

```bash
git worktree add ../heartbeat-before-fix b89566d5
```

```bash
SPRING_PROFILES_ACTIVE=local GOOGLE_TOKEN_ENCRYPTION_KEY="<로컬 임시값>" JWT_SECRET="<로컬 임시값 32바이트 이상>" SERVER_PORT=8081 ./gradlew bootRun
```

### 3-3. 적용군(heartbeat 적용) 서버 기동 — 8080 포트

```bash
SPRING_PROFILES_ACTIVE=local GOOGLE_TOKEN_ENCRYPTION_KEY="<로컬 임시값>" JWT_SECRET="<로컬 임시값 32바이트 이상>" ./gradlew bootRun
```

> `.env.local`이 없는 환경에서는 `GOOGLE_TOKEN_ENCRYPTION_KEY`, `JWT_SECRET` 두 개가 기본값 없는 필수 프로퍼티라 기동이 실패한다. 이 테스트에 한해 임시값을 주입했다(커밋 대상 아님).

### 3-4. k6 실행

heartbeat 적용군:

```bash
BASE_URL="http://localhost:8080" LOGIN_PASSWORD="test1234" VUS=200 HOLD_OPEN_MS=60000 k6 run k6/scripts/99-heartbeat-repro-baseline.js
```

heartbeat 미적용군(비교군):

```bash
BASE_URL="http://localhost:8081" LOGIN_PASSWORD="test1234" VUS=200 HOLD_OPEN_MS=60000 k6 run k6/scripts/99-heartbeat-repro-baseline.js
```

### 3-5. 서버 측 연결 수 관측

테스트 진행 중 8초 간격으로 관측한다.

```bash
while true; do echo "established=$(netstat -ano | grep ':8081' | grep -c ESTABLISHED)"; sleep 8; done
```

---

## 4. 결과

### 4-1. 핵심 비교

| 지표 | **BEFORE** (heartbeat 미적용, :8081) | **AFTER** (heartbeat 적용, :8080) |
| --- | --- | --- |
| STOMP CONNECT 성공 | 200 / 200 | 200 / 200 |
| checks 성공률 | 100.00% (800/800) | 100.00% (800/800) |
| `http_req_failed` | 0.00% (0/200) | 0.00% (0/200) |
| **서버 주도 연결 종료 건수** | **0건** | **200건** (close code `1002`) |
| **`ws_session_duration` avg** | **1m0s** (설정값 그대로) | **34.22s** (설정값보다 먼저 종료) |
| `ws_session_duration` min ~ max | 1m0s ~ 1m0s | 30.03s ~ 40.03s |
| 테스트 중 서버 측 ESTABLISHED | 800 유지 (변화 없음) | — |
| 테스트 종료 후 ESTABLISHED | 0 (k6의 정상 종료로 회수됨) | 0 (heartbeat로 이미 회수됨) |

### 4-2. 해석

**BEFORE — `ws_session_duration`이 정확히 60.0초로 고정된 것이 핵심 증거다.** min/max/평균이 전부 `1m0s`라는 것은, 200개 세션 전부가 서버 개입 없이 **k6가 설정한 시간을 꽉 채우고 클라이언트 쪽에서 종료**했다는 뜻이다. 서버는 60초 동안 무응답 연결을 단 하나도 감지하지 못했다. 이번 테스트에서는 k6가 종료 시 정상적으로 close 프레임을 보내줘서 결국 정리됐지만, 실제 절전모드처럼 close 프레임 자체가 오지 않는 상황이라면 **이 200개 연결은 무기한 남는다.**

테스트 중 관측한 서버 측 ESTABLISHED 연결 수도 14초 시점부터 56초 시점까지 계속 800으로 고정이었고, 64초 시점(k6 종료 직후)에 136 → 0으로 떨어졌다. 즉 감소의 원인은 heartbeat가 아니라 **k6의 정상 종료**였다.

**AFTER — `ws_session_duration`이 설정값 60초보다 짧은 평균 34.22초로 내려갔고, 200개 전부에 close code `1002`(protocol error)가 기록됐다.** 서버가 heartbeat 무응답을 감지해 능동적으로 연결을 끊은 것이다. 최대값도 40.03초로, 설정한 60초에 도달한 세션이 하나도 없다.

### 4-3. 원시 로그 발췌

```text
# BEFORE (:8081)
checks_succeeded...: 100.00% 800 out of 800
ws_session_duration: avg=1m0s  min=1m0s  med=1m0s  max=1m0s  p(90)=1m0s  p(95)=1m0s
ws_sessions........: 200
repro_socket_error : 0건

# AFTER (:8080)
checks_succeeded...: 100.00% 800 out of 800
ws_session_duration: avg=34.22s min=30.03s med=32.59s max=40.03s p(90)=39.66s p(95)=39.85s
ws_sessions........: 200
repro_socket_error : 200건 (전부 code 1002)
```

전체 k6 요약 출력은 아래 두 파일에 원문 그대로 보관했다.

- [`k6-summary-before-no-heartbeat.txt`](./k6-summary-before-no-heartbeat.txt)
- [`k6-summary-after-heartbeat.txt`](./k6-summary-after-heartbeat.txt)

---

## 5. 운영 서버 사전 실측 (참고)

테스트 설계 전, 운영 ECS(EC2 launch type) 컨테이너에서 실제로 FD가 누적 중인지 먼저 확인했다.

```bash
sudo docker inspect -f '{{.State.Pid}}' <app 컨테이너 ID>
```

```bash
sudo ls -la /proc/<PID>/fd | grep socket | wc -l
```

```bash
sudo cat /proc/<PID>/net/tcp
```

> `lsof`는 런타임 이미지(`eclipse-temurin:17-jre-jammy`)에 없다. 또 `/proc/net/tcp`(호스트)와 `/proc/<PID>/net/tcp`(컨테이너 네임스페이스)는 서로 다른 목록이므로 **반드시 후자를 봐야 한다.**

**결과: 소켓 FD 9개 전부 설명 가능한 정상 연결이었고, 좀비 소켓의 시그니처인 `CLOSE_WAIT`(상태 코드 `08`)은 0건이었다.**

| FD inode | 상태 | 상대 |
| --- | --- | --- |
| 2915027 | `0A` LISTEN | 자기 자신 (8080) |
| 2914937 / 2914938 / 2968780 / 2972883 | `01` ESTABLISHED | RDS `:3306` (HikariCP 커넥션 풀) |
| 2956596 | `01` ESTABLISHED | `172.17.0.1` (도커 브릿지 — 헬스체크성) |
| 나머지 2개 | — | TCP 외 소켓(UDP/유닉스 도메인)으로 추정 |

`/proc/<PID>/net/tcp`에 대량으로 보이던 `06`(TIME_WAIT) 항목들은 inode가 `0`이라 **어떤 프로세스도 소유하지 않는** 커널 자동 정리 대상이며, 우리 앱의 FD가 아니다.

**즉 이번 수정은 "실제로 새는 것을 관측해서" 고친 것이 아니라, "새지 않는다는 보장이 없어서" 선제적으로 막은 방어 조치다.** 이 결함은 클라이언트가 실제로 비정상 종료했을 때만 트리거되므로, 관측되지 않았다는 것이 안전하다는 뜻은 아니다.

---

## 6. 적용한 수정

`WebSocketConfig`에 heartbeat 전용 스케줄러를 붙이고 브로커에 10초/10초 주기를 등록했다.

```java
// 죽은 WebSocket 연결이 heartbeat 무응답으로 감지·정리되도록 전용 스케줄러를 둔다.
// 기존 @Scheduled 작업(SchedulingConfig)과 스레드 풀을 공유하지 않기 위해 별도로 만든다.
private final ThreadPoolTaskScheduler heartbeatScheduler = new ThreadPoolTaskScheduler();

@PostConstruct
void initializeHeartbeatScheduler() {
  heartbeatScheduler.setPoolSize(1);
  heartbeatScheduler.setThreadNamePrefix("ws-heartbeat-");
  heartbeatScheduler.initialize();
}

@PreDestroy
void shutdownHeartbeatScheduler() {
  heartbeatScheduler.shutdown();
}

public void configureMessageBroker(MessageBrokerRegistry r) {
  r.enableSimpleBroker("/topic", "/queue")
      .setHeartbeatValue(new long[] {10000, 10000})
      .setTaskScheduler(heartbeatScheduler);
  r.setApplicationDestinationPrefixes("/app");
  r.setUserDestinationPrefix("/user");
}
```

### 프론트엔드 측 필요 조건

STOMP heartbeat는 `CONNECT` 시점에 서버·클라이언트가 주기를 **협상**하는 구조라, 한쪽이 `0`을 선언하면 그 방향은 무효가 된다. 따라서 **백엔드 설정만으로는 완성되지 않는다.**

```javascript
const client = new Client({
  webSocketFactory: () => new SockJS('/ws'),
  heartbeatOutgoing: 10000,
  heartbeatIncoming: 10000,
});
```

`@stomp/stompjs`는 두 값의 **기본값이 이미 `10000`**이므로(`client.ts`의 `public heartbeatIncoming: number = 10000`), 명시적으로 `0`으로 끈 코드가 없다면 프론트는 추가 작업 없이 이미 준비된 상태다. 다만 라이브러리 버전 업 시 기본값 변경에 영향받지 않도록 명시적으로 선언해두는 편을 권장한다.

---

## 7. 테스트 중 발견한 함정 (재현 시 참고)

### 7-1. VU 200명 동시 로그인 시 커넥션 거부

`per-vu-iterations` executor는 VU 200개를 거의 동시에 시작시킨다. 첫 실행에서 200개 로그인 요청이 한꺼번에 몰려 **70건이 `connection actively refused`로 실패**했다 (로컬 Tomcat backlog 한계).

**해결**: 로그인 시점에 0~5초 랜덤 지터를 넣었다. 실제로도 200명이 같은 밀리초에 로그인하지는 않으며, 나중에 Toxiproxy로 "동시에 조용히 끊기"를 흉내낼 때는 **이미 다 붙어 있는 연결을 한 번에 얼리는 것**이라 이 지터와 무관하다.

```javascript
sleep(Math.random() * 5);
```

### 7-2. heartbeat 커밋이 이미 develop에 병합된 상태

비교군 워크트리를 `develop` 기준으로 만들면 **이미 heartbeat가 적용된 상태**라 대조가 되지 않는다. 반드시 병합 직전 커밋(`b89566d5`)을 명시적으로 체크아웃해야 한다.

### 7-3. `/proc/net/tcp` vs `/proc/<PID>/net/tcp`

운영 서버 실측 시 호스트에서 `/proc/net/tcp`를 읽으면 **컨테이너의 네트워크 네임스페이스가 아닌 호스트 것**이 나온다. inode가 하나도 겹치지 않는 것으로 알아챌 수 있다.

### 7-4. TCP 상태 코드 혼동 주의

| 코드 | 상태 | 의미 |
| --- | --- | --- |
| `01` | ESTABLISHED | 정상 연결 |
| `06` | TIME_WAIT | 커널이 타이머로 자동 정리 중 (앱 FD 아님) |
| **`08`** | **CLOSE_WAIT** | **좀비 소켓의 시그니처 — 상대는 끊었는데 앱이 `close()`를 안 부른 상태** |
| `0A` | LISTEN | 수신 대기 |

---

## 8. 남은 과제

- [ ] **프론트엔드 STOMP 클라이언트 heartbeat 설정 확인/명시** — 별도 저장소, 기본값상 이미 동작 중일 가능성이 높으나 명시적 선언 권장
- [ ] **배포 후 운영 FD 추이 모니터링** — Prometheus `process_open_fds` 또는 `/proc/<PID>/fd` 개수로, heartbeat 적용 전후 우상향 패턴이 사라지는지 관찰. 인위적 재현보다 이쪽이 실효성 검증에 더 적합하다.
- [ ] **Toxiproxy 기반 완전 재현 (선택)** — Docker Desktop이 필요해 이번 회차에서는 미실시. `timeout` toxic으로 TCP 자체를 먹통으로 만들어 "close 프레임조차 오지 않는" 상황까지 재현할 수 있으나, 위 결과의 결론을 바꾸지는 않을 것으로 판단된다.

---

## 부록 A. 테스트 스크립트 전문

`k6/scripts/99-heartbeat-repro-baseline.js` — 저장소 `.gitignore`가 `*.js`를 전역 제외하므로(`.gitignore:188`) 파일 대신 전문을 여기에 보관한다. 재현 시 이 내용을 그대로 해당 경로에 저장하면 된다.

```javascript
// k6/scripts/99-heartbeat-repro-baseline.js
// ------------------------------------------------------------
// [임시/재현용 스크립트 — 정식 부하테스트 시나리오 아님]
//
// STOMP heartbeat 도입(WebSocketConfig, feat/websocket-heartbeat) 검증을 위한
// 1단계 baseline: Toxiproxy 없이, k6가 실제로 STOMP 연결 N개를 "정상 종료"까지
// 동시에 붙잡고 있을 수 있는지만 확인한다.
//
// 시나리오: 피크타임 직후 점심시간 — VU 200명이 로그인해서 STOMP CONNECT만 하고
// 아무 토픽도 구독하지 않은 채 DURATION 동안 idle 상태로 연결을 유지한다.
// (Toxiproxy로 "조용히 끊기"를 흉내내는 건 2단계 — 이 스크립트는 정상 종료라
//  heartbeat 유무와 무관하게 새지 않는 게 정상이다. 여기서 확인하려는 건
//  "k6/서버가 200개 동시 연결을 실제로 버틸 수 있는가"뿐이다.)
//
// PowerShell 실행 예시:
//   $env:BASE_URL='http://localhost:8080'
//   $env:LOGIN_PASSWORD='test1234'
//   k6 run .\k6\scripts\99-heartbeat-repro-baseline.js
// ------------------------------------------------------------

import ws from 'k6/ws';
import { check, sleep } from 'k6';
import { login } from '../lib/clients/auth-client.js';
import { BASE_URL, LOGIN_PASSWORD } from '../lib/config.js';
import { connectFrame, decodeStompFrames } from '../lib/stomp.js';

const VUS = Number(__ENV.VUS || 200);
const HOLD_OPEN_MS = Number(__ENV.HOLD_OPEN_MS || 60000); // 연결을 유지할 시간
const WS_PATH = '/ws/websocket'; // 00-ws-check.js로 확인된 경로

export const options = {
    scenarios: {
        hold_open_connections: {
            executor: 'per-vu-iterations',
            vus: VUS,
            iterations: 1, // VU마다 딱 1개 연결만 맺고 유지
            maxDuration: `${Math.ceil(HOLD_OPEN_MS / 1000) + 30}s`,
        },
    },
};

// VU마다 겹치지 않게 계정을 배분한다 (loadtest0001 ~ loadtest0500 중 VU 번호 사용).
export default function () {
    // per-vu-iterations는 VU 200개가 거의 동시에 시작된다. 로컬 Tomcat 커넥션 backlog가
    // 200개 동시 로그인 요청을 못 받아 "connection actively refused"가 났던 문제를 피하려고,
    // 로그인 시점을 0~5초 사이로 흩뿌린다(실제로도 200명이 정확히 같은 밀리초에 로그인하진
    // 않는다 — 나중에 Toxiproxy로 "동시에 조용히 끊기"를 흉내낼 때는 이미 다 붙어있는
    // 연결을 한번에 얼리는 거라 이 지터와 무관하다).
    sleep(Math.random() * 5);

    const accountNumber = ((__VU - 1) % 500) + 1;
    const username = `loadtest${String(accountNumber).padStart(4, '0')}`;
    const accessToken = login(username, LOGIN_PASSWORD);

    const url = BASE_URL.replace(/^http/, 'ws') + WS_PATH;
    let stompConnected = false;

    const res = ws.connect(url, { headers: { Cookie: `accessToken=${accessToken}` } }, (socket) => {
        socket.on('open', () => {
            socket.send(connectFrame());
        });

        socket.on('message', (raw) => {
            const frames = decodeStompFrames(raw);
            for (const frame of frames) {
                if (frame.command === 'CONNECTED') {
                    stompConnected = true;
                }
            }
        });

        socket.on('error', (e) => {
            console.log(`event=repro_socket_error vu=${__VU} error=${JSON.stringify(e)}`);
        });

        // 여기서 아무것도 안 하고 HOLD_OPEN_MS 동안 그냥 버틴다 (idle 연결 유지).
        // 시간이 다 되면 정상적으로 close() — 이번 baseline은 "정상 종료" 시나리오다.
        socket.setTimeout(() => {
            socket.close();
        }, HOLD_OPEN_MS);
    });

    check(res, {
        'ws upgrade status is 101': (r) => r && r.status === 101,
    });
    check(null, {
        'stomp CONNECTED received': () => stompConnected,
    });

    sleep(1);
}
```

# k6 Load Tests

이 프로젝트의 k6 부하 테스트 스크립트 모음입니다.

## Prerequisites

### k6 설치

```bash
# macOS
brew install k6

# Docker (설치 없이 실행)
docker run --rm -i grafana/k6 run - <script.js
```

### 서비스 실행

k6 테스트를 실행하기 전에 대상 서비스가 동작 중이어야 합니다.

```bash
# 인프라 (MySQL, Redis, Kafka)
docker-compose up -d

# Commerce API
./gradlew :apps:commerce-api:bootRun
```

---

## 테스트 목록

| 스크립트 | 대상 | 총 소요 시간 | 설명 |
|---------|------|-------------|------|
| `pg-benchmark.js` | PG Simulator (`:8082`) | ~2분 15초 | 결제 요청 처리량 & 상태 조회 |
| `queue-benchmark.js` | Commerce API (`:8080`) | ~2분 | 대기열 진입, Polling, 주문 전체 플로우 |

---

## 1. PG Benchmark (`pg-benchmark.js`)

PG(Payment Gateway) 시뮬레이터의 결제 요청 처리량과 상태 조회 성능을 측정합니다.

### 시나리오

| 시나리오 | VUs | 시간 | 설명 |
|---------|-----|------|------|
| `payment_request` | 1 → 10 → 30 → 50 → 0 | 100초 | 결제 요청 부하 (ramping-vus) |
| `status_check` | 10 (constant) | 30초 | 결제 상태 조회 부하 (105초 후 시작) |

### Thresholds

- `http_req_duration` — p(95) < 3000ms
- `http_req_failed` — rate < 50% (PG 60% 성공률 기준)

### 실행

```bash
k6 run k6/pg-benchmark.js
```

### Custom Metrics

| Metric | 설명 |
|--------|------|
| `payment_success` | 결제 성공 건수 |
| `payment_fail` | 결제 실패 건수 |
| `status_pending` | 조회 시 PENDING 상태 건수 |
| `status_success_result` | 조회 시 SUCCESS 상태 건수 |
| `status_failed_result` | 조회 시 FAILED 상태 건수 |
| `status_check_duration` | 상태 조회 응답 시간 |

### 사전 조건

- PG Simulator가 `localhost:8082`에서 실행 중
- Commerce API가 `localhost:8080`에서 실행 중 (callback 수신용)

---

## 2. Queue Benchmark (`queue-benchmark.js`)

대기열 시스템의 진입 폭증, Polling 부하, 주문 전체 플로우를 순차적으로 측정합니다.

### 시나리오

| 시나리오 | VUs | 시간 | 시작 시점 | 설명 |
|---------|-----|------|----------|------|
| `queue_flood` | 0 → 100 → 500 → 1000 → 0 | 50초 | 0초 | 대기열 진입 폭증 |
| `polling_load` | 200 (constant) | 30초 | 55초 | 순번 Polling 부하 |
| `order_with_token` | 50 (constant) | 30초 | 90초 | 대기열 → 토큰 → 주문 전체 플로우 |

### Thresholds

| Threshold | 기준 | 설명 |
|-----------|------|------|
| `http_req_duration` | p(95) < 2000ms | 전체 요청 응답 시간 |
| `queue_enter` | p(99) < 1000ms | 대기열 진입 응답 시간 |
| `queue_position` | p(99) < 500ms | 순번 조회 응답 시간 |
| `http_req_failed` | rate < 30% | 전체 실패율 |

### 실행

```bash
# 기본 실행 (localhost:8080, testuser/TestPass1!)
k6 run k6/queue-benchmark.js

# 환경 변수 지정
k6 run \
  -e BASE_URL=http://localhost:8080 \
  -e LOGIN_ID=testuser \
  -e LOGIN_PW='TestPass1!' \
  k6/queue-benchmark.js
```

### 환경 변수

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `BASE_URL` | `http://localhost:8080` | Commerce API 주소 |
| `LOGIN_ID` | `testuser` | 테스트 유저 loginId 접두사 (VU별 `{LOGIN_ID}-{VU번호}` 생성) |
| `LOGIN_PW` | `TestPass1!` | 테스트 유저 비밀번호 |

### Custom Metrics

| Metric | 설명 |
|--------|------|
| `queue_enter_success` | 대기열 진입 성공 (신규) |
| `queue_enter_duplicate` | 대기열 중복 진입 (이미 대기 중) |
| `position_check_success` | 순번 조회 성공 |
| `token_received` | 입장 토큰 수신 |
| `order_success` | 토큰으로 주문 성공 |
| `order_rejected` | 주문 실패 (토큰 미발급 or 거부) |
| `wait_time_seconds` | 예상 대기 시간 분포 |
| `order_throughput_rate` | 주문 성공률 |

### 사전 조건

- Commerce API가 실행 중이고 `queue.scheduler.enabled=true` (기본값)
- 테스트 유저가 사전 생성되어 있어야 함 (`testuser-1` ~ `testuser-1000`)
- 주문 시나리오(`order_with_token`)를 테스트하려면 상품/재고 데이터도 필요

### 테스트 데이터 준비 예시

```bash
# 유저 1000명 생성 (API 호출 또는 DB 직접 삽입)
for i in $(seq 1 1000); do
  curl -s -X POST http://localhost:8080/api/v1/users \
    -H 'Content-Type: application/json' \
    -d "{
      \"userId\": \"testuser-$i\",
      \"name\": \"Test User $i\",
      \"email\": \"testuser$i@test.com\",
      \"password\": \"TestPass1!\",
      \"birthDate\": \"1990-01-01\"
    }" > /dev/null
done

# 상품 + 재고 생성 (주문 시나리오용)
# Admin API 또는 DB 직접 삽입으로 productId=1 상품 및 충분한 재고 확보
```

---

## 결과 확인

모든 테스트 결과는 `k6/results/` 디렉토리에 타임스탬프와 함께 저장됩니다.

```bash
ls k6/results/
# pg-benchmark-2026-03-18T13-17-21.txt
# queue-benchmark-2026-04-01T14-30-00.txt
```

### 결과 해석 포인트

**PG Benchmark**
- `payment_success` vs `payment_fail` — PG 성공률이 기대치(~60%)에 부합하는지
- `status_check_duration` — 상태 조회 지연이 콜백 방식 대비 합리적인지

**Queue Benchmark**
- `queue_enter` p(99) — 1000 VUs 동시 진입 시에도 1초 이내 응답하는지
- `queue_position` p(99) — Redis 기반 순번 조회가 500ms 이내인지
- `token_received` — 스케줄러가 정상적으로 토큰을 발급하고 있는지
- `order_throughput_rate` — 토큰 발급 → 주문 성공 전환율
- `wait_time_seconds` — 유저 체감 대기 시간 분포

---

## 개별 시나리오만 실행

k6는 `--scenario` 플래그로 특정 시나리오만 실행할 수 없지만, 스크립트를 수정하거나 아래처럼 VU/duration을 오버라이드할 수 있습니다.

```bash
# 가볍게 smoke test (VU 5, 10초)
k6 run --vus 5 --duration 10s k6/queue-benchmark.js
```

> **Note:** `--vus`/`--duration` 오버라이드는 `scenarios`가 정의된 스크립트에서는 무시됩니다.
> 개별 시나리오 테스트가 필요하면 스크립트의 `scenarios` 객체에서 불필요한 시나리오를 주석 처리하세요.

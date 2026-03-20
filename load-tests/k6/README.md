# K6 부하 테스트 - Loop PG 성능 분석

## 개요
Loop PG의 500ms 지연을 고려하여 최적의 timeout/circuit breaker 설정을 결정하기 위한 k6 부하 테스트

## 준비 사항

### 1. K6 설치
```bash
# macOS
brew install k6

# Linux
sudo apt-get install k6

# Windows
choco install k6
```

### 2. 서비스 실행
터미널 1 - commerce-api 실행:
```bash
cd /Users/chuljoongkim/Documents/loopers/loop-pack-be-l2-vol3-kotlin
./gradlew -p apps/commerce-api bootRun --args='--spring.profiles.active=local'
# 또는
# ./gradlew :apps:commerce-api:bootRun
```

터미널 2 - Loop PG 모의 서버 실행 (필요시):
```bash
# src/test/kotlin/com/loopers/infrastructure/payment/pg/LoopPaymentClientE2ETest.kt 참고
# localhost:8083에서 실행되어야 함
```

## 테스트 시나리오

### 시나리오 1: 정상 트래픽 (평상시)
```bash
k6 run load-tests/k6/payment-request.js --scenario normal
```
- VU: 0 → 10 → 50 → 10 → 0
- 총 시간: ~70초
- 측정: 평균 응답 시간, p95, p99

### 시나리오 2: 500ms 지연 상황
Loop PG이 500ms 지연 생성:
```bash
k6 run load-tests/k6/payment-request.js --scenario delay_scenario -e SCENARIO=delay-500ms
```
- VU: 20 (상수)
- 시간: 60초
- 측정: slow call rate (≥500ms)

### 시나리오 3: 타임아웃 상황
Loop PG이 장시간 응답 지연:
```bash
k6 run load-tests/k6/payment-request.js --scenario timeout_scenario -e SCENARIO=timeout
```
- VU: 10 (상수)
- 시간: 45초
- 측정: timeout 발생률, circuit breaker 동작 확인

## 실행 방법

### 기본 실행 (모든 시나리오)
```bash
k6 run load-tests/k6/payment-request.js
```

### 특정 시나리오만 실행
```bash
k6 run load-tests/k6/payment-request.js --scenario normal
k6 run load-tests/k6/payment-request.js --scenario delay_scenario
k6 run load-tests/k6/payment-request.js --scenario timeout_scenario
```

### 결과를 파일로 저장
```bash
k6 run load-tests/k6/payment-request.js --out json=load-tests/results/$(date +%Y%m%d_%H%M%S).json
```

## 성능 메트릭 해석

### 주요 메트릭
- **response_time**: 응답 시간 (Trend)
  - avg: 평균 응답 시간
  - p95: 95 percentile (상위 5% 제외 최대 시간)
  - p99: 99 percentile (상위 1% 제외 최대 시간)

- **slow_calls**: 500ms 이상 소요된 호출 수

- **fast_calls**: 500ms 이하 소요된 호출 수

- **errors**: 실패한 요청 수

### 해석 가이드
**현재 설정 (read timeout: 10초)**
- Slow call threshold: 5초 (500ms 지연 감지 안 함)
- 많은 호출이 500ms 지연 발생해도 circuit breaker가 반응하지 않음

**권장 설정 (read timeout: 1초)**
- Slow call threshold: 800ms 설정
- 500ms 지연이 발생하면 circuit breaker가 감지
- Retry 정책으로 빠른 복구

## 결과 분석 및 결정

테스트 결과를 다음 기준으로 평가:

| 메트릭 | 목표 | 설정 |
|--------|------|------|
| 평균 응답시간 | < 600ms | timeout: 1s |
| p95 응답시간 | < 1000ms | 충분함 |
| slow call rate | < 10% | threshold: 800ms |
| error rate | < 1% | retry: 2회, retry delay: 200ms |

## 참고

- Loop PG 최대 지연: 500ms (예상)
- 현재 timeout: 10초 (과하지 않음, 단 slow call detection 미흡)
- 권장 timeout: 1초 (빠른 실패와 retry, 재시도 전략과 결합)
- Circuit breaker 역할: slow call rate 감시 (필요시 빠른 circuit open)

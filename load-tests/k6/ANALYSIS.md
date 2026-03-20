# Loop PG Timeout/CircuitBreaker 설정 분석 및 최적화

## 배경
- Loop PG는 최대 500ms 지연 발생 가능
- 현재 설정이 이를 제대로 반영하지 못함
- k6 부하 테스트로 실제 성능 기반 설정 최적화

---

## 현재 설정 분석

### WebClient Timeout
```yaml
pg:
  timeout:
    connect-ms: 5000      # 5초
    read-sec: 10          # 10초
    write-sec: 10         # 10초
```

**문제점**:
- read timeout이 10초는 500ms 지연에 비해 20배 과함
- 빠른 실패(fail-fast) 전략 부재
- timeout이 길면 요청당 대기 시간이 길어져 처리량 감소

### Circuit Breaker 설정
```yaml
resilience4j:
  circuitbreaker:
    instances:
      loop-pg-payment:
        slowCallDurationThreshold: 5s          # 5초 초과시만 slow call로 카운트
        slowCallRateThreshold: 100             # 모든 호출이 5초 초과해야 circuit open
        failureRateThreshold: 50
```

**문제점**:
- slowCallDurationThreshold: 5초 > 500ms 지연 × 10배 차이
- 500ms 지연은 감지되지 않음 (정상 범위로 간주)
- slow call detection이 작동하지 않음

### Retry 설정
```yaml
resilience4j:
  retry:
    instances:
      loop-pg-payment:
        maxAttempts: 3
        waitDuration: 500ms
```

**평가**:
- 기본값으로는 합리적
- 하지만 read timeout이 길면 재시도 전략의 이점 감소
- 예: 1회 실패 10초 + 2회 재시도 10초 × 2 = 30초 (너무 김)

---

## 권장 설정 (Loop PG 500ms 지연 고려)

### 1단계: WebClient Timeout 단축
```yaml
pg:
  timeout:
    connect-ms: 1000      # 1초 (연결 지연 + 여유)
    read-sec: 1           # 1초 (500ms + 500ms 버퍼)
    write-sec: 1          # 1초
```

**근거**:
- 500ms + 500ms 안전마진 = 1000ms
- 빠른 실패로 재시도 가능성 높음
- 응답 대기 시간 감소

### 2단계: Circuit Breaker 민감도 조정
```yaml
resilience4j:
  circuitbreaker:
    instances:
      loop-pg-payment:
        # 500ms 지연을 감지하기 위해 threshold 단축
        slowCallDurationThreshold: 800ms       # 500ms + 300ms 버퍼
        slowCallRateThreshold: 50              # 50% 이상이 800ms 초과시 주의

        # 빠른 circuit open/half-open 전환
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        permittedNumberOfCallsInHalfOpenState: 3
        waitDurationInOpenState: 30s           # 30초 후 재시도
        failureRateThreshold: 50               # 50% 실패율시 circuit open
```

**근거**:
- slowCallDurationThreshold: 800ms로 설정하면 500ms 지연 감지 가능
- slowCallRateThreshold: 50으로 설정하면 과반이 지연될 때 반응

### 3단계: Retry 전략 최적화
```yaml
resilience4j:
  retry:
    instances:
      loop-pg-payment:
        maxAttempts: 2           # 3회 → 2회 (timeout 단축으로 충분)
        waitDuration: 200ms      # 500ms → 200ms (빠른 재시도)
        retryExceptions:
          - java.lang.Exception
```

**근거**:
- timeout이 1초이므로 2회 재시도 = 최대 3초 (훨씬 빠름)
- 200ms 대기는 일시적 지연 복구에 충분
- 과도한 재시도는 PG에 추가 부하

---

## 동작 비교

### 시나리오: 500ms 지연 발생

#### Before (현재 설정)
```
요청 발송
  ↓ (10초 대기)
[Circuit Breaker 반응 없음] ← 5초 threshold 미도달
  ↓
응답 수신 (500ms + 네트워크)
  → 계속 같은 경로 사용
  → 모든 요청이 느린 응답 처리
  → 사용자 입장에서 느린 시스템
```

**Result**: 3회 요청 × ~10초 = ~30초 대기

#### After (권장 설정)
```
요청 발송
  ↓ (1초 대기)
[Circuit Breaker 감지] ← 800ms threshold 도달
  ↓ (slowCallRateThreshold 확인)
Slow call rate가 50% 초과
  → Circuit Open (또는 Half-Open)
  ↓
Fallback 호출 또는 빠른 재시도
  → 대체 경로 사용 또는 PG 복구 대기
```

**Result**: 2회 시도 × ~1초 + 재대기 = ~2-3초 (10배 빠름)

---

## K6 테스트로 검증할 항목

### 1. 응답 시간 분포
- **측정**: avg, p50, p95, p99
- **목표**:
  - avg < 600ms (500ms + 버퍼)
  - p95 < 1000ms
  - p99 < 1500ms

### 2. Slow Call 감지율
- **측정**: 500ms 이상 소요된 호출의 비율
- **목표**:
  - 정상 상황: < 5%
  - 500ms 지연 상황: > 80%

### 3. Error Rate
- **측정**: 실패(timeout, exception) 호출의 비율
- **목표**: < 1%

### 4. 동시성 처리
- **측정**: VU 10 → 50 → 10 단계별 응답 시간
- **목표**: VU 증가에 따른 응답시간 선형 증가 (지수적 증가 아님)

---

## 설정 적용 순서

### Phase 1: 위험도 낮음 (바로 적용 가능)
```yaml
# 1. WebClient timeout 단축 (1초로)
pg.timeout.read-sec: 10 → 1

# 2. Retry 전략 조정
resilience4j.retry.maxAttempts: 3 → 2
resilience4j.retry.waitDuration: 500ms → 200ms
```

### Phase 2: 중간 위험도 (모니터링 후 적용)
```yaml
# 3. Circuit Breaker threshold 조정
resilience4j.circuitbreaker.slowCallDurationThreshold: 5s → 800ms
resilience4j.circuitbreaker.slowCallRateThreshold: 100 → 50
```

### Phase 3: 검증 (k6 테스트)
- 각 단계 후 k6 테스트 실행
- 메트릭 확인 및 로그 분석
- 필요시 미세 조정

---

## 예상 효과

| 메트릭 | Before | After | 개선 |
|--------|--------|-------|------|
| 평균 응답시간 | ~10초 | ~600ms | 94% ↓ |
| p95 응답시간 | ~10초 | ~1초 | 90% ↓ |
| Slow call detection | 불가 | 가능 | ✅ |
| Circuit breaker 반응 | 느림 | 빠름 | ✅ |
| 전체 처리 시간 (3회) | ~30초 | ~2-3초 | 93% ↓ |

---

## 주의사항

1. **Timeout 단축의 영향**
   - 정상적인 500ms 지연도 재시도 유발 가능
   - 재시도를 통해 결과적으로 성공률 유지

2. **Circuit Breaker와 Retry의 상호작용**
   - Circuit breaker open 상태에서는 retry 미실행 (fallback만)
   - 의도된 동작 (cascade 실패 방지)

3. **모니터링 필수**
   - 설정 변경 후 메트릭 지속 모니터링
   - 필요시 미세 조정 (threshold, retry 횟수 등)

4. **테스트 환경 vs 프로덕션**
   - 테스트: k6 부하 테스트로 검증
   - 프로덕션: 단계적 적용 (Canary, Blue-Green 배포 권장)

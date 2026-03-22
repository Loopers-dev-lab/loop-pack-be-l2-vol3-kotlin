# 결제(Payment) 요청 - Retry & CircuitBreaker 동작 가이드

## 개요

`LoopPaymentClient`는 PG(Payment Gateway) 서비스 호출 시 **Retry**와 **CircuitBreaker** 패턴을 사용하여 장애 상황을 대응합니다.

## 아키텍처

### 데코레이터 순서 (중요!)

```
requestPayment() 호출
    ↓
[CircuitBreaker] ← 먼저 체크! (바깥쪽)
    ├─ OPEN? → 즉시 Fallback (Retry 스킵) ❌
    └─ CLOSED/HALF_OPEN? → 다음 단계로
    ↓
[Retry Interceptor] ← CB 통과 후 실행 (안쪽)
    ↓ (최대 3회 재시도)
[PG Service HTTP Call]
    ↓
[성공/실패] → CircuitBreaker에 결과 기록
```

**⚠️ 핵심**: CircuitBreaker는 Retry를 포함하지 않음
- CB OPEN 상태 → Retry 실행 안 함 (빠른 실패)
- CB CLOSED 상태 → Retry 실행 (최대 3회 시도)

---

## Retry vs CircuitBreaker - 차이점

### 역할이 다릅니다!

| 항목 | Retry | CircuitBreaker |
|------|-------|-----------------|
| **역할** | 일시적 실패 극복 | 지속적 장애 방지 |
| **범위** | 1회 호출 내 | 여러 호출 패턴 |
| **대기** | 500ms 간격 | 30초 (상태 유지) |
| **반복** | 최대 3회 | 상태 판단 후 차단 |
| **응답** | 최대 1.3초 | 1ms (즉시 실패) |

### 예제: 뭔가 일시적으로 느린 상황

```
PG 서비스가 약간 느린 경우:

[요청 1] Retry 시도 1 (100ms)
         Retry 시도 2 (100ms)
         Retry 시도 3 (100ms)
         ✅ 성공 (300ms)
         → CB는 성공으로 기록

[요청 2] ✅ 성공 (100ms)
[요청 3] ✅ 성공 (100ms)

💡 Retry가 없었으면 모두 실패했을텐데,
   Retry 덕분에 모두 성공!
   CB 상태: CLOSED (변화 없음)
```

### 예제: PG 서비스가 완전히 다운된 경우

```
[요청 1] Retry 1, 2, 3 모두 실패 (1.5초 소요)
         → CB에 실패 기록

[요청 2] Retry 1, 2, 3 모두 실패 (1.5초 소요)
         → CB에 실패 기록

[요청 3] Retry 1, 2, 3 모두 실패 (1.5초 소요)
         → CB에 실패 기록

[요청 4] Retry 1, 2, 3 모두 실패 (1.5초 소요)
         → CB에 실패 기록

[요청 5] Retry 1, 2, 3 모두 실패 (1.5초 소요)
         → CB에 실패 기록 (5개 연속 실패!)
         → CB OPEN 전환! 🚨

[요청 6] CB가 OPEN → Retry 스킵 → 즉시 Fallback (1ms!)
         ❌ 빠른 실패

💡 CB 덕분에 응답 시간이 1.5초 → 1ms로 단축!
   사용자가 더 빨리 에러를 받음
```

---

## 1. 설정 비교

### 개발 환경 (application-dev.yml)

```yaml
resilience4j:
  retry:
    instances:
      loop-pg-payment:
        maxAttempts: 3              # 최대 3회 시도 (초기 1회 + 재시도 2회)
        waitDuration: 500ms         # 재시도 간 대기 시간

  circuitbreaker:
    instances:
      loop-pg-payment:
        slidingWindowSize: 10       # 최근 10개 호출 기반 판단
        minimumNumberOfCalls: 5     # 최소 5개 호출 후 판단
        failureRateThreshold: 50    # 실패율 50% 이상 시 OPEN
        permittedNumberOfCallsInHalfOpenState: 3  # HALF_OPEN에서 3개 호출 허용
        automaticTransitionFromOpenToHalfOpenEnabled: true  # 자동 복구 시도
        waitDurationInOpenState: 30s # OPEN 상태 유지 시간
```

### 테스트 환경 (application-test.yml)

```yaml
resilience4j:
  retry:
    instances:
      loop-pg-payment:
        maxAttempts: 1              # 재시도 없음 (빠른 테스트 실행)
        waitDuration: 0ms

  circuitbreaker:
    instances:
      loop-pg-payment:
        slidingWindowSize: 2        # 최근 2개 호출만 평가
        minimumNumberOfCalls: 2     # 최소 2개 호출 후 판단
        failureRateThreshold: 50    # 실패율 50% 이상 시 OPEN
        waitDurationInOpenState: 1s # 1초 후 HALF_OPEN으로 전환
        automaticTransitionFromOpenToHalfOpenEnabled: false  # 수동 제어
```

---

## 2. 동작 흐름 - 시간축

### 시나리오: 첫 호출 실패, 재시도, 성공

```
시간    동작                          상태
────────────────────────────────────────────────────────
T0      requestPayment() 호출
        ↓
T1      Retry Attempt 1 (초기)
        ↓ PG Service 500 Error
T2      Wait 500ms (재시도 대기)
        ↓
T3      Retry Attempt 2
        ↓ PG Service 500 Error
T4      Wait 500ms
        ↓
T5      Retry Attempt 3
        ↓ PG Service 200 OK
T6      ✅ 성공 반환
        CircuitBreaker: CLOSED (변화 없음)
```

**핵심**: Retry는 모두 실패해야 CircuitBreaker에 기록됨

---

## 3. 서킷브레이커 상태 전환

### CLOSED → OPEN 전환 조건

개발 환경 예제:

```
10개 호출 window 기준 (slidingWindowSize: 10)
최소 5개 호출 필요 (minimumNumberOfCalls: 5)
실패율 50% 이상 → OPEN

예시 시나리오:
─────────────────────────────────────────
호출#  결과      누적 실패율    상태
─────────────────────────────────────────
1     FAIL      -              CLOSED (호출 1개, 최소 필요 5개)
2     FAIL      -              CLOSED
3     FAIL      -              CLOSED
4     FAIL      -              CLOSED
5     FAIL      100% (5/5)     CLOSED (최소 호출 도달, 실패율 100% > 50%)
       ↓
       OPEN 전환! 🚨
─────────────────────────────────────────
```

### OPEN → HALF_OPEN 전환

- **개발**: `automaticTransitionFromOpenToHalfOpenEnabled: true`
  - 30초 대기 후 자동으로 HALF_OPEN 상태로 전환
  - 실제 서비스에서 자동 복구 시도

- **테스트**: `automaticTransitionFromOpenToHalfOpenEnabled: false`
  - 수동으로 전환해야 함 (테스트 제어용)

### HALF_OPEN → CLOSED / OPEN

```
HALF_OPEN 상태에서:
  - 3개 호출 허용 (permittedNumberOfCallsInHalfOpenState: 3)
  - 모두 성공 → CLOSED (정상 복구)
  - 1개 이상 실패 → OPEN (여전히 장애)
```

---

## 4. 실제 호출 경로별 동작

### Case 1: 정상 응답 (200 OK)

```
requestPayment()
  └─ [Retry] 초기 호출
       └─ [CB] 상태: CLOSED
            └─ HTTP 200 OK
                 └─ ✅ 즉시 반환
                 CB 상태: 변화 없음
```

### Case 2: 재시도 끝에 성공

```
requestPayment()
  └─ [Retry] Attempt 1 → 500 Error
  └─ [Wait] 500ms
  └─ [Retry] Attempt 2 → 500 Error
  └─ [Wait] 500ms
  └─ [Retry] Attempt 3 → 200 OK
       └─ ✅ 성공 반환
       CB 상태: 변화 없음 (마지막 시도가 성공)
```

### Case 3: 모든 재시도 실패 → CB OPEN

```
requestPayment()
  └─ [Retry] Attempt 1 → 500 Error
  └─ [Wait] 500ms
  └─ [Retry] Attempt 2 → 500 Error
  └─ [Wait] 500ms
  └─ [Retry] Attempt 3 → 500 Error
       └─ ❌ 최종 실패
       └─ [CB] 실패 기록
            └─ 최근 10개 중 50% 실패 → OPEN 전환 🚨
                 └─ ❌ CoreException 발생 (paymentFallback)
```

### Case 4: CB OPEN 상태에서 호출

```
requestPayment()
  └─ [CB] 상태 확인
       └─ OPEN! 🚨
            └─ HTTP 호출 스킵 (빠른 실패)
            └─ paymentFallback() 즉시 호출
                 └─ ❌ CoreException("PG payment service is unavailable...")
                 응답 시간: ~1ms (네트워크 호출 없음)
```

---

## 5. Retry 동작 상세

### Retry 메커니즘

```
@Retry(name = "loop-pg-payment")
fun requestPayment(...): PaymentRequestResult
```

**개발 환경**:
- `maxAttempts: 3` = 초기 1회 + 재시도 2회
- 각 재시도 간 500ms 대기
- 모든 예외 유형 재시도 (`java.lang.Exception`)

**재시도 예외**:
- `ConnectException` - PG 서버 연결 불가
- `TimeoutException` - 10초 이상 응답 없음
- `HttpClientErrorException` - 5xx 서버 에러
- `WebClientRequestException` - 기타 요청 에러

**재시도하지 않는 경우**:
- 4xx 클라이언트 에러 (잘못된 요청)
  - 400, 401, 403, 404 등
  - 유효한 응답이므로 재시도 의미 없음

---

## 6. CircuitBreaker 동작 상세

### 3가지 상태

| 상태 | 설명 | 동작 |
|------|-----|------|
| **CLOSED** | 정상 상태 | 모든 요청 통과 |
| **OPEN** | 장애 상태 | 모든 요청 즉시 실패 (fallback) |
| **HALF_OPEN** | 복구 시도 중 | 제한된 요청만 통과 |

### 상태 전환 조건 (개발)

```
CLOSED 상태:
  └─ 최근 10개 호출 평가
  └─ 최소 5개 호출 수집 후 판단
  └─ 실패율 ≥ 50% 또는 느린 호출 ≥ 100%
       └─ OPEN 전환

OPEN 상태:
  └─ 30초 유지
  └─ 30초 경과 후 HALF_OPEN으로 자동 전환

HALF_OPEN 상태:
  └─ 최대 3개 호출 허용
  └─ 모두 성공 → CLOSED (정상 복구)
  └─ 1개 이상 실패 → OPEN (여전히 장애)
```

---

## 7. 타임라인 예제 - 실제 장애 상황

```
시간     이벤트                              CB 상태     설명
──────────────────────────────────────────────────────────────
T0       결제 요청 1 - 500 Error            CLOSED
         → Retry 2회 → 모두 실패

T2.5     결제 요청 2 - 500 Error            CLOSED
         → Retry 2회 → 모두 실패
         (누적 실패: 2건, 평가 불가 - 최소 5건 필요)

T5       결제 요청 3 - 500 Error            CLOSED
         → Retry 2회 → 모두 실패
         (누적 실패: 3건)

T7.5     결제 요청 4 - 500 Error            CLOSED
         → Retry 2회 → 모두 실패
         (누적 실패: 4건)

T10      결제 요청 5 - 500 Error            CLOSED
         → Retry 2회 → 모두 실패
         (누적 실패: 5건/10건 = 50%)
         ⚠️ 실패율 50% ≥ 50% → OPEN 전환!

T10.5    결제 요청 6 ❌                     OPEN        ✓ 즉시 실패
         CB가 OPEN이므로 HTTP 호출 안 함
         응답 시간: ~1ms

T20      결제 요청 7 ❌                     OPEN        ✓ 즉시 실패
         여전히 OPEN 상태

T30.1    [자동 복구 시도]                   HALF_OPEN   ✓ 30초 경과
         결제 요청 8 - 200 OK ✓
         (1/3 성공)

T31      결제 요청 9 - 200 OK ✓             HALF_OPEN
         (2/3 성공)

T32      결제 요청 10 - 200 OK ✓            HALF_OPEN
         (3/3 성공)
         → CLOSED 전환! (정상 복구)

T33      결제 요청 11 - 200 OK ✓            CLOSED      ✓ 정상 서비스 재개
```

---

## 8. 성능 영향

### 네트워크 레이턴시 비교

| 시나리오 | 첫 호출 | 재시도 | 총 시간 |
|---------|--------|--------|---------|
| 즉시 성공 | ~100ms | - | ~100ms |
| 1회 실패 후 성공 | ~100ms | 500ms + ~100ms | ~700ms |
| 2회 실패 후 성공 | ~100ms | 500ms + ~100ms + 500ms + ~100ms | ~1300ms |
| 3회 모두 실패 | ~100ms × 3 | 500ms × 2 | ~1200ms |
| CB OPEN + Fallback | - | - | ~1ms |

**CB가 OPEN되면 응답 속도는 1000배 이상 빨라짐!**

---

## 9. 모니터링 & 로깅

### CircuitBreaker 상태 확인

```kotlin
val cb = circuitBreakerRegistry.circuitBreaker("loop-pg-payment")
println("Status: ${cb.state}")  // CLOSED, OPEN, HALF_OPEN
println("Metrics: ${cb.metrics}")
```

### 로그 레벨

- **개발**: `com.loopers: DEBUG` - 모든 재시도와 CB 상태 변환 로깅
- **프로덕션**: `com.loopers: INFO` - 주요 이벤트만 로깅

### 로그 예제

```
[Retry] requestPayment - Retry attempt 1/3
[Retry] requestPayment - Retry attempt 2/3
[CircuitBreaker] loop-pg-payment transitioned to OPEN
[Fallback] paymentFallback called - service unavailable
```

---

## 10. 운영 가이드

### 개발 환경에서 테스트

```bash
# 1. PG 서비스 정상 상태에서 실패 유도
# → localhost:8083 서비스 종료

# 2. 결제 요청 5회 이상 시도
# → 실패율 50% 도달 → CB OPEN

# 3. PG 서비스 재시작
# → 30초 대기 → 자동 복구 시도 (HALF_OPEN)
# → 성공 시 CB CLOSED로 복구
```

### 알림 설정

```
CircuitBreaker가 OPEN되었을 때:
- ✉️ 운영팀 알림 발송
- 🚨 에러 로그 급증 감지
- 📊 대시보드: CB 상태 실시간 표시
```

### 수동 복구

```kotlin
// 긴급 상황에서 CB 강제 CLOSED
val cb = circuitBreakerRegistry.circuitBreaker("loop-pg-payment")
cb.transitionToClosedState()
```

---

## 11. 트러블슈팅

### Q1: 결제가 자주 실패하는데 어떻게 하나?

**A**: 다음을 순서대로 확인:
1. PG 서비스 상태 확인 (localhost:8083 정상 여부)
2. CircuitBreaker 상태 확인 (`CB.state`)
3. Retry 로그 확인 (재시도 몇 회까지 진행)
4. 네트워크 지연 확인 (응답 시간 > 5초?)

### Q2: CB가 OPEN되면 얼마나 빨리 복구?

**A**: 개발은 30초, 테스트는 수동 복구
```
개발: OPEN → 30초 대기 → HALF_OPEN → 3개 요청 성공 → CLOSED
테스트: OPEN → 수동 전환 필요
```

### Q3: 클라이언트는 재시도를 알 수 있나?

**A**: 투명함
```
클라이언트 입장에서는 1회 호출처럼 보임
- 첫 호출 성공 → 100ms 응답
- 재시도 필요 → 최대 1.3초 응답
- CB OPEN → 1ms 응답
```

### Q4: Retry와 CB는 중복되나?

**A**: 아니오, 순차적 적용
```
Retry는 호출 레벨 (1회 호출 = 최대 3회 시도)
CB는 패턴 레벨 (연속 실패 시 전체 차단)
```

---

## 12. 설정 조정 권장사항

### 높은 가용성 (가능한 재시도)

```yaml
retry:
  maxAttempts: 5          # 더 많은 재시도
  waitDuration: 1000ms    # 더 긴 대기

circuitbreaker:
  slidingWindowSize: 50   # 더 많은 샘플
  minimumNumberOfCalls: 10
  failureRateThreshold: 60  # 더 높은 임계값
```

### 빠른 응답 (장애 격리)

```yaml
retry:
  maxAttempts: 2          # 적은 재시도
  waitDuration: 100ms     # 짧은 대기

circuitbreaker:
  slidingWindowSize: 5    # 적은 샘플
  minimumNumberOfCalls: 3
  failureRateThreshold: 40  # 낮은 임계값
```

---

## 참고자료

- **Resilience4j 공식 문서**: https://resilience4j.readme.io/
- **LoopPaymentClient 구현**: `LoopPaymentClient.kt`
- **테스트 코드**: `LoopPaymentClientCircuitBreakerTest.kt`, `LoopPaymentClientE2ETest.kt`

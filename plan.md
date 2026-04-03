# Graceful Degradation (Redis 장애 시 Fallback)

## 개요
Redis 장애 시 대기열 시스템이 완전 실패하는 대신, Resilience4j Circuit Breaker + Retry를 적용하여
일시 장애는 재시도로 복구하고, 지속 장애 시 대기열을 bypass하여 주문을 허용한다.

## 구현 계획

### 1. OrderQueueRedisRepository fallback 메서드
- [x] enqueueFallback — Redis 장애 시 true를 반환하고 WARN 로그를 남긴다
- [x] getPositionFallback — Redis 장애 시 null을 반환한다
- [x] getTotalSizeFallback — Redis 장애 시 0을 반환한다
- [x] popFrontFallback — Redis 장애 시 빈 목록을 반환한다

### 2. EntryTokenRedisRepository fallback 메서드
- [x] getFallback — Redis 장애 시 BYPASS_TOKEN을 반환하고 WARN 로그를 남긴다
- [x] issueFallback / consumeFallback — Redis 장애 시 예외 없이 무시한다

### 3. 토큰 검증 bypass
- [x] validateAndConsumeToken — BYPASS_TOKEN이면 검증을 스킵하고 WARN 로그를 남긴다
- [x] validateAndConsumeToken — 정상 토큰이면 기존과 동일하게 검증한다 (회귀 확인)

### 4. Resilience4j 설정
- [x] application.yml — order-queue CircuitBreaker + Retry 설정 추가
- [x] @CircuitBreaker, @Retry 어노테이션을 Repository 메서드에 적용한다

### 5. Bypass 보완: 동시 주문 제한 (Bulkhead)
- [x] OrderFacade.placeOrder에 @Bulkhead 적용 — 동시 호출 수 초과 시 fallback 실행
- [x] placeOrderFallback — Bulkhead 초과 시 CoreException(SERVICE_UNAVAILABLE) throw
- [x] ErrorType에 SERVICE_UNAVAILABLE(503) 추가
- [x] application.yml에 order-place bulkhead 설정 추가 (maxConcurrentCalls, maxWaitDuration)

### 6. Bypass 보완: SSE bypass 알림
- [x] QueueHealthChecker 인터페이스를 도메인 레이어에 정의한다 (DIP)
- [x] CircuitBreakerQueueHealthChecker를 인프라 레이어에 구현한다 (CircuitBreakerRegistry 활용)
- [x] QueueFacade.broadcastBypass — 모든 SSE 구독자에게 bypass 이벤트를 전송하고 complete한다
- [x] QueueAdmissionScheduler에서 bypass 감지 시 broadcastBypass 호출

### 7. Bypass 상태 모니터링
- [x] Actuator에 CircuitBreaker health endpoint 노출 설정
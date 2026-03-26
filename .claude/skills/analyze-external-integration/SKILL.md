---
name: analyze-external-integration
description:
  외부 시스템(PG, 메시징, 서드파티 API 등) 연동 코드를 분석한다.

  특히 다음을 중점적으로 점검한다.
  - 외부 호출이 트랜잭션 경계 안에 포함되어 있지 않은지
  - 타임아웃, 재시도, 서킷브레이커 등 Resilience 패턴이 적용되어 있는지
  - 외부 시스템 장애 시 fallback 전략이 수립되어 있는지
  - 비동기 콜백 처리 시 멱등성이 보장되는지

  단순한 정답 제시가 아니라, 장애 시나리오별 동작을 추적하고 개선 가능 지점을 선택적으로 제안한다.
---

### Analysis Scope
이 스킬은 아래 대상에 대해 분석한다.
- 외부 API 호출 코드 (RestTemplate, WebClient, Feign 등)
- Resilience4j 설정 및 어노테이션 (@CircuitBreaker, @Retry, @TimeLimiter)
- 콜백/웹훅 수신 엔드포인트
- 스케줄러 기반 상태 동기화 코드
> UseCase → 외부 호출 → 콜백 처리 전체 흐름을 기준으로 분석한다.

### Analysis Checklist

#### 1. Transaction Boundary vs External Call
다음을 순서대로 확인한다.
- 외부 API 호출이 @Transactional 범위 안에 있는지
- DB 커밋 전에 외부 호출이 실패하면 어떤 상태가 되는지
- 외부 호출 성공 후 DB 커밋이 실패하면 어떤 상태가 되는지

**출력 예시**
```markdown
- 현재 흐름:
UseCase.execute()
  ├─ [TX-1] paymentService.createPayment() → PENDING 저장 + 커밋
  ├─ [TX 외부] pgClient.requestPayment() → PG 호출
  └─ [TX-2] paymentService.updateTransactionKey() → transactionKey 저장

- 판정: 외부 호출이 트랜잭션 외부에 위치하여 적절함
```

#### 2. Resilience Pattern 점검
아래 패턴이 적용되어 있는지 확인한다.
- Timeout: 연결/읽기 타임아웃 설정
- CircuitBreaker: 연속 실패 시 빠른 실패 (fail-fast)
- Fallback: 장애 시 대체 응답 전략
- Retry: 일시적 실패 시 재시도 (멱등한 요청에만 적용)

**체크리스트**
```markdown
- [ ] RestTemplate/WebClient에 타임아웃 설정이 있는가?
- [ ] CircuitBreaker가 적절한 임계값으로 설정되어 있는가?
- [ ] Fallback 메서드가 적절한 에러를 반환하는가?
- [ ] CircuitBreaker OPEN 상태에서 스케줄러가 PG에 부하를 주지 않는가?
```

#### 3. Idempotency (멱등성) 분석
콜백/웹훅 처리에서 다음을 확인한다.
- 동일 콜백이 2회 이상 수신될 경우 동작
- 이미 처리된 상태(SUCCESS/FAILED)에서 재처리 시도 시 동작
- transactionKey 기반 조회의 유일성 보장

#### 4. Failure Scenario 추적
주요 장애 시나리오를 나열하고, 각각의 결과 상태를 추적한다.
- PG 요청 타임아웃 → Payment 상태?
- PG 요청 성공 → 콜백 미수신 → Payment 상태?
- 콜백 수신 → DB 저장 실패 → 재수신 시 동작?
- CircuitBreaker OPEN → 결제 요청 시 동작?

#### 5. Improvement Proposal (선택적 제안)
- 트랜잭션 경계 조정
- Resilience 파라미터 튜닝
- 보상 트랜잭션 / Saga 패턴 도입
- Dead Letter Queue 활용
- 모니터링/알림 추가

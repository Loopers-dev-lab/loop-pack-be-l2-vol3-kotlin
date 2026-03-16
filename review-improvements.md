# 결제 시스템 코드 리뷰 개선점

> 4개 에이전트 리뷰 결과 통합 (architect, qa-manager, simplifier, design-critic)
> 이미 확정된 설계 결정(afterCommit 패턴, 멱등 키, cardNo 마스킹, BC 분리 등)에 대한 재논의는 제외

---

## CRITICAL — 머지 전 수정 필요

### 1. SUCCESS 상태 중복 결제 허용 + Race Condition

**파일**: `RequestPaymentUseCase.kt:26-32`

현재 `findByOrderId`가 `REQUESTED`/`TIMEOUT`만 CONFLICT로 거부한다. SUCCESS 상태의 기존 결제가 있어도 새 결제가 통과한다.

또한 SELECT→INSERT 사이에 두 요청이 동시 진입하면 둘 다 `null`을 받고 중복 Payment가 생성된다. `READ_COMMITTED`에서 커밋 전 INSERT는 보이지 않으므로 Application 레벨 체크만으로는 방어 불가.

**수정 방안**:
- (A) `SUCCESS` 상태도 CONFLICT 처리, `FAILED`는 재결제 허용 여부 비즈니스 결정
- (B) DB에 `(order_id)` UNIQUE 제약 추가 → `DataIntegrityViolationException` catch로 CONFLICT 반환
- 둘 다 적용 권장 (Application 가드 + DB 제약)

### 2. `?: return` 무음 실패 패턴 (7곳)

**파일**: `PaymentPgProcessorImpl:30,40,45`, `PaymentRecoveryProcessorImpl:29`

Payment/Order 조회 실패 시 `?: return`으로 조용히 종료. PG 호출이 누락되거나 Payment↔Order 상태 불일치가 발생해도 로그 없이 사라진다. 결제는 돈이 걸린 흐름이므로 실패를 삼키면 안 된다.

**수정 방안**: `?: return` → 예외 발생 또는 최소한 `log.error` + return. 특히 `findById(paymentId)`는 방금 저장한 데이터이므로 null이면 데이터 정합성 문제 → 예외가 적절.

### 3. PG 비즈니스 거절(4xx) vs 인프라 장애(5xx) 미구분

**파일**: `PgClientImpl.kt:45-56`

fallback에서 모든 실패를 TIMEOUT으로 분류한다. PG가 400(잘못된 카드)/402(한도 초과) 같은 확정 거절을 반환해도 TIMEOUT이 되어, RecoverPaymentUseCase가 이미 거절된 결제를 반복 조회한다.

**수정 방안**: fallback의 `Throwable t` 타입을 분석하여 FeignException의 HTTP 상태코드 확인. 4xx → FAILED, 5xx/타임아웃 → TIMEOUT.

### 4. Payment FAILED인데 Order PENDING_PAYMENT 불일치

**파일**: `PaymentPgProcessorImpl:44-51`

FAILED 분기에서 `payment.markFailed` + `save` 이후 `orderRepository.findById`가 null이면 `return`. Payment는 FAILED인데 Order는 PENDING_PAYMENT 상태로 남는 비즈니스 불변식 위반.

**수정 방안**: Order 조회를 Payment 상태 변경 **이전**에 수행 (RecoveryProcessorImpl:32에서는 이미 이 순서). Order 없으면 예외 → 트랜잭션 롤백.

### 5. recoverAll의 readOnly 트랜잭션 전파 문제

**파일**: `RecoverPaymentUseCase.kt:19`, `PaymentRecoveryProcessorImpl`

`recoverAll`에 `@Transactional(readOnly = true)`, `recoverSingle`에 `@Transactional`(REQUIRED). REQUIRED 전파는 기존 readOnly 트랜잭션에 **참여**하므로 쓰기가 flush되지 않거나 예외 발생 가능.

**수정 방안**:
- (A) `recoverSingle`을 `REQUIRES_NEW`로 변경
- (B) `recoverAll`에서 `@Transactional(readOnly = true)` 제거 (목록 조회에 트랜잭션 불필요)

---

## WARNING — 수정 권장

### 6. PgPaymentRequest.callbackUrl이 Domain 계층에 존재

**파일**: `domain/payment/PgPaymentRequest.kt:10`

`callbackUrl`은 인프라 배포 설정값이지 도메인 개념이 아니다.

**수정 방안**: `PgPaymentRequest`에서 `callbackUrl` 제거, `PgClientImpl`이 내부에서 설정값을 주입하여 합성.

### 7. RecoverPaymentUseCase의 execute() 단일 메서드 규칙 위반

**파일**: `RecoverPaymentUseCase.kt:20,34`

Application CLAUDE.md 규칙상 UseCase는 `execute()` 단일 메서드여야 하는데, `recoverAll()`과 `recoverByOrderId()` 두 개의 public 메서드가 존재.

**수정 방안**: `RecoverAllPaymentsUseCase`와 `RecoverPaymentByOrderIdUseCase`로 분리.

### 8. processPayment에서 amount 불필요 재조회 + command 전체 전달

**파일**: `PaymentPgProcessor.kt:14,30`

UseCase에서 이미 알고 있는 amount를 afterCommit 이후 DB에서 다시 조회. command 전체를 전달하지만 userId는 미사용.

**수정 방안**: 시그니처를 `processPayment(paymentId, orderId, amount, pgRequest)` 등 필요값만 전달하도록 변경. DB 왕복 1회 제거.

### 9. findByStatusIn 정렬 기준 누락

**파일**: `PaymentRepositoryImpl.kt:43`

`PageRequest.of(0, limit)`에 Sort 없음. Infrastructure CLAUDE.md의 "페이지네이션 쿼리에는 안정적 정렬 기준 명시" 규칙 위반.

**수정 방안**: `PageRequest.of(0, limit, Sort.by(Sort.Direction.ASC, "id"))` 추가.

### 10. cardType 미검증 → 500 에러

**파일**: `RequestPaymentUseCase.kt:49`

잘못된 `cardType` 문자열이 `CardType.valueOf()`에서 `IllegalArgumentException` → 500 에러로 전환.

**수정 방안**: UseCase에서 `try-catch`로 감싸 `BAD_REQUEST` 반환, 또는 ApiSpec에서 Enum 바인딩 검증.

### 11. recoverAll_oneFailure 테스트 false positive

**파일**: `RecoverPaymentUseCaseTest.kt:117-143`

"한 건 실패 시 나머지 계속 처리"를 검증한다고 하지만, `pgClient.transactionDetailException` 설정 시 **모든** 건이 실패하여 `count == 0`을 검증한다. 테스트명이 약속하는 시나리오를 증명하지 못함.

**수정 방안**: orderId별 다른 응답을 설정할 수 있도록 FakePgClient 수정, 첫 번째만 실패/두 번째는 성공 시나리오 작성.

### 12. Order 동시성 보호 부재

**파일**: `PaymentPgProcessorImpl:48`, `PaymentRecoveryProcessorImpl:32`

Payment는 `findByOrderIdForUpdate`로 비관적 락을 걸지만, Order는 `findById`로 락 없이 조회. 동시에 두 콜백이 같은 주문을 처리하면 Order의 lost update 가능.

**수정 방안**: `OrderRepository`에 `findByIdForUpdate` 추가.

### 13. Callback/Admin Controller ApiSpec 누락

**파일**: `PaymentCallbackController.kt`, `PaymentAdminController.kt`

Interfaces CLAUDE.md 규칙 "OpenAPI 명세는 ApiSpec 인터페이스로 분리"를 미준수. `PaymentAdminController:21`의 `@Positive`도 구현체에 직접 선언.

**수정 방안**: `PaymentCallbackApiSpec`, `PaymentAdminApiSpec` 인터페이스 생성.

### 14. updateStatusConditionally 데드코드

**파일**: `PaymentRepository.kt:12`, `PaymentRepositoryImpl.kt`

프로덕션 코드에서 호출처 없음. 테스트 셋업에서만 사용되며, 도메인 불변식을 우회하는 메서드.

**수정 방안**: 제거. 테스트는 `Payment.markSuccess()` → `save()`로 상태 전이.

### 15. PaymentRecoveryProcessor 인터페이스 불필요

**파일**: `PaymentRecoveryProcessor.kt`

구현체 1개, Fake 없음. `RecoverPaymentUseCaseTest`에서도 Impl을 직접 사용.

**수정 방안**: 인터페이스 제거, `PaymentRecoveryProcessorImpl` → `PaymentRecoveryProcessor`로 리네임.

### 16. afterCommit 콜백 예외 미전파/미로깅

**파일**: `RequestPaymentUseCase.kt` (afterCommit 호출부)

afterCommit 콜백은 예외를 호출자에게 전파하지 않는다. processPayment 내부에서 예외 발생 시 어디에서도 감지 불가.

**수정 방안**: afterCommit 내부에 `try-catch` + `log.error` 추가. RecoverPaymentUseCase가 복구하더라도 최소한 실패 로그는 필수.

---

## INFO — 참고/후순위

### 17. maskCardNo를 private으로 변경

`Payment.kt:52-56` — `create()` 내부에서만 사용. 외부 노출 불필요 시 `private` 권장.

### 18. ZonedDateTime.now() 직접 호출

`Payment.kt:64` — 테스트에서 시간 제어 어려움. 현재 문제 없으나 시간 의존 테스트 필요 시 `Clock` 주입 고려.

### 19. PG 트랜잭션 null 시 무한 재시도 (TTL 없음)

`PaymentRecoveryProcessorImpl:29` — PG에 트랜잭션 없으면 `return false`. TTL 없이 매 복구 주기마다 조회. `createdAt` 기준 만료 정책 필요 가능.

### 20. totalPrice.toLong() 소수점 절삭 위험

`RequestPaymentUseCase.kt:48-49` — 현재 정수만 사용하지만, 할인율/세금 추가 시 무음 절삭 위험. `setScale(0, UNNECESSARY)` 방어 고려.

### 21. FakePaymentRepository updatedAt 미갱신

`FakePaymentRepository.kt:14` — 시간 기반 테스트 추가 시 JPA `@PreUpdate`와 불일치 가능.

### 22. maskCardNo 엣지 케이스 테스트 부재

`PaymentTest.kt` — "-" 구분자 없는 카드번호, 빈 문자열 등의 케이스 미검증.

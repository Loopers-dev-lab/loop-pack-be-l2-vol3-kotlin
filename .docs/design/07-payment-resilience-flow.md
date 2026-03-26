# Volume 6 결제/복구 흐름 정리

## 목적

Volume 6에서 구현한 결제 흐름을 한 번에 읽히도록 정리한다.  
특히 아래 4가지를 중심으로 본다.

- 주문 생성 시 재고/쿠폰을 언제 확보하는지
- PG 요청 이후 상태가 어떻게 전이되는지
- 실패 시 어떤 보상 작업이 일어나는지
- `sync` / `callback` / `retry`가 각각 어떤 역할인지

---

## 전체 흐름 한 줄 요약

주문 생성 시점에 **재고를 먼저 차감**하고 **쿠폰은 `RESERVED`로 예약**한 뒤,  
결제 요청 결과가 확정되면

- 성공: 주문 `PAID`, 쿠폰 `USED`
- 실패: 주문 `PAYMENT_FAILED`, 재고 복구, 쿠폰 해제
- 미확정: 주문 `PAYMENT_PENDING`, 결제 `UNKNOWN/PENDING` 유지 후 `sync` 또는 `callback`으로 최종 확정

하는 구조다.

---

## 주요 파일

### 주문 생성/취소
- `apps/commerce-api/src/main/kotlin/com/loopers/application/order/OrderUseCase.kt`
- `apps/commerce-api/src/main/kotlin/com/loopers/domain/order/OrderCanceller.kt`
- `apps/commerce-api/src/main/kotlin/com/loopers/domain/order/OrderPaymentProcessor.kt`

### 재고
- `apps/commerce-api/src/main/kotlin/com/loopers/domain/product/ProductStockDeductor.kt`
- `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/product/ProductJpaRepository.kt`

### 쿠폰
- `apps/commerce-api/src/main/kotlin/com/loopers/domain/coupon/IssuedCouponProcessor.kt`
- `apps/commerce-api/src/main/kotlin/com/loopers/domain/coupon/IssuedCoupon.kt`
- `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/coupon/IssuedCouponJpaRepository.kt`

### 결제
- `apps/commerce-api/src/main/kotlin/com/loopers/application/payment/PaymentUseCase.kt`
- `apps/commerce-api/src/main/kotlin/com/loopers/domain/payment/Payment.kt`
- `apps/commerce-api/src/main/kotlin/com/loopers/domain/payment/PaymentProcessor.kt`
- `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/payment/PgSimulatorClient.kt`
- `apps/commerce-api/src/main/resources/application.yml`

---

## 1. 주문 생성 단계

주문 생성은 `OrderUseCase.createOrder()`에서 시작한다.

### 1-1. 재고 차감
주문 상품마다 `ProductStockDeductor.deductStock()`를 호출한다.

실제 DB 반영은 아래 update 쿼리로 이뤄진다.

- `stock = stock - quantity`
- 단, `stock >= quantity`일 때만 성공

즉 주문 생성 시점에 이미 재고를 선점한다.

### 1-2. 쿠폰 예약
쿠폰이 있으면 `IssuedCouponProcessor.reserve()`를 호출한다.

이 과정에서:

- 발급 쿠폰 row를 `PESSIMISTIC_WRITE`로 조회
- 주문 금액에 적용 가능한지 검증
- 상태를 `AVAILABLE -> RESERVED`로 변경

즉 쿠폰은 이 시점에 바로 `USED`가 아니라, **예약 상태**로만 바뀐다.

### 1-3. 주문 저장
재고 차감과 쿠폰 예약이 끝나면 할인 금액을 반영한 주문을 저장한다.

이 시점의 주문 상태는 기본적으로 `ORDERED`.

---

## 2. 결제 요청 단계

결제 요청은 `PaymentUseCase.requestPayment()`에서 처리한다.

이 흐름은 크게 3단계다.

### 2-1. 결제 준비 트랜잭션
먼저 `TransactionTemplate` 안에서:

- 주문을 `PAYMENT_PENDING`으로 전이 (`beginPayment()`)
- `Payment` 엔티티를 `REQUESTED` 상태로 생성

한다.

여기까지는 DB 작업만 수행하고 커밋한다.

### 2-2. PG 요청
그 다음 트랜잭션 밖에서 `PgSimulatorClient.requestPayment()`를 호출한다.

현재 설정:

- `connect-timeout: 200ms`
- `read-timeout: 700ms`
- `CircuitBreaker` 적용

### 2-3. 결제 요청 결과 반영 트랜잭션
PG 응답을 받은 뒤 다시 `TransactionTemplate` 안에서:

- `Payment` 상태 반영
- `Order` 상태 반영
- 쿠폰 상태 동기화

를 수행한다.

외부 HTTP 호출을 DB 트랜잭션 밖으로 분리한 이유는,  
PG 지연 때문에 DB 커넥션과 락을 오래 잡지 않기 위해서다.

---

## 3. PG 응답별 처리

### 3-1. `Accepted`
PG가 요청을 정상적으로 받아서 `transactionKey`를 내려준 경우다.

이때 상태는:

- `Payment = PENDING`
- `Order = PAYMENT_PENDING`
- 쿠폰 = `RESERVED` 유지
- 재고 = 차감 상태 유지

즉 아직 결제가 끝난 게 아니라 **최종 확정 대기 상태**다.

### 3-2. `RequestFailed`
요청 자체가 명확히 실패한 경우다.

이때 상태는:

- `Payment = REQUEST_FAILED`
- `Order = PAYMENT_FAILED`
- 재고 restore
- 쿠폰 `RESERVED -> AVAILABLE`

즉 주문 시점에 선점했던 자원을 바로 원복한다.

### 3-3. `Unknown`
timeout / 네트워크 오류처럼 결과가 애매한 경우다.

이때 상태는:

- `Payment = UNKNOWN`
- `Order = PAYMENT_PENDING`
- 쿠폰 = `RESERVED` 유지
- 재고 = 차감 상태 유지

즉 실패로 단정하지 않고 **미확정 상태**로 남긴다.

---

## 4. 최종 확정 단계

`PENDING` 또는 `UNKNOWN`으로 남은 결제는 나중에 최종 확정해야 한다.

### 4-1. callback
PG가 우리 서버의 callback endpoint를 호출한다.

- endpoint: `POST /api/v1/payments/callback`

이때 `transactionKey`로 기존 결제를 찾아 최종 상태를 반영한다.

### 4-2. sync
우리 서비스가 PG에 다시 조회한다.

- `transactionKey`가 있으면: `GET /payments/{transactionKey}`
- 없으면: `GET /payments?orderId=...`

즉 callback은 **PG가 밀어주는 방식**,  
sync는 **우리가 다시 조회하는 방식**이다.

둘 다 **최종 확정 경로**다.

---

## 5. retry / fallback / callback 차이

이 셋이 헷갈리기 쉬워서 따로 정리한다.

### callback
- PG가 우리 서버로 나중에 결과를 보내는 외부 HTTP 호출
- 결제 최종 결과를 확정하는 경로

### fallback
- Resilience4j가 내부에서 대신 실행하는 로컬 메서드
- 외부 callback과 전혀 다름
- “지금 정상 호출이 안 되니 어떤 결과를 반환할지”를 정하는 장치

### retry
- 이번 구현에서는 **조회(sync) 경로에만 1회 적용**
- 대상:
  - `getTransaction()`
  - `findLatestTransactionByOrderId()`
- `LookupResult.Unavailable`일 때만 재시도

결제 요청 자체에는 retry를 걸지 않았다.  
이유는 요청이 실제로 PG에 전달됐는지 애매한 상태에서 자동 재시도를 넣으면 **중복 결제 위험**이 생길 수 있기 때문이다.

---

## 6. 상태 전이 정리

### 주문 상태
- `ORDERED`
- `PAYMENT_PENDING`
- `PAYMENT_FAILED`
- `PAID`
- `CANCELLED`

### 결제 상태
- `REQUESTED`
- `PENDING`
- `REQUEST_FAILED`
- `UNKNOWN`
- `SUCCESS`
- `FAILED`

### 쿠폰 상태
- `AVAILABLE`
- `RESERVED`
- `USED`
- `EXPIRED`

---

## 7. 현재 설계의 장점

- 외부 PG 호출을 트랜잭션 밖으로 분리해서 DB 락 점유를 줄임
- 주문 생성 시 재고/쿠폰을 먼저 확보해 자원 경쟁을 줄임
- timeout을 곧바로 실패로 단정하지 않고 `UNKNOWN`으로 둬서 섣부른 보상을 피함
- `sync` / `callback`으로 비동기 확정 가능
- retry는 조회 경로에만 제한적으로 적용해서 중복 결제 위험을 낮춤

---

## 8. 아직 열려 있는 고민

### 8-1. `UNKNOWN` 자원 홀딩
`UNKNOWN`이 길어지면 재고와 쿠폰이 계속 묶일 수 있다.  
추후 reconciliation이나 운영 알림이 필요할 수 있다.

### 8-2. 결제 실패 후 재결제 정책
현재는 실패 시 재고와 쿠폰은 복구되지만, 주문 자체는 다시 결제 흐름을 탈 수 있게 열려 있다.  
실패 원인별로 재결제 허용 범위를 나눌지 고민이 남아 있다.

### 8-3. 카드 정보 보안
응답에서는 마스킹했지만 DB 저장은 아직 평문이다.  
과제 범위를 넘어가면 토큰화 또는 암호화 설계가 필요하다.

---

## 9. 한 줄 결론

이 구현은 **주문 시점 선점 → 결제 요청 → 성공 시 확정 / 실패 시 보상 / 애매하면 보류 후 후속 확정** 구조로 설계되어 있다.  
즉, 단순한 “결제 API 호출”이 아니라 **자원 정합성을 우선한 상태 전이 중심 결제 흐름**이라고 볼 수 있다.

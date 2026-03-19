# 주문 처리 및 결제 흐름

## 1. 전체 아키텍처 흐름

```mermaid
graph TD
    A["사용자<br/>(클라이언트)"] -->|주문 요청| B["OrderController"]
    B -->|주문 생성| C["OrderFacade"]
    C -->|재고 관리| D["StockService"]
    C -->|할인 계산| E["CouponService"]
    C -->|주문 저장| F["OrderService"]
    F -->|주문 영속| G["OrderRepository"]

    A -->|결제 요청| H["PaymentController"]
    H -->|결제 흐름| I["PaymentFacade"]
    I -->|주문 검증| F
    I -->|결제 요청| J["LoopPaymentClient"]
    J -->|Circuit Breaker<br/>Retry| K["PG 서버"]

    K -->|콜백| L["PaymentCallbackController"]
    L -->|결제 완료 처리| I
    I -->|상태 업데이트| M["ReceiptService"]
    M -->|결제 저장| N["ReceiptRepository"]
    M -->|주문 상태 변경| F

    O["타임아웃 배치<br/>ReceiptTimeoutHandler"] -->|30분 초과| M
```

---

## 2. 주문 생성 흐름 (Order Creation Flow)

```mermaid
sequenceDiagram
    participant Client as 클라이언트
    participant OC as OrderController
    participant OF as OrderFacade
    participant SS as StockService
    participant CS as CouponService
    participant OS as OrderService
    participant OR as OrderRepository

    Client->>OC: POST /api/v1/orders<br/>{items, couponId}
    OC->>OF: createOrder(userId, request)

    OF->>SS: decreaseAllStocks(items)<br/>(재고 감소)
    SS-->>OF: ✓ 재고 감소 완료<br/>(원자적)

    OF->>CS: calculateDiscount(couponId)<br/>(할인금 계산)
    CS-->>OF: discountAmount

    OF->>CS: useCoupon(userId, couponId)<br/>(쿠폰 사용)
    CS-->>OF: ✓ 쿠폰 사용 완료

    OF->>OS: createOrder(userId, items,<br/>couponId, totalPrice)
    OS->>OR: save(order)
    OR-->>OS: orderId
    OS-->>OF: order (PENDING)

    OF->>OF: distributeDiscount(order,<br/>discountAmount)<br/>(각 OrderItem에<br/>할인금 배분)

    OF-->>OC: OrderInfo {orderId, status}
    OC-->>Client: 201 Created<br/>OrderInfo
```

---

## 3. 결제 요청 흐름 (Payment Request Flow)

```mermaid
sequenceDiagram
    participant Client as 클라이언트
    participant PC as PaymentController
    participant PF as PaymentFacade
    participant OS as OrderService
    participant RS as ReceiptService
    participant LPC as LoopPaymentClient
    participant PG as "PG 서버<br/>(외부)"

    Client->>PC: POST /api/v1/payments<br/>{orderId, cardType, cardNo}
    PC->>PF: requestPayment(userId, orderId,<br/>cardType, cardNo)

    PF->>OS: getOrderByIdForUpdate(userId, orderId)<br/>(PENDING 상태 확인)
    OS-->>PF: order

    PF->>RS: getReceiptByOrderId(orderId)<br/>(기존 결제 확인)
    alt Receipt 존재
        PF->>PF: 상태 검증<br/>INITIATED/PENDING→에러<br/>COMPLETED→에러<br/>CANCELLED→에러<br/>TIMEOUT/FAILED→재시도
    else 없음
        RS-->>PF: null (정상)
    end

    PF->>PF: transactionId = generateTransactionId(orderId)<br/>TXN_{timestamp}_{orderId}

    PF->>LPC: requestPayment(userId, transactionId,<br/>orderId, amount, cardType, cardNo)

    rect rgb(200, 150, 255)
        note right of LPC: Resilience4j 적용<br/>@CircuitBreaker<br/>@Retry
        LPC->>PG: POST /api/v1/payments<br/>PgPaymentRequest {orderId, amount,<br/>cardType, cardNo, callbackUrl}
        alt PG 성공
            PG-->>LPC: PgPaymentResponse {transactionKey,<br/>status, amount, ...}
            LPC-->>PF: PaymentRequestResult
        else PG 실패
            PG-->>LPC: Exception
            LPC->>LPC: Retry 적용<br/>(최대 3회)
            alt Retry 완료<br/>여전히 실패
                LPC->>LPC: Circuit Breaker<br/>OPEN 상태
                LPC-->>PF: RuntimeException<br/>(fallback)
            end
        end
    end

    PF->>RS: initiateReceipt(orderId, transactionId,<br/>amount, cardType, cardNo)<br/>(Receipt 생성)
    RS->>RS: receipt = Receipt.create(...)<br/>status = INITIATED
    RS->>RS: save(receipt)<br/>(DB 저장)
    RS-->>PF: receipt

    PF->>RS: markAsPending(receiptId)<br/>(상태: INITIATED → PENDING)
    RS->>RS: receipt.markAsPending()
    RS->>RS: save(receipt)
    RS-->>PF: ✓

    PF-->>PC: ReceiptInfo {paymentId, orderId,<br/>amount, status: PENDING, ...}
    PC-->>Client: 200 OK<br/>ReceiptInfo

    Note over Client,PG: 콜백 대기 (최대 30분)
```

---

## 4. 결제 콜백 처리 흐름 (Payment Callback Flow)

```mermaid
sequenceDiagram
    participant PG as "PG 서버<br/>(외부)"
    participant PCC as PaymentCallbackController
    participant PF as PaymentFacade
    participant RS as ReceiptService
    participant OS as OrderService

    PG->>PCC: POST /api/v1/payments/callback<br/>{transactionId, orderId, status,<br/>amount, ...}

    PCC->>PCC: PaymentCallbackCommand 생성
    PCC->>PF: completePayment(command)

    PF->>RS: updateReceiptStatus(command)

    RS->>RS: receipt = getReceiptByTransactionIdForUpdate(transactionId)<br/>(FOR UPDATE 락)

    RS->>RS: 멱등성 체크:<br/>status ∈ {INITIATED, PENDING} ?<br/>예: 진행, 아니오: 무시

    alt command.status = "COMPLETED"
        RS->>RS: 금액 검증<br/>command.amount == receipt.amount ?
        alt 일치
            RS->>RS: receipt.markAsCompleted(amount)
            RS->>RS: receipt.status = COMPLETED
        else 불일치
            RS-->>PF: CoreException<br/>(BAD_REQUEST)
        end
    else command.status = "FAILED"
        RS->>RS: receipt.markAsFailed()
        RS->>RS: receipt.status = FAILED
    else command.status = "CANCELLED"
        RS->>RS: receipt.markAsCancelled()
        RS->>RS: receipt.status = CANCELLED
    else 기타
        RS-->>PF: CoreException<br/>(BAD_REQUEST)
    end

    RS->>RS: save(receipt)<br/>(DB 저장)
    RS-->>PF: ✓

    alt receipt.status = COMPLETED
        PF->>OS: markOrderAsPaid(orderId)<br/>(주문 상태: PENDING → PAID)
        OS->>OS: order.changeStatus(OrderStatus.PAID)<br/>(더티 체킹으로<br/>자동 저장)
        OS-->>PF: ✓
        PF-->>PCC: ✓ 결제 완료
        PCC-->>PG: 200 OK
    else receipt.status ≠ COMPLETED
        PF-->>PCC: ✓ 상태 업데이트됨
        PCC-->>PG: 200 OK
    end
```

---

## 5. Receipt 상태 전이도 (State Machine)

```mermaid
stateDiagram-v2
    [*] --> INITIATED: Receipt 초기화

    INITIATED --> PENDING: markAsPending()<br/>(PG 요청 완료)

    PENDING --> COMPLETED: markAsCompleted()<br/>(콜백 "COMPLETED")
    PENDING --> FAILED: markAsFailed()<br/>(콜백 "FAILED")
    PENDING --> CANCELLED: markAsCancelled()<br/>(콜백 "CANCELLED")
    PENDING --> TIMEOUT: markAsTimeout()<br/>(30분 경과)

    COMPLETED --> [*]: 최종 상태
    FAILED --> [*]: 최종 상태
    CANCELLED --> [*]: 최종 상태
    TIMEOUT --> [*]: 재시도 가능 상태

    note right of PENDING
        콜백 대기
        최대 30분
    end note

    note right of TIMEOUT
        배치에서
        자동 처리
        재시도 가능
    end note
```

---

## 6. 타임아웃 배치 처리 (Timeout Batch Handler)

```mermaid
graph TD
    A["@Scheduled<br/>fixedDelay = 5분"] -->|실행| B["ReceiptTimeoutHandler<br/>.handlePending<br/>PaymentTimeout()"]

    B -->|NOW - 30분| C["타임아웃<br/>임계값 계산"]

    C -->|쿼리| D["SELECT * FROM Receipt<br/>WHERE status = PENDING<br/>AND createdAt < 임계값"]

    D -->|결과| E{PENDING<br/>Receipt?}

    E -->|있음| F["각 Receipt에 대해"]
    E -->|없음| G["배치 완료"]

    F -->|반복| H["receipt<br/>.markAsTimeout()"]
    H -->|상태 변경| I["PENDING → TIMEOUT"]
    I -->|저장| J["receiptService<br/>.save(receipt)"]

    J -->|다음| F
    F -->|모두 처리| G

    G -->|로그| K["배치 완료 로그<br/>(info)"]

    L["에러 발생"] -->|catch| M["에러 로그<br/>(error)"]
    M -->|다음 배치까지| N["대기"]
```

---

## 7. Order 상태 전이도

```mermaid
stateDiagram-v2
    [*] --> PENDING: 주문 생성

    PENDING --> PAID: 결제 완료<br/>(콜백 수신)

    PAID --> SHIPPED: 배송 시작
    SHIPPED --> DELIVERED: 배송 완료
    DELIVERED --> COMPLETED: 주문 완료

    PENDING --> CANCELLED: 주문 취소
    PAID --> RETURNED: 반품 신청

    COMPLETED --> [*]: 종료
    CANCELLED --> [*]: 종료
    RETURNED --> [*]: 종료
```

---

## 8. 클래스 다이어그램 (결제 관련)

```mermaid
classDiagram
    class PaymentClient {
        <<interface>>
        +requestPayment()*
    }

    class LoopPaymentClient {
        -webClient: WebClient
        -baseUrl: String
        +requestPayment()
        +performRequest()
        -paymentFallback()
    }

    class PaymentFacade {
        -receiptService
        -orderService
        -paymentClient
        +requestPayment()
        +completePayment()
        -generateTransactionId()
    }

    class ReceiptService {
        -receiptRepository
        +initiateReceipt()
        +markAsPending()
        +updateReceiptStatus()
        +getReceiptByOrderId()
    }

    class Receipt {
        -id
        -orderId
        -transactionId
        -amount
        -status: ReceiptStatus
        -createdAt
        +markAsPending()
        +markAsCompleted()
        +markAsFailed()
        +markAsCancelled()
        +markAsTimeout()
    }

    class ReceiptStatus {
        <<enum>>
        INITIATED
        PENDING
        COMPLETED
        FAILED
        CANCELLED
        TIMEOUT
    }

    class PaymentRequestResult {
        +transactionKey
        +orderId
        +amount
        +status
        +cardType
        +cardNo
        +reason
    }

    class PgPaymentRequest {
        +orderId
        +cardType
        +cardNo
        +amount
        +callbackUrl
    }

    class PgPaymentResponse {
        +transactionKey
        +orderId
        +amount
        +status
        +cardType
        +cardNo
        +reason
    }

    PaymentClient <|.. LoopPaymentClient
    PaymentFacade --> PaymentClient
    PaymentFacade --> ReceiptService
    PaymentFacade --> PaymentRequestResult

    ReceiptService --> Receipt
    Receipt --> ReceiptStatus

    LoopPaymentClient --> PgPaymentRequest
    LoopPaymentClient --> PgPaymentResponse
    LoopPaymentClient --> PaymentRequestResult
```

---

## 9. 에러 처리 흐름

```mermaid
graph TD
    A["결제 요청"] --> B{유효성<br/>검사}

    B -->|Order 없음| C["BAD_REQUEST<br/>주문을 찾을 수 없음"]
    B -->|Order PAID| C
    B -->|정상| D{Receipt<br/>상태 확인}

    D -->|INITIATED| E["CONFLICT<br/>결제가 진행 중"]
    D -->|PENDING| E
    D -->|COMPLETED| F["CONFLICT<br/>이미 완료된 결제"]
    D -->|CANCELLED| G["CONFLICT<br/>취소된 결제"]
    D -->|없음 또는 TIMEOUT/FAILED| H["PG 요청"]

    H --> I{PG 응답}
    I -->|성공| J["Receipt 생성<br/>PENDING 상태"]
    I -->|실패| K["INTERNAL_ERROR<br/>PG 요청 실패"]

    rect rgb(255, 200, 200)
        L["콜백 처리"] --> M{Receipt<br/>상태}
        M -->|INITIATED/PENDING| N["상태 업데이트"]
        M -->|기타| O["무시<br/>(이미 처리됨)"]

        N --> P{status<br/>유효?}
        P -->|COMPLETED| Q["금액 검증"]
        P -->|FAILED/CANCELLED| R["상태만 변경"]
        P -->|기타| S["BAD_REQUEST<br/>유효하지 않은 상태"]

        Q --> T{금액<br/>일치?}
        T -->|일치| U["COMPLETED로 변경"]
        T -->|불일치| V["BAD_REQUEST<br/>금액 불일치"]
    end

    C --> W["에러 응답"]
    E --> W
    F --> W
    G --> W
    K --> W
    S --> W
    V --> W

    J --> X["정상 응답"]
    U --> X
    R --> X
```

---

## 10. 타이밍 다이어그램 (Timeout Scenario)

```mermaid
timing
    title Receipt 타임아웃 시나리오

    section 사용자
    결제 요청 : user, 0, 1m
    콜백 기다리는 중 (무응답) : crit, 1m, 30m

    section Receipt
    INITIATED : receipt, 0, 1m
    PENDING : pending, 1m, 30m
    TIMEOUT (자동) : crit, 30m, 31m

    section 배치
    배치 실행 : batch, 0, 5m
    배치 실행 : batch, 5m, 10m
    배치 실행 : batch, 10m, 15m
    ...타임아웃 임계값까지... : batch, 25m, 30m
    타임아웃 처리 : crit, 30m, 31m

    section 사용자 재시도
    재시도 요청 가능 : retry, 31m, 40m
    (새로운 Receipt 생성) : retry, 32m, 33m
```

---

## 11. 주요 설정값

| 항목 | 값 | 비고 |
|------|-----|------|
| **Receipt 타임아웃** | 30분 | ReceiptTimeoutHandler에서 체크 |
| **배치 실행 주기** | 5분 | @Scheduled(fixedDelay = 5분) |
| **Retry 최대 횟수** | 3회 | resilience4j 설정 |
| **Retry 대기 시간** | 1000ms | resilience4j 설정 |
| **Circuit Breaker 실패율** | 50% | resilience4j 설정 |
| **Circuit Breaker 윈도우** | 10개 호출 | slidingWindowSize |
| **Circuit Breaker OPEN 유지시간** | 5초 | waitDurationInOpenState |

---

## 12. 멱등성 보장

### 콜백 처리의 멱등성

```mermaid
sequenceDiagram
    participant PG
    participant System
    participant DB

    PG->>System: 콜백 시도 1
    System->>DB: FOR UPDATE로 행 락
    DB-->>System: Receipt (PENDING)
    System->>System: 상태 업데이트
    System->>DB: COMMIT
    DB-->>System: ✓ 완료
    System-->>PG: 200 OK

    Note over PG,System: 네트워크 재시도

    PG->>System: 콜백 시도 2 (중복)
    System->>DB: FOR UPDATE로 행 락
    DB-->>System: Receipt (COMPLETED)
    System->>System: 상태 확인<br/>INITIATED/PENDING이 아님<br/>→ 무시
    System-->>PG: 200 OK (멱등)

    Note over System,DB: 같은 결과 (COMPLETED)
    Note over System,DB: 비용 감소 (중복 처리 없음)
```

---

## 주요 특징

### ✅ 안정성
- **Circuit Breaker**: PG 서버 장애 시 연쇄 장애 방지
- **Retry**: 일시적 네트워크 오류 자동 재시도
- **Timeout**: 장시간 미응답 자동 처리
- **멱등성**: 중복 콜백도 안전하게 처리

### ✅ 일관성
- **행 락 (FOR UPDATE)**: 콜백 처리 중 동시 업데이트 방지
- **트랜잭션**: 각 상태 전이 원자적 처리
- **금액 검증**: COMPLETED 시 금액 일치 확인

### ✅ 추적 가능성
- **transactionId**: 각 결제 요청 고유 ID
- **Receipt**: 모든 결제 과정 기록
- **상태 전이**: 각 상태 변경 추적
- **배치 로그**: 타임아웃 처리 기록

### ✅ 확장성
- **PaymentClient 인터페이스**: 다른 PG사 쉽게 추가 (Toss, Kakao, ...)
- **payment-client.yml**: 환경별 설정 분리
- **Resilience4j 설정**: 각 PG별 다른 정책 적용 가능

# 주문 요구사항

## 개요

유저가 여러 상품을 한 번에 주문하고, 자신의 주문 내역을 기간별로 조회하거나 특정 주문의 상세 정보를 확인할 수 있는 기능입니다.
주문 생성 시 상품의 재고를 확인하고 차감하며, 주문 당시의 상품 정보를 스냅샷으로 보존합니다.

## 관련 도메인

| 도메인 | 관계 | 설명 |
|--------|------|------|
| 유저(Member) | 주문자 | 로그인한 유저만 주문할 수 있으며, 본인의 주문만 조회 가능합니다. |
| 상품(Product) | 주문 대상 | 주문 항목에 포함되는 상품이며, 재고 확인 및 차감의 대상입니다. |
| 브랜드(Brand) | 상품 소속 | 주문 스냅샷에 브랜드명이 포함되어 주문 당시의 브랜드 정보를 보존합니다. |

---

## Part 1: API 명세

### 1.1 주문 요청 API

#### Endpoint
- **Method**: `POST`
- **URI**: `/api/v1/orders`
- **인증**: 필수 (`X-Loopers-LoginId`, `X-Loopers-LoginPw` 헤더)
- **설명**: 유저가 여러 상품을 한 번에 주문합니다. 주문 항목별로 상품 ID와 수량을 지정합니다.

#### Request Body
```json
{
  "items": [
    { "productId": 1, "quantity": 2 },
    { "productId": 3, "quantity": 1 }
  ]
}
```

| 필드 | 타입 | 필수 | 설명 | 제약 조건 |
|------|------|------|------|-----------|
| `items` | Array | O | 주문 항목 목록 | 최소 1개 이상 |
| `items[].productId` | Long | O | 주문할 상품 ID | 존재하는 상품이어야 함 |
| `items[].quantity` | Int | O | 주문 수량 | 1 이상 99 이하 |

#### Response (성공) - HTTP 200
```json
{
  "meta": {
    "result": "SUCCESS",
    "errorCode": null,
    "message": null
  },
  "data": {
    "orderId": 1,
    "orderStatus": "ORDERED",
    "orderedAt": "2026-02-13T10:30:00",
    "totalAmount": 55000,
    "items": [
      {
        "productName": "베이직 티셔츠",
        "brandName": "루퍼스",
        "price": 25000,
        "quantity": 2,
        "subTotal": 50000
      },
      {
        "productName": "캔버스 에코백",
        "brandName": "루퍼스",
        "price": 5000,
        "quantity": 1,
        "subTotal": 5000
      }
    ]
  }
}
```

#### Response (실패)

| 상황 | HTTP 상태 | errorCode | message |
|------|-----------|-----------|---------|
| 인증 헤더 누락 또는 잘못된 로그인 정보 | 401 | UNAUTHORIZED | 인증이 필요합니다. |
| 요청 바디가 비어있거나 items가 빈 배열 | 400 | Bad Request | 주문 항목은 최소 1개 이상이어야 합니다. |
| 수량이 1 미만 또는 99 초과 | 400 | Bad Request | 주문 수량은 1개 이상 99개 이하여야 합니다. |
| 동일 상품 ID가 중복으로 포함됨 | 400 | Bad Request | 동일한 상품을 중복으로 주문할 수 없습니다. 수량을 조정해 주세요. |
| 존재하지 않는 상품 ID | 404 | Not Found | 존재하지 않는 상품입니다. |
| 재고 부족 | 400 | Bad Request | 상품의 재고가 부족합니다. (상품명: {상품명}, 요청 수량: {quantity}개, 현재 재고: {stock}개) |

---

### 1.2 주문 목록 조회 API

#### Endpoint
- **Method**: `GET`
- **URI**: `/api/v1/orders`
- **인증**: 필수 (`X-Loopers-LoginId`, `X-Loopers-LoginPw` 헤더)
- **설명**: 유저가 자신의 주문 목록을 기간별로 조회합니다. 본인의 주문만 조회 가능합니다.

#### Query Parameters

| 파라미터 | 타입 | 필수 | 기본값 | 예시 | 설명 |
|----------|------|------|--------|------|------|
| `startAt` | LocalDate | O | - | `2026-01-01` | 조회 시작일 |
| `endAt` | LocalDate | O | - | `2026-02-13` | 조회 종료일 |

#### 요청 예시
```http
GET /api/v1/orders?startAt=2026-01-01&endAt=2026-02-13
X-Loopers-LoginId: testuser01
X-Loopers-LoginPw: password123!
```

#### Response (성공) - HTTP 200
```json
{
  "meta": {
    "result": "SUCCESS",
    "errorCode": null,
    "message": null
  },
  "data": {
    "orders": [
      {
        "orderId": 2,
        "orderStatus": "ORDERED",
        "orderedAt": "2026-02-10T14:00:00",
        "totalAmount": 55000,
        "itemCount": 2
      },
      {
        "orderId": 1,
        "orderStatus": "ORDERED",
        "orderedAt": "2026-01-15T09:30:00",
        "totalAmount": 30000,
        "itemCount": 1
      }
    ]
  }
}
```

- 주문 목록은 최신 주문순(orderedAt 내림차순)으로 정렬됩니다.
- 각 주문의 상세 항목은 포함하지 않고, 주문 건 단위의 요약 정보만 반환합니다.

#### Response (실패)

| 상황 | HTTP 상태 | errorCode | message |
|------|-----------|-----------|---------|
| 인증 헤더 누락 또는 잘못된 로그인 정보 | 401 | UNAUTHORIZED | 인증이 필요합니다. |
| startAt 또는 endAt 누락 | 400 | Bad Request | 조회 시작일과 종료일은 필수입니다. |
| startAt이 endAt보다 이후인 경우 | 400 | Bad Request | 조회 시작일은 종료일보다 이전이어야 합니다. |
| 조회 기간이 3개월(90일)을 초과 | 400 | Bad Request | 조회 기간은 최대 3개월까지 가능합니다. |

---

### 1.3 단일 주문 상세 조회 API

#### Endpoint
- **Method**: `GET`
- **URI**: `/api/v1/orders/{orderId}`
- **인증**: 필수 (`X-Loopers-LoginId`, `X-Loopers-LoginPw` 헤더)
- **설명**: 유저가 특정 주문의 상세 내역을 조회합니다. 본인의 주문만 조회 가능합니다.

#### Path Parameters

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `orderId` | Long | 조회할 주문 ID |

#### Response (성공) - HTTP 200
```json
{
  "meta": {
    "result": "SUCCESS",
    "errorCode": null,
    "message": null
  },
  "data": {
    "orderId": 1,
    "orderStatus": "ORDERED",
    "orderedAt": "2026-02-13T10:30:00",
    "totalAmount": 55000,
    "items": [
      {
        "productName": "베이직 티셔츠",
        "brandName": "루퍼스",
        "price": 25000,
        "quantity": 2,
        "subTotal": 50000
      },
      {
        "productName": "캔버스 에코백",
        "brandName": "루퍼스",
        "price": 5000,
        "quantity": 1,
        "subTotal": 5000
      }
    ]
  }
}
```

#### Response (실패)

| 상황 | HTTP 상태 | errorCode | message |
|------|-----------|-----------|---------|
| 인증 헤더 누락 또는 잘못된 로그인 정보 | 401 | UNAUTHORIZED | 인증이 필요합니다. |
| 존재하지 않는 주문 ID | 404 | Not Found | 존재하지 않는 주문입니다. |
| 다른 유저의 주문을 조회하려는 경우 | 404 | Not Found | 존재하지 않는 주문입니다. |

- 다른 유저의 주문을 조회하려는 경우 보안상 404를 반환합니다. (주문 존재 여부를 노출하지 않기 위함)

---

## Part 2: 비즈니스 규칙

### 2.1 필수 규칙

#### 주문 생성 규칙
1. **인증 필수**: 로그인한 유저만 주문할 수 있습니다.
2. **최소 항목 수**: 주문에는 최소 1개 이상의 항목이 포함되어야 합니다.
3. **수량 범위**: 각 주문 항목의 수량은 1개 이상 99개 이하여야 합니다.
4. **상품 중복 불가**: 동일한 상품 ID가 주문 항목에 중복으로 포함될 수 없습니다. 같은 상품을 여러 개 주문하려면 수량(quantity)을 조정해야 합니다.
5. **상품 존재 확인**: 주문 항목의 모든 상품 ID가 실제 존재하는 상품이어야 합니다.
6. **재고 확인 및 차감**: 주문 요청된 각 상품의 재고가 충분한지 확인하고, 충분한 경우 주문 수량만큼 재고를 차감합니다.
7. **원자성 보장**: 여러 상품을 한 번에 주문하므로, 일부 상품의 재고가 부족하면 전체 주문이 실패해야 합니다. (All or Nothing)
8. **상품 정보 스냅샷**: 주문 당시의 상품명, 가격, 브랜드명을 주문 항목에 스냅샷으로 저장합니다. 이후 상품 정보가 변경되더라도 주문 내역에는 주문 시점의 정보가 유지됩니다.
9. **주문 상태**: 주문 생성 시 초기 상태는 `ORDERED`입니다.

#### 주문 조회 규칙
1. **본인 주문만 조회**: 유저는 자신의 주문만 조회할 수 있습니다. 다른 유저의 주문에는 접근할 수 없습니다.
2. **기간 필수**: 주문 목록 조회 시 시작일(startAt)과 종료일(endAt)은 필수입니다.
3. **기간 유효성**: 시작일은 종료일보다 이전이어야 합니다.
4. **최대 조회 기간**: 시작일과 종료일 사이의 기간은 최대 3개월(90일)까지 가능합니다.
5. **정렬**: 주문 목록은 최신 주문순(주문일시 내림차순)으로 정렬됩니다.
6. **보안**: 다른 유저의 주문 ID로 상세 조회를 시도하면 주문 존재 여부를 노출하지 않기 위해 404를 반환합니다.

### 2.2 동시성 제어

- 동일 상품에 여러 유저가 동시에 주문할 때 재고 정합성을 보장하기 위해 **비관적 락(Pessimistic Lock)**을 사용합니다.
- JPA의 `@Lock(LockModeType.PESSIMISTIC_WRITE)`를 사용하여 재고 조회 시 행 잠금을 걸어 동시성을 제어합니다.
- 데드락 방지를 위해 상품 ID 오름차순으로 정렬하여 순서대로 락을 획득합니다.

### 2.3 결제

- 결제 기능은 이후 단계에서 추가로 개발됩니다.
- 초기 구현에서는 주문 생성과 재고 차감까지만 처리합니다.

---

## Part 3: 구현 컴포넌트

### 3.1 레이어별 구조

```
interfaces/api/order/
  ├── OrderV1ApiSpec.kt          # API 인터페이스 (Swagger 문서)
  ├── OrderV1Controller.kt       # REST Controller
  └── OrderV1Dto.kt              # Request/Response DTO

application/order/
  ├── OrderFacade.kt             # 주문 유스케이스 조합 (인증 + 상품 조회 + 주문 생성)
  └── OrderInfo.kt               # Facade 레이어 정보 객체

domain/order/
  ├── OrderModel.kt              # 주문 엔티티 (주문 헤더)
  ├── OrderItemModel.kt          # 주문 항목 엔티티 (주문 상세, 스냅샷 포함)
  ├── OrderStatus.kt             # 주문 상태 열거형 (ORDERED)
  ├── OrderService.kt            # 주문 도메인 서비스
  └── OrderRepository.kt         # 주문 Repository 인터페이스

infrastructure/order/
  ├── OrderRepositoryImpl.kt     # 주문 Repository 구현
  └── OrderJpaRepository.kt      # Spring Data JPA Repository
```

### 3.2 처리 흐름

#### 주문 요청 흐름
```
1. Controller: 요청 수신 및 DTO 변환
2. Facade: 인증 검증 → 유저 조회
3. Facade: 요청 검증 (중복 상품, 수량 범위)
4. Facade: 상품 목록 조회 (ProductService 활용)
5. Domain Service: 비관적 락으로 재고 확인 및 차감
6. Domain Service: 주문 생성 (스냅샷 포함)
7. Controller: 응답 반환
```

#### 주문 목록 조회 흐름
```
1. Controller: 요청 수신 및 쿼리 파라미터 바인딩
2. Facade: 인증 검증 → 유저 조회
3. Facade: 기간 유효성 검증 (startAt < endAt, 최대 3개월)
4. Domain Service: 해당 유저의 주문 목록 조회 (기간 필터, 최신순 정렬)
5. Controller: 응답 반환
```

#### 단일 주문 상세 조회 흐름
```
1. Controller: 요청 수신 및 orderId 추출
2. Facade: 인증 검증 → 유저 조회
3. Domain Service: 주문 조회 (유저 ID + 주문 ID로 조회하여 본인 확인)
4. Controller: 응답 반환 (스냅샷 데이터 포함)
```

---

## Part 4: 구현 체크리스트

### Phase 1: 도메인 모델 및 Repository 구현

주문 도메인의 핵심 엔티티와 Repository를 구현합니다.

- [ ] **RED**: `OrderModel`, `OrderItemModel`, `OrderStatus` 도메인 모델에 대한 단위 테스트 작성
  - 주문 생성 시 상태가 ORDERED로 설정되는지 검증
  - 주문 항목에 스냅샷 정보(상품명, 가격, 브랜드명)가 저장되는지 검증
  - 총 주문 금액(totalAmount) 계산이 올바른지 검증
- [ ] **GREEN**: 테스트를 통과하는 최소 구현
  - `OrderStatus` 열거형 생성
  - `OrderModel` 엔티티 생성 (유저 ID, 주문 상태, 주문 일시, 총 금액)
  - `OrderItemModel` 엔티티 생성 (주문 ID, 상품 ID, 상품명, 브랜드명, 가격, 수량, 소계)
  - `OrderRepository` 인터페이스 생성
  - `OrderJpaRepository`, `OrderRepositoryImpl` 구현
- [ ] **REFACTOR**: 코드 정리 및 품질 개선

### Phase 2: 주문 생성 서비스 구현

주문 생성의 비즈니스 로직(재고 확인, 재고 차감, 스냅샷 저장)을 구현합니다.

- [ ] **RED**: `OrderService.createOrder` 에 대한 단위 테스트 작성
  - 정상 주문 생성 시 재고 차감 및 스냅샷 저장 검증
  - 재고 부족 시 예외 발생 및 상세 에러 메시지 검증
  - 여러 상품 주문 시 일부 재고 부족이면 전체 주문 실패 검증 (원자성)
  - 비관적 락을 통한 동시성 제어 검증
- [ ] **GREEN**: 테스트를 통과하는 최소 구현
  - 상품 조회 및 재고 확인 로직
  - 비관적 락을 활용한 재고 차감 로직
  - 주문 및 주문 항목 생성 로직 (스냅샷 포함)
- [ ] **REFACTOR**: 코드 정리 및 품질 개선

### Phase 3: 주문 조회 서비스 구현

주문 목록 조회 및 단일 주문 상세 조회의 비즈니스 로직을 구현합니다.

- [ ] **RED**: `OrderService`의 조회 메서드에 대한 단위 테스트 작성
  - 기간별 주문 목록 조회 (최신순 정렬) 검증
  - 시작일이 종료일보다 이후인 경우 예외 발생 검증
  - 조회 기간 3개월 초과 시 예외 발생 검증
  - 본인 주문만 조회되는지 검증
  - 존재하지 않는 주문 ID로 조회 시 예외 발생 검증
  - 다른 유저의 주문 조회 시 NOT_FOUND 예외 발생 검증
- [ ] **GREEN**: 테스트를 통과하는 최소 구현
  - 기간 유효성 검증 로직
  - 기간별 주문 목록 조회 쿼리 (유저 ID + 기간 필터)
  - 단일 주문 상세 조회 (유저 ID + 주문 ID)
- [ ] **REFACTOR**: 코드 정리 및 품질 개선

### Phase 4: Facade 및 Controller 구현

API 레이어(Controller, Facade, DTO)를 구현하고 통합 테스트를 작성합니다.

- [ ] **RED**: `OrderFacade` 및 `OrderV1Controller`에 대한 통합 테스트 작성
  - 주문 생성 API 통합 테스트 (정상 / 인증 실패 / 유효성 검증 실패)
  - 주문 목록 조회 API 통합 테스트 (정상 / 기간 유효성 / 인증 실패)
  - 단일 주문 상세 조회 API 통합 테스트 (정상 / 미존재 / 타인 주문)
- [ ] **GREEN**: 테스트를 통과하는 최소 구현
  - `OrderV1Dto` (Request/Response DTO) 생성
  - `OrderV1ApiSpec` (Swagger API 인터페이스) 생성
  - `OrderInfo` (Facade 레이어 정보 객체) 생성
  - `OrderFacade` (인증 검증, 서비스 조합) 생성
  - `OrderV1Controller` 생성
- [ ] **REFACTOR**: 코드 정리 및 품질 개선

### Phase 5: E2E 테스트 및 API 문서 정리

실제 API를 호출하여 전체 흐름을 검증하고 .http 파일을 작성합니다.

- [ ] E2E 테스트 작성 (실제 API 호출을 통한 전체 흐름 검증)
- [ ] `.http/order/create-order.http` 작성 (주문 생성 API 테스트)
- [ ] `.http/order/get-orders.http` 작성 (주문 목록 조회 API 테스트)
- [ ] `.http/order/get-order-detail.http` 작성 (주문 상세 조회 API 테스트)

---

## Part 5: 테스트 시나리오

### 5.1 단위 테스트

#### 도메인 모델 테스트
| 테스트 케이스 | 검증 내용 |
|--------------|-----------|
| 주문 생성 시 초기 상태 | 주문 생성 시 상태가 ORDERED로 설정되는지 검증 |
| 총 금액 계산 | 주문 항목들의 (가격 x 수량) 합계가 올바른지 검증 |
| 스냅샷 저장 | 주문 항목에 상품명, 가격, 브랜드명이 스냅샷으로 저장되는지 검증 |

#### 도메인 서비스 테스트
| 테스트 케이스 | 검증 내용 |
|--------------|-----------|
| 정상 주문 생성 | 재고 확인 후 차감, 주문 및 항목 생성 성공 |
| 재고 부족 | 재고 부족 시 상세 에러 메시지와 함께 BAD_REQUEST 예외 발생 |
| 원자성 보장 | 3개 상품 중 1개 재고 부족 시 전체 주문 실패, 재고 차감 없음 |
| 중복 상품 검증 | 동일 상품 ID가 중복 포함 시 BAD_REQUEST 예외 발생 |
| 수량 범위 검증 | 수량이 0 이하 또는 100 이상일 때 BAD_REQUEST 예외 발생 |
| 기간 유효성 검증 | startAt > endAt 시 BAD_REQUEST 예외 발생 |
| 기간 초과 검증 | 조회 기간 90일 초과 시 BAD_REQUEST 예외 발생 |
| 본인 주문 조회 | 본인의 주문만 조회 결과에 포함되는지 검증 |
| 타인 주문 조회 불가 | 다른 유저의 주문 ID로 조회 시 NOT_FOUND 예외 발생 |

### 5.2 통합 테스트

| 테스트 케이스 | 검증 내용 |
|--------------|-----------|
| 주문 생성 성공 | POST /api/v1/orders 정상 요청 시 200 + 주문 데이터 반환 |
| 주문 생성 - 인증 실패 | 헤더 누락 시 401 반환 |
| 주문 생성 - 빈 항목 | items가 빈 배열일 때 400 반환 |
| 주문 생성 - 수량 초과 | quantity가 100일 때 400 반환 |
| 주문 생성 - 중복 상품 | 동일 productId 중복 시 400 반환 |
| 주문 생성 - 미존재 상품 | 존재하지 않는 productId 포함 시 404 반환 |
| 주문 생성 - 재고 부족 | 재고보다 많은 수량 요청 시 400 + 상세 메시지 반환 |
| 주문 목록 조회 성공 | GET /api/v1/orders?startAt=...&endAt=... 정상 요청 시 200 반환 |
| 주문 목록 조회 - 기간 초과 | 91일 기간 요청 시 400 반환 |
| 주문 목록 조회 - 역순 기간 | startAt > endAt 시 400 반환 |
| 주문 상세 조회 성공 | GET /api/v1/orders/{orderId} 정상 요청 시 200 + 스냅샷 데이터 반환 |
| 주문 상세 조회 - 타인 주문 | 다른 유저의 주문 조회 시 404 반환 |

### 5.3 E2E 테스트

| 시나리오 | 흐름 |
|----------|------|
| 주문 전체 흐름 | 회원가입 -> 상품 등록(어드민) -> 주문 생성 -> 주문 목록 조회 -> 주문 상세 조회 |
| 재고 부족 시나리오 | 상품 등록(재고 5개) -> 주문 생성(수량 3개) -> 주문 생성(수량 3개, 실패 확인) |
| 동시 주문 시나리오 | 상품 등록(재고 1개) -> 두 유저가 동시에 주문 -> 한 건만 성공, 한 건은 재고 부족 확인 |

---

## Part 6: 보안 고려사항

| 항목 | 대응 방안 |
|------|-----------|
| 인증 검증 | 모든 주문 API는 `X-Loopers-LoginId`, `X-Loopers-LoginPw` 헤더로 유저를 식별하고, 로그인 정보가 올바른지 검증합니다. |
| 본인 데이터 접근 제한 | 주문 조회 시 반드시 유저 ID를 조건에 포함하여 본인의 주문만 조회되도록 합니다. |
| 주문 존재 여부 노출 방지 | 다른 유저의 주문 조회 시 403(Forbidden) 대신 404(Not Found)를 반환하여 주문 존재 여부를 외부에 노출하지 않습니다. |
| 입력값 검증 | 요청 본문의 모든 필드를 서버 측에서 검증합니다. (items 비어있음, 수량 범위, 상품 중복 등) |
| SQL 인젝션 방지 | JPA 파라미터 바인딩을 사용하여 쿼리 파라미터를 안전하게 처리합니다. |
| 비관적 락 타임아웃 | 비관적 락에 적절한 타임아웃을 설정하여 무한 대기를 방지합니다. |

---

## Part 7: 검증 명령어

```bash
# 전체 테스트 실행
./gradlew :apps:commerce-api:test

# 주문 관련 테스트만 실행
./gradlew :apps:commerce-api:test --tests "*Order*"

# 테스트 커버리지 확인
./gradlew :apps:commerce-api:test jacocoTestReport

# ktlint 검사
./gradlew ktlintCheck

# ktlint 자동 수정
./gradlew ktlintFormat

# 빌드
./gradlew :apps:commerce-api:build
```

---

## 품질 체크리스트

- [x] 상품/브랜드/좋아요/주문 등 관련 도메인과의 관계가 명시되어 있는가?
  - 유저(주문자), 상품(주문 대상), 브랜드(스냅샷) 도메인과의 관계를 "관련 도메인" 섹션에 명시함
- [x] 기능 요구사항이 유저 중심("유저가 ~한다")으로 서술되어 있는가?
  - 개요 및 각 API 설명에 "유저가 주문한다", "유저가 조회한다" 형태로 서술함
- [x] 인증 방식(헤더 기반)이 정확히 명시되어 있는가?
  - 모든 API에 `X-Loopers-LoginId`, `X-Loopers-LoginPw` 헤더 필수 명시함
- [x] 에러 케이스와 예외 상황이 포함되어 있는가?
  - 각 API별 실패 응답 표에 모든 에러 케이스를 상세히 정리함
- [x] Phase별 구현 체크리스트가 TDD 워크플로우에 맞게 구성되어 있는가?
  - Phase 1~4는 RED -> GREEN -> REFACTOR 단계로 구성, Phase 5는 E2E 및 문서 정리

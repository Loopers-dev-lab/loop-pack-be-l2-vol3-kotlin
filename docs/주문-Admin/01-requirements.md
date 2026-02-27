# 주문 Admin 요구사항

## 개요
어드민이 전체 사용자의 주문을 조회하고 관리할 수 있는 API를 제공합니다. 대고객 주문 API와 달리, 어드민은 모든 유저의 주문을 페이징/필터링/정렬하여 조회할 수 있으며, 주문 상세에서 주문자 정보와 주문 항목별 소계를 포함한 확장 정보를 확인할 수 있습니다.

## 관련 도메인
| 도메인 | 관계 | 설명 |
|--------|------|------|
| 주문(Order) | 직접 대상 | 조회 대상이 되는 핵심 도메인. 주문 모델, 주문 항목 모델, 주문 상태 등을 사용합니다. |
| 유저(Member) | 주문자 정보 | 주문을 생성한 유저의 정보(loginId, 이름, 이메일)를 상세 조회에서 표시합니다. |
| 상품(Product) | 주문 항목 참조 | 주문 항목에 저장된 상품 스냅샷(상품명, 가격)을 표시합니다. |
| 브랜드(Brand) | 상품 소속 | 주문 항목의 상품 스냅샷에 브랜드명을 포함합니다. |

**선행 조건**: 이 기능은 주문 도메인(Order, OrderItem, OrderStatus)이 먼저 구현되어 있어야 합니다. 대고객 주문 API(`docs/주문/01-requirements.md`)의 구현이 선행되어야 합니다.

---

## Part 1: API 명세

### 1.1 주문 목록 조회

#### Endpoint
```
GET /api-admin/v1/orders
X-Loopers-Ldap: loopers.admin
```

#### 쿼리 파라미터

| 파라미터 | 타입 | 필수 여부 | 기본값 | 예시 | 설명 |
|----------|------|-----------|--------|------|------|
| `page` | Int | X | `0` | `0` | 페이지 번호 (0부터 시작) |
| `size` | Int | X | `20` | `20` | 페이지당 주문 수 (최대 100) |
| `status` | String | X | 없음 | `ORDERED` | 주문 상태별 필터링 |
| `loginId` | String | X | 없음 | `testuser01` | 특정 유저의 주문만 필터링 |
| `sort` | String | X | `orderedAt` | `orderedAt` | 정렬 기준 필드 (`orderedAt`, `totalAmount`) |
| `direction` | String | X | `DESC` | `ASC` | 정렬 방향 (`ASC`, `DESC`) |

#### 요청 예시
```http
GET /api-admin/v1/orders?page=0&size=20&status=ORDERED&loginId=testuser01&sort=orderedAt&direction=DESC
X-Loopers-Ldap: loopers.admin
```

#### Response (성공 - 200 OK)
```json
{
  "meta": {
    "result": "SUCCESS",
    "errorCode": null,
    "message": null
  },
  "data": {
    "content": [
      {
        "orderId": 1,
        "loginId": "testuser01",
        "status": "ORDERED",
        "totalAmount": 35000,
        "itemCount": 2,
        "orderedAt": "2026-02-10T14:30:00+09:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

#### Response (실패)

##### LDAP 헤더 누락 또는 유효하지 않은 값 (401 Unauthorized)
```json
{
  "meta": {
    "result": "FAIL",
    "errorCode": "UNAUTHORIZED",
    "message": "인증이 필요합니다."
  },
  "data": null
}
```

##### 유효하지 않은 status 값 (400 Bad Request)
```json
{
  "meta": {
    "result": "FAIL",
    "errorCode": "Bad Request",
    "message": "유효하지 않은 주문 상태입니다. 사용 가능한 값: [ORDERED, PREPARING, SHIPPING, DELIVERED, CANCELLED]"
  },
  "data": null
}
```

##### 유효하지 않은 sort 값 (400 Bad Request)
```json
{
  "meta": {
    "result": "FAIL",
    "errorCode": "Bad Request",
    "message": "유효하지 않은 정렬 기준입니다. 사용 가능한 값: [orderedAt, totalAmount]"
  },
  "data": null
}
```

---

### 1.2 단일 주문 상세 조회

#### Endpoint
```
GET /api-admin/v1/orders/{orderId}
X-Loopers-Ldap: loopers.admin
```

#### 요청 예시
```http
GET /api-admin/v1/orders/1
X-Loopers-Ldap: loopers.admin
```

#### Response (성공 - 200 OK)
```json
{
  "meta": {
    "result": "SUCCESS",
    "errorCode": null,
    "message": null
  },
  "data": {
    "orderId": 1,
    "orderer": {
      "loginId": "testuser01",
      "name": "홍길동",
      "email": "hong@example.com"
    },
    "status": "ORDERED",
    "items": [
      {
        "orderItemId": 1,
        "productId": 10,
        "productName": "클래식 티셔츠",
        "brandName": "루퍼스",
        "price": 15000,
        "quantity": 2,
        "subtotal": 30000
      },
      {
        "orderItemId": 2,
        "productId": 25,
        "productName": "데님 팬츠",
        "brandName": "루퍼스",
        "price": 5000,
        "quantity": 1,
        "subtotal": 5000
      }
    ],
    "totalAmount": 35000,
    "orderedAt": "2026-02-10T14:30:00+09:00"
  }
}
```

#### Response (실패)

##### 존재하지 않는 주문 (404 Not Found)
```json
{
  "meta": {
    "result": "FAIL",
    "errorCode": "Not Found",
    "message": "존재하지 않는 주문입니다."
  },
  "data": null
}
```

##### LDAP 헤더 누락 또는 유효하지 않은 값 (401 Unauthorized)
```json
{
  "meta": {
    "result": "FAIL",
    "errorCode": "UNAUTHORIZED",
    "message": "인증이 필요합니다."
  },
  "data": null
}
```

---

## Part 2: 비즈니스 규칙

### 2.1 필수 규칙

| # | 규칙 | 설명 |
|---|------|------|
| 1 | LDAP 인증 필수 | 모든 어드민 API는 `X-Loopers-Ldap: loopers.admin` 헤더가 반드시 포함되어야 합니다. 헤더가 없거나 값이 `loopers.admin`이 아닌 경우 401 Unauthorized를 반환합니다. |
| 2 | 전체 유저 주문 조회 가능 | 대고객 API와 달리, 어드민은 모든 유저의 주문을 조회할 수 있습니다. 본인 주문 제한이 없습니다. |
| 3 | 페이징 필수 적용 | 주문 목록 조회는 항상 페이징 처리됩니다. page 기본값 0, size 기본값 20, size 최대값 100입니다. |
| 4 | 상세 조회 시 주문자 정보 포함 | 어드민이 주문자를 식별할 수 있도록 주문 상세에 주문자의 loginId, 이름, 이메일을 마스킹 없이 원본으로 포함합니다. |
| 5 | 상품 스냅샷 데이터 사용 | 주문 항목의 상품 정보는 주문 시점의 스냅샷 데이터를 사용합니다. 현재 상품 정보가 아닌 주문 당시 저장된 정보를 표시합니다. |

### 2.2 주문 상태 정의

주문 도메인에서 사용하는 상태 값은 다음과 같습니다:

| 상태 | 설명 |
|------|------|
| `ORDERED` | 주문 완료 (초기 상태) |
| `PREPARING` | 배송 준비 중 |
| `SHIPPING` | 배송 중 |
| `DELIVERED` | 배송 완료 |
| `CANCELLED` | 주문 취소 |

### 2.3 필터링 규칙

| 필터 | 규칙 |
|------|------|
| `status` | OrderStatus enum 값 중 하나와 정확히 일치해야 합니다. 유효하지 않은 값이면 400 Bad Request를 반환합니다. |
| `loginId` | 해당 loginId를 가진 유저의 주문만 조회합니다. 존재하지 않는 loginId인 경우 빈 목록을 반환합니다 (에러가 아닙니다). |

### 2.4 정렬 규칙

| 정렬 기준 | 설명 |
|-----------|------|
| `orderedAt` (기본값) | 주문 일시 기준 정렬 |
| `totalAmount` | 주문 총액 기준 정렬 |

- 정렬 방향 기본값은 `DESC` (내림차순)입니다.
- `direction` 파라미터는 `ASC` 또는 `DESC`만 허용합니다. 유효하지 않은 값이면 400 Bad Request를 반환합니다.

### 2.5 금액 계산 규칙

| 항목 | 계산 방식 |
|------|-----------|
| `subtotal` (항목별 소계) | `price * quantity` |
| `totalAmount` (주문 총액) | 모든 항목의 `subtotal` 합계 |

---

## Part 3: 구현 컴포넌트

### 3.1 레이어별 구조

```
interfaces/api/admin/order/
  ├── OrderAdminV1Controller.kt     # 어드민 주문 조회 컨트롤러
  ├── OrderAdminV1ApiSpec.kt        # OpenAPI 스펙 인터페이스
  └── OrderAdminV1Dto.kt            # Request/Response DTO

application/admin/order/
  ├── OrderAdminFacade.kt           # 어드민 주문 조회 퍼사드
  └── OrderAdminInfo.kt             # 내부 전달 DTO

domain/order/
  ├── OrderModel.kt                 # 주문 엔티티 (주문 도메인에서 구현)
  ├── OrderItemModel.kt             # 주문 항목 엔티티 (주문 도메인에서 구현)
  ├── OrderStatus.kt                # 주문 상태 enum (주문 도메인에서 구현)
  ├── OrderService.kt               # 주문 도메인 서비스 (주문 도메인에서 구현, 어드민용 조회 메서드 추가)
  └── OrderRepository.kt            # 주문 리포지토리 인터페이스 (주문 도메인에서 구현, 어드민용 조회 메서드 추가)

infrastructure/order/
  ├── OrderJpaRepository.kt         # JPA 리포지토리 (주문 도메인에서 구현)
  └── OrderRepositoryImpl.kt        # 리포지토리 구현체 (주문 도메인에서 구현, 어드민용 조회 메서드 추가)

infrastructure/admin/
  └── LdapAuthenticationFilter.kt   # LDAP 어드민 인증 필터 (/api-admin/** 경로 보호)
```

### 3.2 처리 흐름

#### 주문 목록 조회
```
Controller (GET /api-admin/v1/orders)
  -> LdapAuthenticationFilter: X-Loopers-Ldap 헤더 검증
  -> OrderAdminFacade.getOrders(page, size, status, loginId, sort, direction)
    -> OrderService.findAllForAdmin(page, size, status, loginId, sort, direction)
      -> OrderRepository.findAllWithFilters(pageable, status, loginId): Page<OrderModel>
    -> 각 주문의 itemCount, totalAmount 계산
    -> OrderAdminInfo.OrderListItem 목록으로 변환
  -> OrderAdminV1Dto.OrderListResponse로 변환하여 응답
```

#### 단일 주문 상세 조회
```
Controller (GET /api-admin/v1/orders/{orderId})
  -> LdapAuthenticationFilter: X-Loopers-Ldap 헤더 검증
  -> OrderAdminFacade.getOrderDetail(orderId)
    -> OrderService.findByIdWithItems(orderId): OrderModel (주문 항목 포함)
      -> 주문이 존재하지 않으면 NOT_FOUND 예외 발생
    -> MemberService.findById(order.memberId): MemberModel (주문자 정보 조회)
    -> OrderAdminInfo.OrderDetail로 변환 (주문자 정보 + 주문 항목 + 금액 계산)
  -> OrderAdminV1Dto.OrderDetailResponse로 변환하여 응답
```

---

## Part 4: 구현 체크리스트

### Phase 1: LDAP 인증 필터 구현
어드민 API 경로(`/api-admin/**`)를 보호하는 LDAP 인증 필터를 구현합니다.

- [ ] `LdapAuthenticationFilter` 구현: `X-Loopers-Ldap` 헤더 값이 `loopers.admin`인지 검증
- [ ] `/api-admin/` 으로 시작하는 경로에만 필터 적용 (`shouldNotFilter` 메서드)
- [ ] 인증 실패 시 401 Unauthorized 응답 반환
- [ ] `LdapAuthenticationFilterTest` 단위 테스트 작성
  - 유효한 LDAP 헤더로 요청 시 필터 통과
  - LDAP 헤더 누락 시 401 반환
  - 잘못된 LDAP 값 시 401 반환
  - `/api-admin/` 이 아닌 경로는 필터 미적용

### Phase 2: 어드민 주문 목록 조회 API 구현
어드민이 전체 유저의 주문을 페이징/필터링/정렬하여 조회할 수 있는 API를 구현합니다.

- [ ] `OrderAdminV1Dto.OrderListResponse` DTO 정의 (orderId, loginId, status, totalAmount, itemCount, orderedAt)
- [ ] `OrderAdminV1Dto.PageResponse` DTO 정의 (content, page, size, totalElements, totalPages)
- [ ] `OrderAdminInfo.OrderListItem` 내부 DTO 정의
- [ ] `OrderRepository`에 어드민용 조회 메서드 추가: `findAllForAdmin(pageable, status, loginId)`
- [ ] `OrderRepositoryImpl`에 구현 추가 (Spring Data JPA Specification 또는 QueryDSL 활용)
- [ ] `OrderService`에 어드민용 조회 메서드 추가: `findAllForAdmin(page, size, status, loginId, sort, direction)`
- [ ] `OrderAdminFacade.getOrders()` 구현
- [ ] `OrderAdminV1ApiSpec` 인터페이스 정의
- [ ] `OrderAdminV1Controller` 구현 (GET /api-admin/v1/orders)
- [ ] 단위 테스트 작성
  - `OrderServiceTest`: 필터링/정렬/페이징 조합 테스트
  - `OrderAdminFacadeTest`: 목록 조회 오케스트레이션 테스트
- [ ] 통합 테스트 작성
  - 필터 없이 전체 목록 조회
  - status 필터 적용
  - loginId 필터 적용
  - status + loginId 복합 필터 적용
  - 정렬 기준 변경 (orderedAt ASC, totalAmount DESC 등)
  - 페이징 동작 확인
  - 유효하지 않은 status 값 시 400 응답
  - LDAP 인증 미포함 시 401 응답

### Phase 3: 어드민 단일 주문 상세 조회 API 구현
어드민이 특정 주문의 상세 내역(주문자 정보, 주문 항목, 금액 소계/총액)을 조회할 수 있는 API를 구현합니다.

- [ ] `OrderAdminV1Dto.OrderDetailResponse` DTO 정의 (orderId, orderer, status, items, totalAmount, orderedAt)
- [ ] `OrderAdminV1Dto.OrdererResponse` DTO 정의 (loginId, name, email)
- [ ] `OrderAdminV1Dto.OrderItemResponse` DTO 정의 (orderItemId, productId, productName, brandName, price, quantity, subtotal)
- [ ] `OrderAdminInfo.OrderDetail` 내부 DTO 정의
- [ ] `OrderRepository`에 주문 항목 포함 조회 메서드 추가 (이미 있으면 재사용): `findByIdWithItems(orderId)`
- [ ] `OrderAdminFacade.getOrderDetail(orderId)` 구현 (주문 조회 + 주문자 조회 + DTO 변환)
- [ ] `OrderAdminV1Controller`에 상세 조회 엔드포인트 추가 (GET /api-admin/v1/orders/{orderId})
- [ ] 단위 테스트 작성
  - `OrderAdminFacadeTest`: 상세 조회 시 주문자 정보 포함 확인
  - `OrderAdminFacadeTest`: 주문 항목별 subtotal 계산 확인
  - `OrderAdminFacadeTest`: totalAmount 계산 확인
  - `OrderAdminFacadeTest`: 존재하지 않는 주문 ID 시 NOT_FOUND 예외
- [ ] 통합 테스트 작성
  - 정상 상세 조회 (주문자 정보 + 주문 항목 + 금액 확인)
  - 존재하지 않는 orderId 시 404 응답
  - LDAP 인증 미포함 시 401 응답

### Phase 4: HTTP 테스트 파일 작성
완성된 API를 IntelliJ HTTP Client로 수동 테스트할 수 있도록 .http 파일을 작성합니다.

- [ ] `.http/admin/order/list-orders.http` 생성 (필터/정렬/페이징 다양한 케이스)
- [ ] `.http/admin/order/get-order-detail.http` 생성

---

## Part 5: 테스트 시나리오

### 5.1 단위 테스트

#### LDAP 인증 필터 테스트

| 시나리오 | 입력 | 기대 결과 |
|---------|------|----------|
| 유효한 LDAP 헤더 | `X-Loopers-Ldap: loopers.admin` | 필터 통과, 다음 필터 체인 호출 |
| LDAP 헤더 누락 | 헤더 없음 | 401 Unauthorized |
| 잘못된 LDAP 값 | `X-Loopers-Ldap: invalid.value` | 401 Unauthorized |
| 어드민 경로가 아닌 요청 | `GET /api/v1/products` | 필터 미적용 (shouldNotFilter = true) |

#### OrderAdminFacade 테스트

| 시나리오 | 입력 | 기대 결과 |
|---------|------|----------|
| 목록 조회 (필터 없음) | page=0, size=20 | 전체 주문 페이징 결과 반환 |
| 상태 필터 적용 | status=ORDERED | 해당 상태의 주문만 반환 |
| 유저 필터 적용 | loginId=testuser01 | 해당 유저의 주문만 반환 |
| 상세 조회 성공 | orderId=1 | 주문 + 주문자 정보 + 항목 + 금액 반환 |
| 상세 조회 시 subtotal 계산 | price=15000, quantity=2 | subtotal=30000 |
| 상세 조회 시 totalAmount 계산 | 항목1: 30000, 항목2: 5000 | totalAmount=35000 |
| 존재하지 않는 주문 | orderId=999 | NOT_FOUND 예외 |

### 5.2 통합 테스트

#### 주문 목록 조회 API

| 시나리오 | 조건 | 기대 결과 |
|---------|------|----------|
| 전체 목록 조회 | 필터 없음, LDAP 헤더 포함 | 200 OK + 페이징된 주문 목록 |
| 상태 필터 | status=ORDERED | 200 OK + ORDERED 상태 주문만 |
| 유저 필터 | loginId=testuser01 | 200 OK + testuser01의 주문만 |
| 복합 필터 | status=ORDERED&loginId=testuser01 | 200 OK + 두 조건 모두 만족하는 주문만 |
| 주문일시 오름차순 정렬 | sort=orderedAt&direction=ASC | 200 OK + 오래된 순으로 정렬 |
| 총액 내림차순 정렬 | sort=totalAmount&direction=DESC | 200 OK + 금액 높은 순으로 정렬 |
| 존재하지 않는 유저 필터 | loginId=nonexistent | 200 OK + 빈 목록 |
| 유효하지 않은 status | status=INVALID | 400 Bad Request |
| 유효하지 않은 sort | sort=invalidField | 400 Bad Request |
| 유효하지 않은 direction | direction=INVALID | 400 Bad Request |
| LDAP 헤더 누락 | 헤더 없음 | 401 Unauthorized |
| 잘못된 LDAP 값 | X-Loopers-Ldap: wrong.value | 401 Unauthorized |
| size 최대값 초과 | size=200 | 400 Bad Request |

#### 단일 주문 상세 조회 API

| 시나리오 | 조건 | 기대 결과 |
|---------|------|----------|
| 정상 상세 조회 | 존재하는 orderId + LDAP 헤더 | 200 OK + 주문자 정보 + 주문 항목 + 금액 |
| 주문자 정보 포함 확인 | 상세 조회 응답 | orderer에 loginId, name, email 포함 (마스킹 없음) |
| 주문 항목 스냅샷 확인 | 상세 조회 응답 | items에 productName, brandName, price (주문 시점 스냅샷) 포함 |
| 항목별 소계 확인 | price=15000, quantity=2 | subtotal=30000 |
| 주문 총액 확인 | 여러 항목 | totalAmount = 모든 subtotal 합계 |
| 존재하지 않는 주문 | orderId=999 | 404 Not Found |
| LDAP 헤더 누락 | 헤더 없음 | 401 Unauthorized |

### 5.3 E2E 테스트

| 시나리오 | 흐름 | 기대 결과 |
|---------|------|----------|
| 주문 목록 → 상세 조회 | 목록에서 orderId 확인 후 상세 조회 | 목록의 orderId와 상세 조회 결과 일치 |
| 유저별 주문 확인 | 두 유저가 각각 주문 후 어드민이 loginId 필터로 조회 | 각 유저의 주문만 정확히 반환 |
| 필터 + 정렬 조합 | status=ORDERED&sort=totalAmount&direction=DESC | 조건에 맞는 결과가 올바르게 정렬되어 반환 |

---

## Part 6: 보안 고려사항

### 6.1 LDAP 인증
- 모든 어드민 API 요청은 `X-Loopers-Ldap: loopers.admin` 헤더를 검증합니다.
- LDAP 헤더 값은 대소문자를 구분합니다 (`loopers.admin` 정확히 일치).
- 인증 실패 시 에러 메시지에 유효한 LDAP 값을 노출하지 않습니다.

### 6.2 데이터 접근 범위
- 어드민 API는 대고객 API와 URL prefix(`/api-admin/v1`)로 명확히 분리합니다.
- 어드민 상세 조회에서 주문자 정보(이름, 이메일)는 마스킹 없이 원본을 표시합니다. 이는 어드민 업무에 필요한 정보이므로 의도된 동작입니다.

### 6.3 입력 검증
- 페이징 파라미터: page >= 0, 1 <= size <= 100 범위를 검증합니다.
- status 파라미터: OrderStatus enum 값과 정확히 일치하는지 검증합니다.
- sort 파라미터: 허용된 필드(`orderedAt`, `totalAmount`)만 접수합니다.
- direction 파라미터: `ASC` 또는 `DESC`만 허용합니다.

---

## Part 7: 검증 명령어

```bash
# 전체 테스트 실행
./gradlew :apps:commerce-api:test

# ktlint 검사
./gradlew :apps:commerce-api:ktlintCheck

# 빌드
./gradlew :apps:commerce-api:build

# 테스트 커버리지 리포트
./gradlew :apps:commerce-api:test jacocoTestReport
```

`.http/admin/order/list-orders.http`와 `.http/admin/order/get-order-detail.http`로 수동 API 테스트가 가능합니다.

---

## 품질 체크리스트
- [x] 상품/브랜드/좋아요/주문 등 관련 도메인과의 관계가 명시되어 있는가?
  - 주문(직접 대상), 유저(주문자 정보), 상품(스냅샷), 브랜드(스냅샷) 관계를 "관련 도메인" 섹션에 명시함
- [x] 기능 요구사항이 유저 중심("유저가 ~한다")으로 서술되어 있는가?
  - "어드민이 전체 사용자의 주문을 조회한다", "어드민이 주문 상세를 확인한다" 형태로 서술함
- [x] 인증 방식(헤더 기반)이 정확히 명시되어 있는가?
  - `X-Loopers-Ldap: loopers.admin` 헤더 기반 인증을 Part 2.1, Part 6.1에 명시함
- [x] 에러 케이스와 예외 상황이 포함되어 있는가?
  - LDAP 인증 실패(401), 유효하지 않은 파라미터(400), 존재하지 않는 주문(404) 케이스를 Part 1에 명시함
- [x] Phase별 구현 체크리스트가 TDD 워크플로우에 맞게 구성되어 있는가?
  - Phase 1~4로 분리하고 각 Phase에 단위 테스트/통합 테스트 항목을 포함함

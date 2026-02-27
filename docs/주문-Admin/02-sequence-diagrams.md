# 주문 Admin 시퀀스 다이어그램

## 개요

이 문서는 주문 Admin API의 요청 처리 흐름을 시퀀스 다이어그램으로 표현합니다.

| API | 메서드 | 경로 | 설명 |
|-----|--------|------|------|
| 주문 목록 조회 | GET | `/api-admin/v1/orders` | 전체 유저의 주문을 필터링/정렬/페이징하여 조회 |
| 주문 상세 조회 | GET | `/api-admin/v1/orders/{orderId}` | 특정 주문의 상세 내역(주문자 정보, 항목, 금액) 조회 |

**인증 방식**: 모든 어드민 API는 `X-Loopers-Ldap: loopers.admin` 헤더로 인증합니다.

---

## 1. 주문 목록 조회 - 성공 흐름

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant Filter as LdapAuthenticationFilter
    participant Controller as OrderAdminV1Controller
    participant Facade as OrderAdminFacade
    participant Service as OrderService
    participant Repository as OrderRepository
    participant DB as MySQL

    Client->>Filter: GET /api-admin/v1/orders?page=0&size=20&status=ORDERED&loginId=testuser01&sort=orderedAt&direction=DESC
    Note over Client,Filter: X-Loopers-Ldap: loopers.admin

    Filter->>Filter: X-Loopers-Ldap 헤더 값 검증 (loopers.admin 일치 확인)
    Filter->>Controller: 인증된 요청 전달

    Controller->>Facade: getOrders(page=0, size=20, status=ORDERED, loginId=testuser01, sort=orderedAt, direction=DESC)

    Facade->>Facade: status 파라미터를 OrderStatus enum으로 변환 및 검증
    Facade->>Facade: sort/direction 파라미터 유효성 검증

    Facade->>Service: findAllForAdmin(pageable, status=ORDERED, loginId=testuser01)
    Service->>Repository: findAllWithFilters(pageable, status=ORDERED, loginId=testuser01)
    Repository->>DB: SELECT o.* FROM orders o JOIN members m ON o.member_id = m.id WHERE o.status = 'ORDERED' AND m.login_id = 'testuser01' ORDER BY o.ordered_at DESC LIMIT 20 OFFSET 0
    DB-->>Repository: 주문 목록 + 총 건수
    Repository-->>Service: Page<OrderModel>
    Service-->>Facade: Page<OrderModel>

    Facade->>Facade: 각 주문의 itemCount, totalAmount 계산
    Facade->>Facade: OrderAdminInfo.OrderListItem 목록으로 변환

    Facade-->>Controller: OrderAdminInfo.OrderListPage (content, page, size, totalElements, totalPages)
    Controller->>Controller: OrderAdminV1Dto.OrderListResponse로 변환
    Controller-->>Client: ApiResponse<OrderListResponse> (200 OK)
```

### 흐름 설명

| 단계 | 책임 객체 | 수행 내용 |
|------|-----------|----------|
| 1 | Client | 쿼리 파라미터(page, size, status, loginId, sort, direction)와 LDAP 헤더를 포함하여 요청을 전송합니다. |
| 2 | LdapAuthenticationFilter | `X-Loopers-Ldap` 헤더 값이 `loopers.admin`과 일치하는지 검증합니다. |
| 3 | LdapAuthenticationFilter | 인증 통과 후 다음 필터 체인(Controller)으로 요청을 전달합니다. |
| 4 | OrderAdminV1Controller | 쿼리 파라미터를 파싱하여 Facade의 `getOrders()` 메서드를 호출합니다. |
| 5-6 | OrderAdminFacade | status 값을 OrderStatus enum으로 변환하고, sort/direction 파라미터의 유효성을 검증합니다. |
| 7-8 | OrderService/Repository | 필터 조건(status, loginId)과 정렬/페이징 조건을 적용하여 DB에서 주문 목록을 조회합니다. |
| 9-12 | DB → Repository → Service → Facade | 쿼리 결과를 `Page<OrderModel>` 형태로 반환합니다. |
| 13-14 | OrderAdminFacade | 각 주문의 항목 수와 총액을 계산하고, `OrderAdminInfo.OrderListItem` 목록으로 변환합니다. |
| 15-17 | Facade → Controller → Client | Info DTO를 Response DTO로 변환하여 `ApiResponse<OrderListResponse>`로 응답합니다. |

---

## 2. 주문 목록 조회 - 에러 흐름

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant Filter as LdapAuthenticationFilter
    participant Controller as OrderAdminV1Controller
    participant Facade as OrderAdminFacade
    participant ControllerAdvice as ApiControllerAdvice

    Client->>Filter: GET /api-admin/v1/orders
    Note over Client,Filter: X-Loopers-Ldap 헤더

    alt LDAP 헤더 누락 또는 유효하지 않은 값
        Filter->>Filter: X-Loopers-Ldap 헤더 검증 실패
        Filter-->>Client: ApiResponse(FAIL, UNAUTHORIZED, "인증이 필요합니다.") [401]
    else LDAP 인증 성공
        Filter->>Controller: 인증된 요청 전달
        Controller->>Facade: getOrders(page, size, status, loginId, sort, direction)

        alt 유효하지 않은 status 값
            Facade->>Facade: OrderStatus enum 변환 실패
            Facade-->>ControllerAdvice: CoreException(BAD_REQUEST, "유효하지 않은 주문 상태입니다.")
            ControllerAdvice-->>Client: ApiResponse(FAIL, Bad Request, "유효하지 않은 주문 상태입니다. 사용 가능한 값: [ORDERED, ...]") [400]

        else 유효하지 않은 sort 값
            Facade->>Facade: sort 필드 유효성 검증 실패
            Facade-->>ControllerAdvice: CoreException(BAD_REQUEST, "유효하지 않은 정렬 기준입니다.")
            ControllerAdvice-->>Client: ApiResponse(FAIL, Bad Request, "유효하지 않은 정렬 기준입니다. 사용 가능한 값: [orderedAt, totalAmount]") [400]

        else 유효하지 않은 direction 값
            Facade->>Facade: direction 유효성 검증 실패
            Facade-->>ControllerAdvice: CoreException(BAD_REQUEST, "유효하지 않은 정렬 방향입니다.")
            ControllerAdvice-->>Client: ApiResponse(FAIL, Bad Request, "유효하지 않은 정렬 방향입니다. 사용 가능한 값: [ASC, DESC]") [400]

        else size 최대값 초과
            Facade->>Facade: size > 100 검증 실패
            Facade-->>ControllerAdvice: CoreException(BAD_REQUEST, "페이지 크기는 최대 100입니다.")
            ControllerAdvice-->>Client: ApiResponse(FAIL, Bad Request, "페이지 크기는 최대 100입니다.") [400]

        else 정상 처리 (빈 결과 포함)
            Facade-->>Controller: OrderAdminInfo.OrderListPage (빈 content 가능)
            Controller-->>Client: ApiResponse(SUCCESS, OrderListResponse) [200]
        end
    end
```

### 에러 시나리오

| 조건 | 발생 시점 | 책임 객체 | 에러 타입 | HTTP 상태 |
|------|----------|-----------|----------|----------|
| LDAP 헤더 누락 | 필터 단계 | LdapAuthenticationFilter | UNAUTHORIZED | 401 |
| 잘못된 LDAP 값 (loopers.admin이 아님) | 필터 단계 | LdapAuthenticationFilter | UNAUTHORIZED | 401 |
| 유효하지 않은 status 값 | Facade 파라미터 검증 | OrderAdminFacade | BAD_REQUEST | 400 |
| 유효하지 않은 sort 값 | Facade 파라미터 검증 | OrderAdminFacade | BAD_REQUEST | 400 |
| 유효하지 않은 direction 값 | Facade 파라미터 검증 | OrderAdminFacade | BAD_REQUEST | 400 |
| size > 100 (최대값 초과) | Facade 파라미터 검증 | OrderAdminFacade | BAD_REQUEST | 400 |
| 존재하지 않는 loginId | 정상 처리 (에러 아님) | OrderRepository | - | 200 (빈 목록) |

---

## 3. 주문 상세 조회 - 성공 흐름

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant Filter as LdapAuthenticationFilter
    participant Controller as OrderAdminV1Controller
    participant Facade as OrderAdminFacade
    participant OrderService as OrderService
    participant MemberService as MemberService
    participant OrderRepo as OrderRepository
    participant MemberRepo as MemberRepository
    participant DB as MySQL

    Client->>Filter: GET /api-admin/v1/orders/1
    Note over Client,Filter: X-Loopers-Ldap: loopers.admin

    Filter->>Filter: X-Loopers-Ldap 헤더 값 검증 (loopers.admin 일치 확인)
    Filter->>Controller: 인증된 요청 전달

    Controller->>Facade: getOrderDetail(orderId=1)

    Facade->>OrderService: findByIdWithItems(orderId=1)
    OrderService->>OrderRepo: findByIdWithItems(orderId=1)
    OrderRepo->>DB: SELECT o.*, oi.* FROM orders o JOIN order_items oi ON o.id = oi.order_id WHERE o.id = 1
    DB-->>OrderRepo: 주문 + 주문 항목 목록
    OrderRepo-->>OrderService: OrderModel (주문 항목 포함)
    OrderService-->>Facade: OrderModel

    Facade->>MemberService: findById(memberId=order.memberId)
    MemberService->>MemberRepo: findById(memberId)
    MemberRepo->>DB: SELECT * FROM member WHERE id = ?
    DB-->>MemberRepo: 회원 정보
    MemberRepo-->>MemberService: MemberModel
    MemberService-->>Facade: MemberModel

    Facade->>Facade: 주문 항목별 subtotal 계산 (price * quantity)
    Facade->>Facade: 주문 totalAmount 계산 (모든 subtotal 합계)
    Facade->>Facade: OrderAdminInfo.OrderDetail로 변환 (주문자 정보 + 주문 항목 + 금액)

    Facade-->>Controller: OrderAdminInfo.OrderDetail
    Controller->>Controller: OrderAdminV1Dto.OrderDetailResponse로 변환
    Controller-->>Client: ApiResponse<OrderDetailResponse> (200 OK)
```

### 흐름 설명

| 단계 | 책임 객체 | 수행 내용 |
|------|-----------|----------|
| 1 | Client | orderId를 경로 변수로 포함하고 LDAP 헤더와 함께 요청을 전송합니다. |
| 2-3 | LdapAuthenticationFilter | LDAP 헤더를 검증하고 인증된 요청을 Controller로 전달합니다. |
| 4 | OrderAdminV1Controller | 경로 변수에서 orderId를 추출하여 Facade의 `getOrderDetail()` 메서드를 호출합니다. |
| 5-10 | OrderAdminFacade → OrderService → OrderRepository → DB | orderId로 주문을 조회합니다. 주문 항목(OrderItem)을 함께 조회하여 상품 스냅샷 정보를 포함합니다. |
| 11-16 | OrderAdminFacade → MemberService → MemberRepository → DB | 주문에 저장된 memberId로 주문자 정보(loginId, 이름, 이메일)를 조회합니다. 어드민 조회이므로 마스킹 없이 원본을 사용합니다. |
| 17-19 | OrderAdminFacade | 각 주문 항목의 subtotal(price * quantity)을 계산하고, 모든 subtotal을 합산하여 totalAmount를 산출합니다. 주문자 정보 + 주문 항목 + 금액 정보를 `OrderAdminInfo.OrderDetail`로 변환합니다. |
| 20-22 | Facade → Controller → Client | Info DTO를 Response DTO로 변환하여 `ApiResponse<OrderDetailResponse>`로 응답합니다. 주문자 정보(orderer), 주문 항목(items), 총액(totalAmount)이 모두 포함됩니다. |

---

## 4. 주문 상세 조회 - 에러 흐름

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant Filter as LdapAuthenticationFilter
    participant Controller as OrderAdminV1Controller
    participant Facade as OrderAdminFacade
    participant OrderService as OrderService
    participant OrderRepo as OrderRepository
    participant DB as MySQL
    participant ControllerAdvice as ApiControllerAdvice

    Client->>Filter: GET /api-admin/v1/orders/{orderId}
    Note over Client,Filter: X-Loopers-Ldap 헤더

    alt LDAP 헤더 누락 또는 유효하지 않은 값
        Filter->>Filter: X-Loopers-Ldap 헤더 검증 실패
        Filter-->>Client: ApiResponse(FAIL, UNAUTHORIZED, "인증이 필요합니다.") [401]
    else LDAP 인증 성공
        Filter->>Controller: 인증된 요청 전달
        Controller->>Facade: getOrderDetail(orderId)
        Facade->>OrderService: findByIdWithItems(orderId)
        OrderService->>OrderRepo: findByIdWithItems(orderId)
        OrderRepo->>DB: SELECT o.*, oi.* FROM orders o JOIN order_items oi ON o.id = oi.order_id WHERE o.id = ?
        DB-->>OrderRepo: 결과

        alt 주문이 존재하지 않음
            OrderRepo-->>OrderService: null
            OrderService-->>ControllerAdvice: CoreException(NOT_FOUND, "존재하지 않는 주문입니다.")
            ControllerAdvice-->>Client: ApiResponse(FAIL, Not Found, "존재하지 않는 주문입니다.") [404]
        else 주문이 존재함
            OrderRepo-->>OrderService: OrderModel
            OrderService-->>Facade: OrderModel
            Facade->>Facade: 주문자 정보 조회, 금액 계산, DTO 변환
            Facade-->>Controller: OrderAdminInfo.OrderDetail
            Controller-->>Client: ApiResponse(SUCCESS, OrderDetailResponse) [200]
        end
    end
```

### 에러 시나리오

| 조건 | 발생 시점 | 책임 객체 | 에러 타입 | HTTP 상태 |
|------|----------|-----------|----------|----------|
| LDAP 헤더 누락 | 필터 단계 | LdapAuthenticationFilter | UNAUTHORIZED | 401 |
| 잘못된 LDAP 값 (loopers.admin이 아님) | 필터 단계 | LdapAuthenticationFilter | UNAUTHORIZED | 401 |
| 존재하지 않는 orderId | Service 조회 단계 | OrderService | NOT_FOUND | 404 |
| orderId 타입 불일치 (숫자가 아님) | Controller 파라미터 바인딩 | ApiControllerAdvice | BAD_REQUEST | 400 |

---

## 품질 체크리스트
- [x] 각 participant의 책임(검증, 변환, 조회, 저장 등)이 메서드명으로 명확히 드러나는가?
  - LdapAuthenticationFilter: 헤더 검증, OrderAdminFacade: 파라미터 검증/금액 계산/DTO 변환, OrderService: 도메인 조회, OrderRepository: DB 쿼리
- [x] 여러 도메인이 관련된 경우, 각 도메인의 Service가 별도 participant로 분리되어 있는가?
  - 주문 상세 조회에서 OrderService와 MemberService를 별도 participant로 분리하여 책임 경계를 명확히 표현함
- [x] 인증 방식(헤더 기반)이 다이어그램에 정확히 반영되어 있는가?
  - LdapAuthenticationFilter에서 `X-Loopers-Ldap: loopers.admin` 헤더 검증을 명시하고, Note로 헤더 값을 표기함
- [x] 성공 흐름과 에러 흐름이 모두 포함되어 있는가?
  - 각 API별로 성공 다이어그램(1, 3번)과 에러 다이어그램(2, 4번) 총 4개를 작성함
- [x] 에러 시나리오 테이블에 발생 시점과 책임 객체가 명시되어 있는가?
  - 각 에러 흐름 다이어그램 하단에 조건, 발생 시점, 책임 객체, 에러 타입, HTTP 상태를 테이블로 정리함

## 📌 Summary

Order 도메인 전체 구현을 완료했습니다. 이 과정에서 **Facade 패턴으로 요청 흐름을 제어**하고, **도메인 서비스로 복잡한 비즈니스 로직을 분리**했으며, **인증 방식을 LDAP와 일반 사용자로 분리**하여 유연한 아키텍처를 구성했습니다.

**핵심 구현:**
- Facade 레이어에서 비즈니스 흐름을 조율하고 서비스들을 조합하여 호출
- ProductDomainService와 OrderService로 복잡한 도메인 로직을 캡슐화
- 인증 방식을 LdapAuthInterceptor (관리자)와 UserAuthInterceptor (일반 사용자)로 분리

---

## 💬 Review Points

#### 1. Facade 패턴: 요청을 받아서 비즈니스 흐름을 조율하고 서비스 호출

**배경 및 문제 상황:**

컨트롤러에서 직접 여러 서비스를 호출하면, 요청 흐름이 복잡해집니다. 예를 들어, 주문 생성 시 다음 단계가 필요합니다:

1. 상품 재고 검증 및 감소
2. 상품 정보 조회 (가격, 이름)
3. OrderItem 생성
4. Order 저장

만약 컨트롤러에서 이 모든 것을 직접 처리한다면, 컨트롤러가 너무 많은 책임을 가지게 되고, 같은 비즈니스 로직이 여러 곳에서 반복될 수 있습니다. 또한 이러한 흐름을 변경할 때마다 컨트롤러를 수정해야 합니다.

**해결 방안:**

Facade 패턴으로 이러한 비즈니스 흐름을 하나의 서비스에 위임합니다. Facade는 요청을 받아서 필요한 서비스들을 호출하고, 결과를 조합하여 반환합니다.

**구현 분석:**

##### OrderFacade.kt 코드 분석
```kotlin
@Service
@Transactional(readOnly = true)
class OrderFacade(
    private val orderService: OrderService,
    private val productService: ProductService,
) {

    @Transactional
    fun createOrder(userId: Long, orderRequest: OrderV1Dto.OrderRequest): Long {
        // 1단계: 재고 감소 (모든 상품의 재고를 한 번에 감소)
        val orderItems = orderRequest.orderItems.map { OrderItemCriteria(it.productId, it.quantity) }
        productService.decreaseProductsStock(orderItems)

        // 2단계: 상품 정보 조회 (OrderItem 생성을 위해 가격, 이름 필요)
        val createOrderItems = orderRequest.orderItems
            .map { orderItem ->
                val product = productService.getProduct(orderItem.productId)
                CreateOrderItemCommand(
                    productId = orderItem.productId,
                    productName = product.name,
                    quantity = orderItem.quantity,
                    price = product.price,
                )
            }

        // 3단계: OrderService에 위임하여 주문 생성
        return orderService.createOrder(userId, createOrderItems)
    }

    fun getOrdersByUserId(userId: Long, pageable: Pageable): Page<OrderedInfo> =
        orderService.getOrdersByUserId(userId, pageable)

    fun getOrderById(userId: Long, orderId: Long): OrderedInfo =
        orderService.getOrderById(userId, orderId).let { OrderedInfo.from(it) }
}
```

**흐름 분석:**

1. **요청 수신**: 컨트롤러에서 `OrderV1Dto.OrderRequest`를 받음
   - 요청: `{ items: [{ productId: 1, quantity: 2 }] }`

2. **1단계 - 재고 감소**: `productService.decreaseProductsStock()`
   - 목적: 모든 상품의 재고를 먼저 감소시켜 다른 요청과의 경합(race condition) 방지
   - 실패 시 Exception 발생 → Facade 레벨에서 전체 트랜잭션 롤백

3. **2단계 - 상품 정보 조회**: `productService.getProduct()`
   - 목적: 구매 당시의 가격과 상품명을 OrderItem에 저장
   - 재고 감소 후 조회하므로, 실제 존재하는 상품이 확실

4. **3단계 - 주문 저장**: `orderService.createOrder()`
   - OrderService는 순수 주문 생성 로직만 담당
   - Facade가 미리 준비한 데이터를 받아서 처리

5. **Facade 레벨 트랜잭션**:
   ```kotlin
   @Transactional
   fun createOrder(...): Long {  // 이 메서드 전체가 하나의 트랜잭션
       productService.decreaseProductsStock(...)  // 트랜잭션 1
       productService.getProduct(...)            // 트랜잭션 2 (읽기)
       orderService.createOrder(...)             // 트랜잭션 3
   }
   ```
   → 1-2-3 중 하나라도 실패하면 모두 롤백

---

##### AdminOrderFacade.kt 코드 분석
```kotlin
@Service
@Transactional(readOnly = true)
class AdminOrderFacade(
    private val orderService: OrderService,
    private val orderRepository: OrderRepository,
) {

    fun getOrders(pageable: Pageable): Page<AdminOrderInfo> =
        orderRepository.findOrders(pageable).map { AdminOrderInfo.from(it) }

    fun getOrderById(orderId: Long): AdminOrderInfo =
        orderService.getOrderByIdForAdmin(orderId).let { AdminOrderInfo.from(it) }
}
```

**흐름 분석:**

AdminOrderFacade는 **읽기 전용**이므로 훨씬 간단합니다:

1. **전체 주문 조회**: `orderRepository.findOrders()`
   - Repository에서 직접 조회 (최적화된 쿼리)
   - DTO로 변환: `AdminOrderInfo.from()`

2. **단건 조회**: `orderService.getOrderByIdForAdmin()`
   - 주문 존재 여부를 서비스 레벨에서 검증
   - 권한 검증 로직이 OrderService에 있음

**두 Facade의 차이점:**

| 항목 | OrderFacade | AdminOrderFacade |
|------|-----------|------------------|
| 트랜잭션 | @Transactional (쓰기) | @Transactional(readOnly=true) |
| 역할 | 비즈니스 흐름 조율 | 여러 방식의 조회 제공 |
| 호출 서비스 | productService + orderService | orderRepository + orderService |
| 복잡도 | 높음 (다단계 로직) | 낮음 (조회만) |

---

**고민한 점:**

- **재고 감소 시점**: OrderFacade에서 먼저 재고를 감소시키는 이유는, 주문 생성이 실패해도 재고는 유지되어야 하기 때문입니다. 만약 OrderService 내부에서 재고를 감소시켰다면, OrderService 실패 시에도 재고가 감소해야 하는 모순이 생깁니다.

- **가격 정보 저장**: OrderFacade에서 상품의 현재 가격을 OrderItem에 저장합니다. 이는 나중에 상품 가격이 변경되어도 이미 생성된 주문의 가격은 유지되어야 하기 때문입니다.

- **Facade 없이 컨트롤러에서 직접 처리하면**:
  ```kotlin
  // 반복, 변경 어려움
  @PostMapping("/orders")
  fun createOrder(...) {
      productService.decreaseProductsStock(...)
      val items = ...
      val order = orderService.createOrder(...)
      return order
  }
  ```
  이렇게 되면 컨트롤러가 비즈니스 로직까지 처리하게 되고, 다른 엔드포인트에서 같은 로직이 필요하면 중복이 발생합니다.

---

#### 2. 도메인 서비스: 복잡한 비즈니스 로직을 엔티티에서 분리하여 캡슐화

**배경 및 문제 상황:**

Product 엔티티는 여러 상태(ProductStatus)와 속성(가격, 재고, 정보)을 가집니다. 이러한 속성들을 변경할 때 특정한 규칙이나 검증이 필요할 수 있습니다:

1. 상품명과 가격을 동시에 변경 (검증 필요)
2. 상태를 ACTIVE → INACTIVE로 변경 (재고 확인 필요)
3. 재고를 업데이트하면서 상태도 자동으로 변경 (OUT_OF_STOCK 등)

이러한 로직을 모두 엔티티에 메서드로 넣으면:
- 엔티티가 너무 복잡해짐
- 상태 변경 규칙이 여러 곳에 흩어짐
- 테스트하기 어려워짐

**해결 방안:**

도메인 서비스(Domain Service)로 이러한 복잡한 비즈니스 로직을 분리합니다. 도메인 서비스는:
- 엔티티의 메서드를 조합하여 더 복잡한 비즈니스 로직 구현
- 여러 엔티티에 걸친 로직 처리
- 검증 및 상태 변경 규칙 적용

**구현 분석:**

##### ProductDomainService.kt 코드 분석
```kotlin
@Service
class ProductDomainService {

    fun updateProductInfo(
        product: Product,
        name: String,
        price: BigDecimal,
        stock: Int,
        status: ProductStatus,
    ) {
        product.updateInfo(name, price)      // Product 엔티티의 메서드 호출
        product.changeStatus(status)         // Product 엔티티의 메서드 호출
        product.updateStock(stock)           // Product 엔티티의 메서드 호출
    }
}
```

**흐름 분석:**

이 도메인 서비스는 Product 엔티티의 여러 메서드를 조합합니다:

1. **`product.updateInfo(name, price)`**
   - Product 엔티티: 상품명과 가격 변경
   - 내부에서 유효성 검증 (가격 > 0 등)

2. **`product.changeStatus(status)`**
   - Product 엔티티: 상품 상태 변경
   - ACTIVE → INACTIVE, INACTIVE → ACTIVE 등

3. **`product.updateStock(stock)`**
   - Product 엔티티: 재고 업데이트
   - 재고가 0이 되면 자동으로 OUT_OF_STOCK 상태로 변경 가능

**도메인 서비스 vs 애플리케이션 서비스:**

| 특성 | 도메인 서비스 | 애플리케이션 서비스 |
|------|-----------|------------|
| 위치 | domain/ | application/ |
| 책임 | 도메인 로직 (비즈니스 규칙) | 유스케이스 조합 |
| 입력 | 엔티티 | DTO |
| 의존성 | 엔티티만 | Repository, 다른 Service |
| 트랜잭션 | 없음 (호출처에서 관리) | @Transactional |
| 예시 | ProductDomainService | ProductService |

---

##### 더 복잡한 예시: OrderService

OrderService를 보면, 도메인 서비스와 애플리케이션 서비스의 경계를 볼 수 있습니다:

```kotlin
@Service
@Transactional(readOnly = true)
class OrderService(
    private val orderRepository: OrderRepository,
) {

    @Transactional
    fun createOrder(userId: Long, items: List<CreateOrderItemCommand>): Long {
        // 1단계: 유효성 검증 (도메인 로직)
        validateItems(items)

        // 2단계: Order 엔티티 생성 (도메인 로직)
        val order = Order.create(userId)
        val savedOrder = orderRepository.save(order)

        // 3단계: OrderItem들을 Order에 추가 (도메인 로직)
        items.forEach { itemRequest ->
            val orderItem = OrderItem.create(
                orderId = savedOrder.id,
                productId = itemRequest.productId,
                quantity = itemRequest.quantity,
                price = itemRequest.price,
                productName = itemRequest.productName,
            )
            savedOrder.addOrderItem(orderItem)  // Order의 메서드 호출
        }

        return savedOrder.id
    }

    private fun validateItems(items: List<CreateOrderItemCommand>) {
        if (items.isEmpty()) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문 항목은 최소 1개 이상이어야 합니다")
        }

        items.forEach { item ->
            if (item.quantity <= 0) {
                throw CoreException(ErrorType.BAD_REQUEST, "주문 수량은 0보다 커야 합니다")
            }
        }
    }

    fun getOrderById(userId: Long, orderId: Long): Order =
        orderRepository.findById(orderId)
            ?.takeIf { it.userId == userId }
            ?: throw CoreException(ErrorType.NOT_FOUND, "주문이 존재하지 않습니다")
}
```

**OrderService의 책임:**

1. **비즈니스 규칙 검증** (`validateItems`)
   - 주문 항목이 최소 1개 이상
   - 수량은 0보다 커야 함
   → 도메인 로직

2. **엔티티 생성 및 조합** (`Order.create()`, `addOrderItem()`)
   - Order 생성
   - OrderItem을 Order에 추가
   → 도메인 로직 + 이벤트 발행

3. **Repository 접근** (`orderRepository.save()`, `findById()`)
   - 영속성 관리
   → 애플리케이션 로직

4. **권한 검증** (`takeIf { it.userId == userId }`)
   - 사용자가 자신의 주문만 조회 가능
   → 애플리케이션 로직

---

**도메인 서비스가 필요한 시점:**

1. **여러 엔티티에 걸친 로직**
   ```kotlin
   // 도메인 서비스 필요
   fun completeOrder(order: Order, payment: Payment) {
       order.changeStatus(PAID)
       payment.markAsCompleted()
       // 더 복잡한 로직...
   }
   ```

2. **엔티티에 맞지 않는 로직**
   ```kotlin
   // 도메인 서비스 필요
   fun calculateDiscount(order: Order, promo: PromoCode) {
       val discount = promo.calculateDiscount(order.getTotalPrice())
       order.applyDiscount(discount)
   }
   ```

3. **정책이나 규칙 변경이 빈번한 경우**
   ```kotlin
   // 도메인 서비스로 분리하면 변경이 용이
   fun validateOrderCreation(order: Order) {
       // 주문 생성 규칙: 최소 금액, 시간 제한 등
   }
   ```

---

**고민한 점:**

- **도메인 서비스 vs 엔티티 메서드**: ProductDomainService.updateProductInfo()는 결국 Product의 메서드들을 순서대로 호출합니다. 이를 필요로 하는 이유는 "특정 순서로 변경해야 함"이기 때문입니다. 예를 들어, 재고가 0이 되었을 때 상태를 자동으로 변경해야 한다면, 이를 도메인 서비스에서 관리할 수 있습니다.

- **도메인 서비스의 책임 범위**: 현재 ProductDomainService는 간단하지만, 나중에 더 복잡해질 수 있습니다. 예를 들어, 상품 가격 변경 시 주문 목록을 갱신한다거나, 상품 상태 변경 시 캐시를 무효화해야 한다면, 이를 도메인 서비스에서 처리할 수 있습니다.

- **도메인 서비스와 애플리케이션 서비스의 경계**: OrderService는 애플리케이션 서비스이면서도 도메인 로직(createOrder, validateItems)도 처리합니다. 나중에 더 복잡해지면, OrderDomainService로 분리할 수 있습니다:
  ```kotlin
  // 도메인 서비스
  class OrderDomainService {
      fun createOrder(userId: Long, items: List<OrderItemCommand>): Order { ... }
      private fun validateItems(items: List<OrderItemCommand>) { ... }
  }

  // 애플리케이션 서비스
  class OrderService(
      private val orderDomainService: OrderDomainService,
      private val orderRepository: OrderRepository,
  ) {
      fun createOrder(userId: Long, items: List<OrderItemCommand>): Long {
          val order = orderDomainService.createOrder(userId, items)
          return orderRepository.save(order).id
      }
  }
  ```

---

#### 3. 인증 분리: LDAP (관리자) vs User (일반 사용자) Interceptor

**배경 및 문제 상황:**

기존 Spring Security로는 모든 엔드포인트에 동일한 인증 방식을 적용했습니다. 하지만 실제 시스템에서는:

1. **관리자** (`/api-admin/**`)
   - LDAP 기반 인증 (기업 디렉토리 서비스)
   - 역할(role) 기반 접근 제어
   - 별도의 비밀번호 정책

2. **일반 사용자** (`/api/**`)
   - 사용자가 회원가입한 로컬 계정
   - 로그인ID + 비밀번호 기반
   - 애플리케이션 DB에서 검증

이 두 가지를 하나의 SecurityConfig로 처리하기는 어렵습니다.

**해결 방안:**

Interceptor 기반으로 경로별로 다른 인증 방식을 적용합니다.

**구현 분석:**

##### WebConfig.kt - 경로별 Interceptor 등록
```kotlin
@Configuration
class WebConfig(
    private val userRepository: UserRepository,
) : WebMvcConfigurer {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun userAuthInterceptor(userRepository: UserRepository, passwordEncoder: PasswordEncoder): UserAuthInterceptor {
        return UserAuthInterceptor(userRepository, passwordEncoder)
    }

    @Bean
    fun ldapAuthInterceptor(): LdapAuthInterceptor {
        return LdapAuthInterceptor()
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        // 일반 API 인증: 사용자 정보, 주문 관련, 상품 좋아요
        registry.addInterceptor(userAuthInterceptor(userRepository, passwordEncoder()))
            .addPathPatterns(
                "/api/*/users/**",
                "/api/*/orders/**",
                "/api/*/products/**/likes",
            )

        // Admin API 인증: LDAP 역할 검증
        registry.addInterceptor(ldapAuthInterceptor())
            .addPathPatterns("/api-admin/**")
    }
}
```

**흐름 분석:**

1. **경로 매칭**:
   - `/api/*/users/**` → UserAuthInterceptor
   - `/api/*/orders/**` → UserAuthInterceptor
   - `/api-admin/**` → LdapAuthInterceptor

2. **Interceptor 우선순위**:
   - 등록 순서대로 실행
   - 하나의 요청이 여러 Interceptor를 거칠 수 있음

---

##### LdapAuthInterceptor.kt - 관리자 인증 분석
```kotlin
class LdapAuthInterceptor : HandlerInterceptor {

    companion object {
        private const val HEADER_LDAP_USERNAME = "X-LDAP-Username"
        private const val HEADER_LDAP_ROLE = "X-LDAP-Role"
    }

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        // 1단계: LDAP 헤더 추출
        val username = request.getHeader(HEADER_LDAP_USERNAME)
        val roleHeader = request.getHeader(HEADER_LDAP_ROLE)

        // 2단계: 헤더 유효성 검증
        if (username.isNullOrBlank() || roleHeader.isNullOrBlank()) {
            return sendUnauthorizedResponse(response, "인증이 필요합니다.")
        }

        // 3단계: 역할 검증 (enum으로 정의된 역할만 허용)
        val ldapRole = runCatching { LdapRole.valueOf(roleHeader) }.getOrNull()
        if (ldapRole == null) {
            return sendUnauthorizedResponse(response, "유효하지 않은 역할입니다.")
        }

        return true
    }

    private fun sendUnauthorizedResponse(response: HttpServletResponse, message: String): Boolean {
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = "application/json;charset=UTF-8"
        val errorResponse = ApiResponse.fail(
            errorCode = ErrorType.UNAUTHORIZED.code,
            errorMessage = message,
        )
        response.writer.write(ObjectMapper().writeValueAsString(errorResponse))
        return false
    }
}
```

**특징:**

1. **헤더 기반 인증**
   - 클라이언트가 `X-LDAP-Username`, `X-LDAP-Role` 헤더를 포함해야 함
   - 실제 LDAP 서버 검증은 프록시나 게이트웨이에서 수행 (마이크로서비스 환경)

2. **역할 검증**
   ```kotlin
   val ldapRole = LdapRole.valueOf(roleHeader)  // ADMIN, MANAGER, USER 등
   ```
   - LdapRole enum으로 정의된 값만 허용
   - 잘못된 역할은 즉시 거부

3. **간단한 구조**
   - 실제 LDAP 연결 없이 헤더만 검증
   - 마이크로서비스 아키텍처에서는 API Gateway에서 LDAP 검증 후 헤더 추가

---

##### UserAuthInterceptor.kt - 일반 사용자 인증 분석
```kotlin
class UserAuthInterceptor(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) : HandlerInterceptor {

    companion object {
        const val USER_ID_ATTRIBUTE = "userId"
        private const val HEADER_LOGIN_ID = "X-Loopers-LoginId"
        private const val HEADER_LOGIN_PW = "X-Loopers-LoginPw"
    }

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        // 1단계: 회원가입(POST /api/*/users)은 인증 제외
        if (request.method == "POST" && request.requestURI.matches(Regex(".*/api/[^/]+/users$"))) {
            return true
        }

        // 2단계: 로그인ID와 비밀번호 헤더 추출
        val loginId = request.getHeader(HEADER_LOGIN_ID)
        val loginPw = request.getHeader(HEADER_LOGIN_PW)

        // 3단계: 헤더 유효성 검증
        if (loginId.isNullOrBlank() || loginPw.isNullOrBlank()) {
            return sendUnauthorizedResponse(response, "인증이 필요합니다.")
        }

        // 4단계: DB에서 사용자 조회
        val user = userRepository.findByLoginId(LoginId.of(loginId))

        // 5단계: 비밀번호 검증 (BCrypt 해시 비교)
        if (user == null || !passwordEncoder.matches(loginPw, user.password.value)) {
            return sendUnauthorizedResponse(response, "로그인 아이디 또는 패스워드가 잘못되었습니다.")
        }

        // 6단계: 성공 시 userId를 request 속성에 저장 (컨트롤러에서 접근 가능)
        request.setAttribute(USER_ID_ATTRIBUTE, user.id)
        return true
    }

    private fun sendUnauthorizedResponse(response: HttpServletResponse, message: String): Boolean {
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = "application/json;charset=UTF-8"
        val errorResponse = ApiResponse.fail(
            errorCode = ErrorType.UNAUTHORIZED.code,
            errorMessage = message,
        )
        response.writer.write(ObjectMapper().writeValueAsString(errorResponse))
        return false
    }
}
```

**특징:**

1. **회원가입 제외**
   ```kotlin
   if (request.method == "POST" && request.requestURI.matches(Regex(".*/api/[^/]+/users$"))) {
       return true  // 인증 제외
   }
   ```
   - POST /api/v1/users (회원가입)은 인증 불필요
   - 다른 사용자 API는 모두 인증 필수

2. **DB 조회 및 비밀번호 검증**
   ```kotlin
   val user = userRepository.findByLoginId(LoginId.of(loginId))
   if (user == null || !passwordEncoder.matches(loginPw, user.password.value)) {
       return sendUnauthorizedResponse(...)
   }
   ```
   - 로컬 DB에서 사용자 검색
   - BCryptPasswordEncoder로 비밀번호 검증 (해시 비교)

3. **userId를 request에 저장**
   ```kotlin
   request.setAttribute(USER_ID_ATTRIBUTE, user.id)
   ```
   - 컨트롤러에서 `request.getAttribute("userId")`로 접근 가능
   - 현재 요청의 사용자 ID를 식별

---

##### 두 Interceptor의 비교

| 항목 | LdapAuthInterceptor | UserAuthInterceptor |
|------|-----------------|----------------|
| 용도 | 관리자 인증 | 일반 사용자 인증 |
| 인증 방식 | 헤더 검증 (LDAP) | DB 조회 + 비밀번호 검증 |
| 데이터 소스 | LDAP 헤더 | 로컬 DB |
| 역할/권한 | 역할 검증 (LdapRole) | 사용자 ID만 추출 |
| 성공 시 처리 | 아무것도 안 함 | userId를 request에 저장 |
| 회원가입 제외 | 없음 | POST /api/*/users 제외 |
| 빠르기 | 매우 빠름 (헤더만 검증) | 느림 (DB 조회) |

---

**인증 요청 흐름:**

```
[요청 수신]
    ↓
[경로 분석]
    ↓
경로가 /api-admin/** → LdapAuthInterceptor 실행
경로가 /api/**/** → UserAuthInterceptor 실행
    ↓
[인증 성공]
    ↓
[컨트롤러 실행]
```

**예시:**

1. **관리자 API 요청**
   ```http
   GET /api-admin/v1/orders
   X-LDAP-Username: admin@company.com
   X-LDAP-Role: ADMIN
   ```
   → LdapAuthInterceptor: 역할이 LdapRole enum에 있으면 통과

2. **일반 사용자 주문 조회**
   ```http
   GET /api/v1/orders
   X-Loopers-LoginId: user123
   X-Loopers-LoginPw: password123
   ```
   → UserAuthInterceptor: DB에서 user123 검색 → 비밀번호 검증 → userId 저장

3. **회원가입 (인증 제외)**
   ```http
   POST /api/v1/users
   (헤더 불필요)
   ```
   → UserAuthInterceptor: 정규식 매칭 → 인증 스킵

---

**고민한 점:**

- **헤더 기반 인증의 보안**: HTTP 헤더는 평문이므로, 실제 환경에서는 HTTPS를 필수로 사용해야 합니다. 또한 회원가입 시 비밀번호를 헤더로 전달하는 것은 위험하므로, 회원가입은 POST 본문으로 처리하고 인증은 헤더로 처리하는 것이 좋습니다.

- **LdapAuthInterceptor의 실제 LDAP 검증**: 현재는 헤더만 검증하고 실제 LDAP 서버에 요청하지 않습니다. 마이크로서비스 환경에서는 API Gateway나 Ingress Controller가 LDAP 검증을 수행하고, 서비스는 신뢰할 수 있는 헤더만 검증하는 것이 일반적입니다. 만약 LDAP 서버에 직접 연결해야 한다면:
  ```kotlin
  class LdapAuthInterceptor(
      private val ldapTemplate: LdapTemplate,  // Spring LDAP
  ) : HandlerInterceptor {
      override fun preHandle(...): Boolean {
          val dn = LdapNameBuilder.root().add("cn", username).build()
          val isAuthenticated = ldapTemplate.authenticate(dn, password)
          return if (isAuthenticated) true else sendUnauthorized(response)
      }
  }
  ```

- **UserAuthInterceptor의 매번 DB 조회**: 모든 요청마다 DB에 조회하므로, 성능상 이슈가 있을 수 있습니다. 개선 방안:
  1. **JWT 토큰 사용**: 토큰에 userId를 포함시켜 DB 조회 제거
  2. **Redis 캐시**: 사용자 정보를 캐시하여 DB 조회 빈도 감소
  3. **Connection Pool 설정**: DB 연결을 미리 풀에 준비하여 조회 속도 향상

- **request.setAttribute() vs SecurityContext**: 현재는 request 속성에 userId를 저장하지만, Spring Security 환경에서는 SecurityContext에 저장하는 것이 표준입니다. 만약 Spring Security를 사용한다면:
  ```kotlin
  val authentication = UsernamePasswordAuthenticationToken(user, null, user.authorities)
  SecurityContextHolder.getContext().authentication = authentication
  ```

---

## ✅ Checklist

### Facade 패턴 구현 (2/2)
- [x] **OrderFacade**: 주문 생성 비즈니스 흐름 조율
  - `apps/commerce-api/src/main/kotlin/com/loopers/application/api/order/OrderFacade.kt`
  - 재고 감소 → 상품 정보 조회 → 주문 생성

- [x] **AdminOrderFacade**: 관리자 주문 조회 제공
  - `apps/commerce-api/src/main/kotlin/com/loopers/application/admin/order/AdminOrderFacade.kt`
  - 읽기 전용 Facade

### 도메인 서비스 분리 (2/2)
- [x] **ProductDomainService**: 상품 정보 변경 로직 캡슐화
  - `apps/commerce-api/src/main/kotlin/com/loopers/domain/product/ProductDomainService.kt`

- [x] **OrderService**: 주문 생성 및 조회 비즈니스 로직
  - `apps/commerce-api/src/main/kotlin/com/loopers/domain/order/OrderService.kt`
  - 검증, 엔티티 생성, Repository 접근

### 인증 분리 (3/3)
- [x] **LdapAuthInterceptor**: 관리자 인증 (LDAP 기반)
  - `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/interceptor/LdapAuthInterceptor.kt`
  - 헤더 검증 및 역할 확인

- [x] **UserAuthInterceptor**: 일반 사용자 인증 (로컬 DB)
  - `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/interceptor/UserAuthInterceptor.kt`
  - DB 조회 및 비밀번호 검증

- [x] **WebConfig**: Interceptor 경로 등록
  - `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/config/WebConfig.kt`
  - `/api-admin/**` → LdapAuthInterceptor
  - `/api/**` → UserAuthInterceptor

## 📎 References

- **Order 도메인 설계**: `docs/plans/2026-02-19-order-domain-design.md`
- **Order 도메인 구현**: `docs/plans/2026-02-19-order-domain-implementation.md`
- **Admin Order API 구현**: `docs/plans/2026-02-20-admin-order-controller-implementation.md`

---

## 🔄 다음 단계

1. **JWT 토큰 기반 인증** (UserAuthInterceptor 개선)
   - 매번 DB 조회 대신 JWT 토큰 검증
   - Refresh Token으로 토큰 갱신

2. **실제 LDAP 서버 연결** (LdapAuthInterceptor 개선)
   - Spring LDAP으로 실제 LDAP 검증
   - 사용자 역할 및 그룹 검증

3. **권한 기반 접근 제어 (RBAC)**
   - 관리자 API마다 필요한 역할 정의
   - 부재(Absent) 권한 검증

4. **감사 로그 (Audit Log)**
   - 모든 관리자 API 호출 기록
   - 사용자 접근 실패 기록

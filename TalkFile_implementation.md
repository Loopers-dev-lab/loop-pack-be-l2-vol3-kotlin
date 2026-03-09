## 📌 Summary

- **배경**: 회원 기능 구현 이후, 이커머스 플랫폼의 핵심 도메인 기능(상품, 브랜드, 주문, 좋아요, ADMIN)이 부재한 상태
- **목표**: Product, Brand, Like, Order, Admin 도메인 전체 구현을 통해 기본적인 이커머스 기능 완성
- **결과**: 도메인 엔티티, 서비스, Repository, API 스펙 및 컨트롤러, 포괄적인 단위/통합/E2E 테스트 완성

---

## 💬 Review Points

#### 1. Facade 패턴: 비즈니스 흐름 조율과 서비스 조합

**배경 및 문제 상황:**

주문 생성은 여러 단계를 거칩니다:
1. 재고 검증 및 감소
2. OrderItem 생성
3. Order 저장

하나의 서비스에서 수행하게되면 서비스가 너무 많은 책임을 가지게 됩니다.

**해결 방안:**

Facade 패턴으로 이러한 비즈니스 흐름을 하나의 서비스에 위임합니다. Facade는 여러 서비스를 조합하여 호출하고, 결과를 조합하여 반환합니다.

**구현 세부사항:**

OrderFacade는 다음과 같이 세 단계의 비즈니스 흐름을 관리합니다:

1. **재고 감소 (ProductService 호출)**
   - 모든 상품의 재고를 먼저 감소
   - 실패 시 여기서 즉시 Exception 발생 → 트랜잭션 롤백

2. **상품 정보 조회 (ProductService 호출)**
   - 각 상품의 현재 가격, 이름 조회
   - 구매 당시의 정보를 OrderItem에 저장

3. **주문 생성 (OrderService 호출)**
   - OrderService는 순수 주문 생성 로직만 담당
   - Facade가 준비한 데이터를 받아서 처리

**관련 코드:**

```kotlin
// OrderFacade.kt - 쓰기 작업의 비즈니스 흐름 조율
@Service
@Transactional(readOnly = true)
class OrderFacade(
    private val orderService: OrderService,
    private val productService: ProductService,
) {

    @Transactional  // 전체 메서드가 하나의 트랜잭션
    fun createOrder(userId: Long, orderRequest: OrderV1Dto.OrderRequest): Long {
        // 1단계: 재고 감소
        val orderItems = orderRequest.orderItems.map { OrderItemCriteria(it.productId, it.quantity) }
        productService.decreaseProductsStock(orderItems)

        // 2단계: 상품 정보 조회
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

        // 3단계: 주문 생성 (OrderService에 위임)
        return orderService.createOrder(userId, createOrderItems)
    }

    fun getOrdersByUserId(userId: Long, pageable: Pageable): Page<OrderedInfo> =
        orderService.getOrdersByUserId(userId, pageable)

    fun getOrderById(userId: Long, orderId: Long): OrderedInfo =
        orderService.getOrderById(userId, orderId).let { OrderedInfo.from(it) }
}
```

**고민한 점:**

- **재고 감소의 시점**: OrderFacade에서 가장 먼저 재고를 감소시키는 이유는, OrderService 실패 시 재고 감소가 롤백되기 위함입니다. 만약 OrderService 내부에서 재고를 감소시켰다면, OrderService 실패 시에도 재고가 감소해야 하는 논리적 모순이 발생합니다.

- **가격 정보의 저장**: Facade에서 상품의 현재 가격을 OrderItem에 저장합니다. 이는 나중에 상품 가격이 변경되어도 이미 생성된 주문의 가격은 변하지 않아야 하기 때문입니다.

- **Facade 없이 컨트롤러에서 직접 처리**하면, 같은 비즈니스 흐름이 여러 컨트롤러에서 반복되고, 흐름 변경 시 모든 곳을 수정해야 합니다.

---

#### 2. 도메인 서비스: 복잡한 비즈니스 로직을 엔티티에서 분리

**배경 및 문제 상황:**

Product 엔티티는 여러 상태(ProductStatus)와 속성(가격, 재고, 정보)을 가집니다. 이러한 속성들을 변경할 때 특정한 규칙이나 순서가 필요할 수 있습니다:

1. 상품명과 가격을 동시에 변경 (유효성 검증 필요)
2. 상태를 변경 (재고와 상태의 일관성 필요)
3. 재고를 업데이트하면서 상태도 변경 (OUT_OF_STOCK 등)

이러한 로직을 모두 엔티티에 메서드로 넣으면:
- 엔티티가 너무 복잡해짐
- 상태 변경 규칙이 여러 곳에 흩어짐
- 테스트하기 어려워짐

**해결 방안:**

도메인 서비스(Domain Service)로 이러한 복잡한 비즈니스 로직을 분리합니다. 도메인 서비스는:
- 엔티티의 메서드를 조합하여 복잡한 비즈니스 로직 구현
- 여러 엔티티에 걸친 로직 처리
- 검증 및 상태 변경 규칙 적용

**구현 세부사항:**

ProductDomainService는 Product 엔티티의 여러 메서드를 조합하여 상품 정보 전체를 업데이트합니다:

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
        // 1단계: 상품명과 가격 변경 (내부에서 유효성 검증)
        product.updateInfo(name, price)

        // 2단계: 상품 상태 변경
        product.changeStatus(status)

        // 3단계: 재고 업데이트
        product.updateStock(stock)
    }
}
```

**고민한 점:**

- **도메인 서비스로 분리**: ProductDomainService.updateProductInfo()는 결국 Product의 메서드들을 순서대로 호출합니다. 이것이 필요한 이유는 "특정 순서로 변경해야 함"이기 때문입니다. 예를 들어, 재고가 0이 되었을 때 상태를 자동으로 변경해야 한다면, 이를 도메인 서비스에서 관리할 수 있습니다.


---

#### 3. 인증 분리: LDAP (관리자) vs User (일반 사용자) Interceptor

**배경 및 문제 상황:**

기존 Spring Security로는 모든 엔드포인트에 동일한 인증 방식을 적용었습니다.
하지만 인증 방식이 다양해면서 복잡도가 올라가는 것을 느꼈습니다.

1. **관리자** (`/api-admin/**`)
   - LDAP 기반 인증
   - 역할(role) 기반 접근 제어

2. **일반 사용자** (`/api/**`)
   - 사용자가 회원가입한 계정
   - 로그인ID + 비밀번호 기반

으로 Interceptor와 경로 설정을 통해 단수화 하였습니다.

**해결 방안:**

Interceptor 기반으로 경로별로 다른 인증 방식을 적용합니다. WebConfig에서 경로 패턴에 따라 적절한 Interceptor를 등록합니다.

**구현 세부사항:**

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

##### LdapAuthInterceptor.kt - 관리자 인증

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

##### UserAuthInterceptor.kt - 일반 사용자 인증

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

## ✅ Checklist

### 호출 구조 통일
- [x] **OrderFacade**: 주문 생성 비즈니스 흐름 조율
  - `apps/commerce-api/src/main/kotlin/com/loopers/application/api/order/OrderFacade.kt`
  - 재고 감소 → 상품 정보 조회 → 주문 생성 (3단계)

- [x] **AdminOrderFacade**: 관리자 주문 조회
  - `apps/commerce-api/src/main/kotlin/com/loopers/application/admin/order/AdminOrderFacade.kt`
  - 단순 조회도 Facade → Service 단계로 호출 단계 통일

### 도메인 서비스 분리
- [x] **ProductDomainService**: 상품 정보 변경 로직 캡슐화
  - `apps/commerce-api/src/main/kotlin/com/loopers/domain/product/ProductDomainService.kt`
  - updateProductInfo() - 상품명, 가격, 재고, 상태를 조합하여 변경

### 인증 분리
- [x] **WebConfig**: Interceptor 경로 등록
  - `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/config/WebConfig.kt`
  - `/api/*/users/**`, `/api/*/orders/**`, `/api/*/products/**/likes` → UserAuthInterceptor
  - `/api-admin/**` → LdapAuthInterceptor

- [x] **LdapAuthInterceptor**: 관리자 인증 (LDAP)
  - `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/interceptor/LdapAuthInterceptor.kt`
  - 헤더: X-LDAP-Username, X-LDAP-Role
  - 역할 검증 (LdapRole enum)

- [x] **UserAuthInterceptor**: 일반 사용자 인증
  - `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/interceptor/UserAuthInterceptor.kt`
  - 헤더: X-Loopers-LoginId, X-Loopers-LoginPw
  - DB 조회 + BCrypt 비밀번호 검증
  - userId를 request 속성에 저장
  - 회원가입 제외 (POST /api/*/users)

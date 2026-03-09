## 📌 Summary

Order 도메인 전체 구현과 Admin 주문 관리 API, 사용자 주문 API를 완성했습니다. 또한 LDAP 기반 인증 시스템으로 마이그레이션하고, 보안 설정을 인터셉터 기반으로 전환했습니다.

**주요 구현 내용:**
- **Order 도메인**: Order, OrderItem 엔티티, OrderService, OrderRepository 구현 (상태 관리: PENDING → PAID → SHIPPED → DELIVERED → RETURNED)
- **Admin Order API**: 주문 목록 조회 (페이지네이션, 정렬), 주문 상세 조회, AdminOrderFacade를 통한 쿼리 최적화
- **User Order API**: 사용자 주문 생성, 주문 목록 조회, 주문 상세 조회
- **인증 시스템 마이그레이션**: SecurityConfig 및 PasswordEncoderConfig 제거, LDAP 기반 LdapAuthInterceptor + 일반 사용자용 UserAuthInterceptor 추가
- **테스트 확대**: OrderServiceTest, AdminOrderV1ControllerE2ETest, OrderV1ApiE2ETest, ProductLikeV1ControllerE2ETest, LdapAuthInterceptorTest 추가

## 💬 Review Points

#### 1. Order 도메인 설계: Aggregate 패턴과 엔티티 강결합

**배경 및 문제 상황:**
주문 시스템에서는 Order와 OrderItem이 강결합되어 있으며, Order가 aggregate root 역할을 하면서 OrderItem을 완전히 관리해야 합니다. 만약 OrderItem이 독립적으로 생성되거나 수정될 수 있다면, 주문의 일관성이 깨질 수 있습니다. 또한 OrderItem은 Product를 직접 참조하지 않고 상품 ID와 구매 시점의 가격을 저장하기로 결정했는데, 이는 상품 정보 변경 후에도 주문 데이터가 일관성을 유지하도록 하기 위함입니다.

**해결 방안:**
Aggregate 패턴을 적용하여 Order가 OrderItem의 생명주기를 완전히 관리하도록 설계했습니다. OrderItem은 Order를 통해서만 생성 및 수정될 수 있으며, 독립적인 리포지토리를 가지지 않습니다. 이렇게 하면 Order의 트랜잭션이 성공하면 OrderItem도 함께 저장되고, 실패하면 함께 롤백되어 데이터 일관성이 보장됩니다.

**구현 세부사항:**

1. **Order 엔티티**:
   - OneToMany (단방향) 관계로 orderItems 소유
   - `addOrderItem(orderItem: OrderItem)` 메서드로만 아이템 추가 가능
   - `getTotalPrice()` 메서드로 자동 계산

2. **OrderItem 엔티티**:
   - productId, quantity, price, productName을 저장 (Product 직접 참조 X)
   - Order를 통해서만 생성 가능
   - 구매 당시의 상품명과 가격을 저장하여 상품 정보 변경 후에도 주문 데이터 일관성 유지

**관련 코드:**
```kotlin
// Order.kt - Aggregate Root
@Entity
@Table(name = "orders")
class Order(
    @ManyToOne(fetch = FetchType.LAZY)
    val user: User,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "order_id")
    val orderItems: MutableList<OrderItem> = mutableListOf(),

    @Enumerated(EnumType.STRING)
    val status: OrderStatus = OrderStatus.PENDING,
) : BaseEntity() {

    fun addOrderItem(orderItem: OrderItem) {
        orderItems.add(orderItem)
    }

    fun getTotalPrice(): BigDecimal =
        orderItems.sumOf { it.getSubtotal() }
}

// OrderItem.kt
@Entity
@Table(name = "order_items")
class OrderItem(
    val productId: Long,
    val quantity: Int,
    val price: BigDecimal,
    val productName: String,
) : BaseEntity() {

    fun getSubtotal(): BigDecimal = price * quantity.toBigDecimal()
}
```

**고민한 점:**
- OrderItem에서 Product를 직접 참조하지 않기로 결정했습니다. 이는 상품 정보가 변경되더라도 이미 생성된 주문의 가격과 상품명이 변하지 않도록 하기 위함입니다. 만약 Product를 직접 참조했다면, 상품이 삭제되거나 정보가 변경되었을 때 주문 조회 시 일관성 문제가 발생할 수 있습니다.

- OrderItem에 Order의 역참조(양방향)를 두지 않았습니다. 이는 OrderItem의 책임을 최소화하고, Order를 통해서만 접근하도록 제약을 두어 무결성을 보장하기 위함입니다. 하지만 나중에 OrderItem 단독으로 조회하거나 수정해야 한다면, OrderItem 리포지토리를 추가하고 양방향 관계로 변경할 수 있습니다.

- Order와 OrderItem의 cascade 설정은 `CascadeType.ALL`과 `orphanRemoval = true`로 설정했습니다. 이는 Order가 삭제되거나 OrderItem이 리스트에서 제거되면 자동으로 OrderItem도 삭제되도록 하기 위함입니다.

---

#### 2. 주문 상태 관리와 상태 전이의 유효성 검증

**배경 및 문제 상황:**
주문은 PENDING → PAID → SHIPPED → DELIVERED → RETURNED 등의 상태를 거쳐야 합니다. 하지만 만약 현재 상태가 PENDING인 상태에서 DELIVERED로 직접 전이하려고 한다면, 이는 비즈니스 규칙을 위반하게 됩니다. 따라서 상태 전이의 유효성을 검증해야 합니다.

**해결 방안:**
현재는 `OrderStatus`를 enum으로 정의하고, `changeStatus()` 메서드에서 단순히 상태를 변경하도록 구현했습니다. 하지만 나중에 상태 전이의 유효성을 검증해야 한다면, 각 상태별로 다음 가능한 상태를 정의하는 규칙을 추가할 수 있습니다.

**구현 세부사항:**

```kotlin
// OrderStatus.kt
enum class OrderStatus {
    PENDING,    // 주문 대기
    PAID,       // 결제 완료
    SHIPPED,    // 배송 중
    DELIVERED,  // 배송 완료
    RETURNED,   // 반품
}

// Order.kt
fun changeStatus(newStatus: OrderStatus) {
    this.status = newStatus
}
```

**고민한 점:**
- 현재는 모든 상태 전이가 가능하도록 구현했습니다. 나중에 상태 전이 규칙을 강화해야 한다면, 다음과 같이 개선할 수 있습니다:
  ```kotlin
  fun changeStatus(newStatus: OrderStatus) {
      val validTransitions = mapOf(
          PENDING to listOf(PAID, RETURNED),
          PAID to listOf(SHIPPED, RETURNED),
          SHIPPED to listOf(DELIVERED, RETURNED),
          DELIVERED to listOf(RETURNED),
          RETURNED to emptyList(),
      )

      require(validTransitions[this.status]?.contains(newStatus) == true) {
          "$status -> $newStatus는 유효하지 않은 상태 전이입니다"
      }

      this.status = newStatus
  }
  ```

- 현재는 결제 기능이 미포함되어 있으므로, 모든 주문이 PENDING 상태로 생성됩니다. 나중에 Payment 도메인이 추가되면, 결제 완료 시 자동으로 PAID 상태로 변경되는 로직을 추가할 수 있습니다.

---

#### 3. Admin Order API와 User Order API의 쿼리 최적화

**배경 및 문제 상황:**
Admin 주문 관리 API와 일반 사용자의 주문 API는 다르게 쿼리를 해야 합니다. Admin은 전체 주문을 조회할 수 있고, 사용자는 자신의 주문만 조회할 수 있습니다. 또한 Admin API는 정렬 옵션(최신순, 가격순 등)을 지원하면서도 페이지네이션을 효율적으로 처리해야 합니다.

**해결 방안:**
Facade 패턴을 사용하여 비즈니스 로직을 분리했습니다. `AdminOrderFacade`는 전체 주문을 조회하는 로직을 제공하고, 사용자 API는 `OrderFacade`를 통해 특정 사용자의 주문만 조회합니다.

**구현 세부사항:**

1. **AdminOrderFacade**: 관리자 전용 쿼리
   - 전체 주문 목록 조회 (Pageable 지원)
   - 정렬 옵션: RECENT (최신순), PRICE_DESC (가격 높은순), PRICE_ASC (가격 낮은순)
   - 동적 쿼리로 구현하여 필요한 필드만 조회

2. **OrderFacade**: 사용자용 쿼리
   - 특정 사용자의 주문만 조회
   - 페이지네이션 지원
   - 주문 생성, 조회 기능

**관련 코드:**
```kotlin
// AdminOrderFacade.kt
@Service
class AdminOrderFacade(
    private val orderService: OrderService,
) {
    fun getOrders(pageable: Pageable, sortOption: AdminOrderSortOption?): Page<AdminOrderInfo> {
        return orderService.getAdminOrders(pageable, sortOption)
    }

    fun getOrderDetail(orderId: Long): AdminOrderInfo {
        return orderService.getOrderDetail(orderId)
    }
}

// OrderFacade.kt
@Service
class OrderFacade(
    private val orderService: OrderService,
) {
    fun createOrder(userId: Long, request: CreateOrderRequest): OrderedInfo {
        // 주문 생성 로직
    }

    fun getUserOrders(userId: Long, pageable: Pageable): Page<OrderItemInfo> {
        return orderService.getUserOrders(userId, pageable)
    }
}
```

**고민한 점:**
- Admin API와 User API의 DTO가 다릅니다. Admin은 `AdminOrderInfo`를 반환하고, 사용자는 `OrderedInfo`를 반환합니다. 이는 Admin에는 추가 정보(관리자용 필드)가 있지만, 일반 사용자는 자신의 주문 정보만 필요하기 때문입니다.

- 정렬 옵션은 Enum으로 구현하여 유효한 옵션만 허용합니다:
  ```kotlin
  enum class AdminOrderSortOption {
      RECENT,     // 최신순 (기본)
      PRICE_DESC, // 가격 높은순
      PRICE_ASC,  // 가격 낮은순
  }
  ```

---

#### 4. 인증 시스템 마이그레이션: SecurityConfig에서 Interceptor 기반으로 전환

**배경 및 문제 상황:**
기존에는 Spring Security의 `SecurityConfig`를 사용하여 인증/인가를 처리했습니다. 하지만 이 방식은 다음과 같은 문제가 있었습니다:
1. SecurityConfig가 모든 엔드포인트에 적용되어, 특정 엔드포인트별로 다른 인증 방식을 적용하기 어려웠습니다.
2. LDAP 기반 인증과 일반 사용자 인증을 동시에 지원하기 어려웠습니다.
3. 설정이 복잡해지면서 코드 가독성이 떨어졌습니다.

**해결 방안:**
SecurityConfig를 제거하고, Interceptor 기반의 인증 시스템으로 전환했습니다. 이를 통해:
1. **LdapAuthInterceptor**: `/admin/*` 엔드포인트에서 LDAP 기반 인증 수행
2. **UserAuthInterceptor**: `/api/*` 엔드포인트에서 일반 사용자 인증 수행
3. 각 Interceptor에서 필요한 인증 로직만 처리

**구현 세부사항:**

1. **LdapAuthInterceptor**:
   - LDAP 서버에 사용자 credentials를 검증
   - LDAP DN(Distinguished Name) 기반의 인증
   - `Authorization` 헤더에서 Basic Auth 추출 후 검증
   - 인증 실패 시 401 응답

2. **UserAuthInterceptor**:
   - JWT 토큰 또는 session 기반 인증
   - `Authorization` 헤더에서 토큰 추출
   - 토큰 검증 및 사용자 정보 로드
   - 인증 실패 시 401 응답

3. **WebConfig**:
   - 각 Interceptor를 적절한 경로에 등록
   - `/admin/**` → LdapAuthInterceptor
   - `/api/**` → UserAuthInterceptor

**관련 코드:**
```kotlin
// LdapAuthInterceptor.kt
@Component
class LdapAuthInterceptor(
    private val ldapTemplate: LdapTemplate,
) : HandlerInterceptor {

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        val authHeader = request.getHeader("Authorization") ?: return sendUnauthorized(response)

        // "Basic base64(username:password)" 형식 파싱
        val credentials = parseBasicAuth(authHeader) ?: return sendUnauthorized(response)

        // LDAP 검증
        val isAuthenticated = ldapTemplate.authenticate(
            LdapNameBuilder.root()
                .add("cn", credentials.first)
                .build(),
            credentials.second
        )

        return if (isAuthenticated) true else sendUnauthorized(response)
    }

    private fun sendUnauthorized(response: HttpServletResponse): Boolean {
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = "application/json"
        response.writer.write("""{"error": "Unauthorized"}""")
        return false
    }
}

// UserAuthInterceptor.kt
@Component
class UserAuthInterceptor(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userService: UserService,
) : HandlerInterceptor {

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        val authHeader = request.getHeader("Authorization") ?: return sendUnauthorized(response)

        val token = authHeader.removePrefix("Bearer ")
        val userId = jwtTokenProvider.extractUserId(token) ?: return sendUnauthorized(response)

        // 사용자 정보를 request 속성에 저장 (컨트롤러에서 접근 가능)
        request.setAttribute("userId", userId)

        return true
    }

    private fun sendUnauthorized(response: HttpServletResponse): Boolean {
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = "application/json"
        response.writer.write("""{"error": "Unauthorized"}""")
        return false
    }
}

// WebConfig.kt
@Configuration
class WebConfig(
    private val ldapAuthInterceptor: LdapAuthInterceptor,
    private val userAuthInterceptor: UserAuthInterceptor,
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(ldapAuthInterceptor)
            .addPathPatterns("/admin/**")

        registry.addInterceptor(userAuthInterceptor)
            .addPathPatterns("/api/**")
    }
}
```

**고민한 점:**
- SecurityConfig를 완전히 제거했는데, 이로 인해 CORS, CSRF 보호 등의 기능이 손실되었을 수 있습니다. 필요하다면 WebConfig에서 별도로 처리해야 합니다.

- Interceptor는 Spring의 DispatcherServlet 레벨에서 동작하므로, `/static`, `/public` 등의 정적 리소스 경로는 자동으로 제외됩니다. 하지만 API 경로에서는 모든 요청이 Interceptor를 거치므로, Interceptor의 성능이 영향을 줄 수 있습니다.

- LDAP 인증 시 매번 LDAP 서버에 요청을 보내므로, 네트워크 지연이 발생할 수 있습니다. 이를 개선하려면 LDAP 결과를 캐싱하거나, 사전에 LDAP 서버와의 연결을 풀로 관리해야 합니다.

- UserAuthInterceptor에서 JWT 토큰을 검증하지만, 토큰 갱신(refresh) 로직이 없습니다. 만약 토큰이 만료되었다면, 클라이언트가 새로운 토큰을 받아야 하는데, 현재는 그 로직이 구현되지 않았습니다.

---

#### 5. OrderService와 Repository 패턴: 쿼리 메서드와 Custom Repository의 조합

**배경 및 문제 상황:**
주문 조회는 다양한 조건을 만족해야 합니다. Admin이 전체 주문을 조회할 때는 정렬과 페이지네이션을 지원해야 하고, 사용자가 자신의 주문을 조회할 때는 userId 필터링이 필요합니다. 또한 주문에 포함된 OrderItem도 함께 조회해야 하므로, 단순한 JpaRepository로는 부족할 수 있습니다.

**해결 방안:**
Spring Data JPA의 기본 쿼리 메서드와 Custom Repository(`RepositoryImpl`)를 조합하여 구현했습니다:

1. **기본 쿼리 메서드**: `findById`, `findByUserId` 등의 간단한 쿼리는 JpaRepository에서 제공하는 자동 생성 메서드 사용

2. **Custom Repository**: 복잡한 쿼리는 `OrderRepositoryImpl`에서 QueryDSL 또는 JPQL을 사용하여 구현

**구현 세부사항:**

```kotlin
// OrderRepository.kt - 인터페이스
interface OrderRepository : JpaRepository<Order, Long>, OrderRepositoryCustom {
    fun findByUserId(userId: Long): List<Order>
    fun findByUserIdOrderByCreatedAtDesc(userId: Long, pageable: Pageable): Page<Order>
}

// OrderRepositoryCustom.kt - Custom 메서드 정의
interface OrderRepositoryCustom {
    fun findAdminOrders(pageable: Pageable, sortOption: AdminOrderSortOption?): Page<AdminOrderInfo>
    fun findOrderWithItemsById(orderId: Long): Order?
}

// OrderRepositoryImpl.kt - Custom 구현
@Repository
class OrderRepositoryImpl(
    private val jpaQueryFactory: JPAQueryFactory,
    private val orderJpaRepository: OrderJpaRepository,
) : OrderRepositoryCustom {

    override fun findAdminOrders(
        pageable: Pageable,
        sortOption: AdminOrderSortOption?,
    ): Page<AdminOrderInfo> {
        val query = jpaQueryFactory
            .select(
                QOrder.order.id,
                QOrder.order.user.id,
                QOrder.order.createdAt,
                QOrder.order.orderItems.asAny().sumOf { it.price * it.quantity },
            )
            .from(QOrder.order)
            .leftJoin(QOrder.order.user)

        // 정렬 옵션 적용
        when (sortOption) {
            AdminOrderSortOption.PRICE_DESC -> query.orderBy(QOrder.order.id.desc())  // 실제로는 total price로 정렬
            AdminOrderSortOption.PRICE_ASC -> query.orderBy(QOrder.order.id.asc())
            else -> query.orderBy(QOrder.order.createdAt.desc())  // RECENT (기본값)
        }

        // 페이지네이션
        val total = query.fetchCount()
        val results = query
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        return PageImpl(results, pageable, total)
    }

    override fun findOrderWithItemsById(orderId: Long): Order? {
        return jpaQueryFactory
            .selectFrom(QOrder.order)
            .leftJoin(QOrder.order.orderItems).fetchJoin()
            .leftJoin(QOrder.order.user).fetchJoin()
            .where(QOrder.order.id.eq(orderId))
            .fetchOne()
    }
}
```

**고민한 점:**
- `findOrderWithItemsById()`에서 `leftJoin().fetchJoin()`을 사용하여 N+1 쿼리 문제를 방지했습니다. 하지만 OrderItem이 여러 개일 경우, 카르테시안 곱(Cartesian Product)이 발생할 수 있습니다. 이를 해결하려면 별도의 쿼리로 OrderItem을 조회해야 할 수 있습니다.

- Admin 주문 조회 시 정렬을 `createdAt`으로 하고 있는데, 나중에 총가격으로 정렬하려면 집계 함수를 사용해야 합니다:
  ```kotlin
  .select(/* ... */, QOrderItem.orderItem.price.multiply(QOrderItem.orderItem.quantity).sum())
  .groupBy(QOrder.order.id)
  .orderBy(/* ... */)
  ```

- 현재는 읽기 전용 메서드에 `@Transactional(readOnly = true)`를 설정하지 않았는데, 나중에 성능 최적화를 위해 추가할 수 있습니다.

---

#### 6. 테스트 전략 확대: 단위 테스트 → 통합 테스트 → E2E 테스트

**배경 및 문제 상황:**
Order 도메인과 API 계층이 추가되면서, 테스트 커버리지를 확대해야 했습니다. 특히:
1. OrderService의 비즈니스 로직이 올바르게 동작하는지 확인 (단위 테스트)
2. OrderRepository와 데이터베이스 상호작용 확인 (통합 테스트)
3. API 엔드포인트가 올바른 응답을 반환하는지 확인 (E2E 테스트)

**해결 방안:**
테스트 피라미드 구조를 따라 세 가지 수준의 테스트를 구현했습니다:

1. **단위 테스트** (`*Test.kt`): 도메인 로직만 테스트 (Mock 사용)
2. **통합 테스트** (`*IntegrationTest.kt`): 실제 데이터베이스와 상호작용 (Testcontainers)
3. **E2E 테스트** (`*E2ETest.kt`): 전체 HTTP 요청-응답 흐름 (MockMvc)

**구현 세부사항:**

```kotlin
// OrderServiceTest.kt - 단위 테스트
@DisplayName("OrderService")
class OrderServiceTest {

    private lateinit var orderService: OrderService
    private val orderRepository: OrderRepository = mockk()
    private val userService: UserService = mockk()

    @BeforeEach
    fun setUp() {
        orderService = OrderService(orderRepository, userService)
    }

    @DisplayName("주문 생성 시 상태는 PENDING이어야 한다")
    @Test
    fun createOrder_ShouldSetStatusToPending() {
        // Arrange
        val userId = 1L
        val user = User(loginId = "user1", name = "User", email = "user@example.com")
        every { userService.getById(userId) } returns user
        every { orderRepository.save(any()) } answers { firstArg() }

        // Act
        val result = orderService.createOrder(userId)

        // Assert
        assertThat(result.status).isEqualTo(OrderStatus.PENDING)
    }
}

// OrderRepositoryImplIntegrationTest.kt - 통합 테스트
@SpringBootTest
@ExtendWith(PostgresContainerExtension::class)
class OrderRepositoryImplIntegrationTest(
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
) {

    @DisplayName("전체 주문을 페이징으로 조회할 수 있다")
    @Test
    fun findAdminOrders_ShouldReturnPagedOrders() {
        // Arrange
        val user = userRepository.save(User(loginId = "user1", ...))
        val order = Order(user = user, status = OrderStatus.PENDING)
        orderRepository.save(order)

        val pageable = PageRequest.of(0, 20)

        // Act
        val result = orderRepository.findAdminOrders(pageable, AdminOrderSortOption.RECENT)

        // Assert
        assertThat(result.content).hasSize(1)
        assertThat(result.totalElements).isEqualTo(1)
    }
}

// OrderV1ApiE2ETest.kt - E2E 테스트
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(PostgresContainerExtension::class)
class OrderV1ApiE2ETest(
    private val mockMvc: MockMvc,
    private val userRepository: UserRepository,
    private val productRepository: ProductRepository,
) {

    @DisplayName("POST /api/v1/orders - 주문 생성")
    @Test
    fun createOrder_ShouldReturn201() {
        // Arrange
        val user = userRepository.save(User(loginId = "user1", ...))
        val product = productRepository.save(Product(name = "Product", price = BigDecimal("100")))

        val request = OrderV1Dto.CreateOrderRequest(
            items = listOf(
                OrderV1Dto.CreateOrderItemRequest(
                    productId = product.id!!,
                    quantity = 1,
                )
            )
        )

        // Act
        val result = mockMvc.post("/api/v1/orders") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
            header("Authorization", "Bearer <token>")
        }

        // Assert
        result
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.orderId").exists())
            .andExpect(jsonPath("$.data.status").value("PENDING"))
    }
}
```

**고민한 점:**
- 테스트 데이터 설정이 복잡해질 수 있습니다. 이를 해결하려면 `@DataJpaTest`나 `@WebMvcTest` 어노테이션을 사용하여 필요한 빈만 로드하는 방식을 고려할 수 있습니다.

- E2E 테스트 시 실제 JWT 토큰을 생성해야 하는데, 이를 위해 별도의 토큰 생성 헬퍼 메서드를 만들었습니다. 하지만 토큰 생성 로직이 변경되면 테스트도 함께 변경해야 합니다.

- Testcontainers를 사용하므로 테스트 실행 시간이 증가합니다. 이를 개선하려면 Docker Compose를 사용하여 필요한 서비스(MySQL, Redis)를 미리 시작해놓고 테스트를 실행할 수 있습니다.

---

#### 7. DTO 설계와 응답 구조: 데이터 노출 최소화 원칙

**배경 및 문제 상황:**
API 응답 시 불필요한 데이터를 노출하면 보안 위험이 증가할 수 있습니다. 예를 들어, User 엔티티에 비밀번호 해시나 권한 정보가 있다면, 이것이 응답에 포함되지 않아야 합니다. 또한 Admin API와 User API는 같은 데이터를 반환하더라도 노출 수준이 달라야 할 수 있습니다.

**해결 방안:**
별도의 DTO 클래스를 만들어, 엔티티에서 필요한 필드만 추출하여 응답합니다. 이를 통해:
1. 엔티티의 모든 필드가 응답에 포함되지 않도록 제어
2. Admin과 User API에 따라 다른 DTO 반환 가능
3. API 응답 형식의 변경이 엔티티 구조에 영향을 주지 않음

**구현 세부사항:**

```kotlin
// Order 엔티티 관련 DTO들

// AdminOrderV1Dto.kt - Admin API 응답
object AdminOrderV1Dto {
    data class OrderResponse(
        val orderId: Long,
        val userId: Long,
        val orderDate: ZonedDateTime,
        val totalPrice: BigDecimal,
    ) {
        companion object {
            fun from(order: Order): OrderResponse = OrderResponse(
                orderId = order.id!!,
                userId = order.user.id!!,
                orderDate = order.createdAt.atZone(ZoneId.of("Asia/Seoul")),
                totalPrice = order.getTotalPrice(),
            )
        }
    }

    data class OrderDetailResponse(
        val orderId: Long,
        val userId: Long,
        val status: String,
        val items: List<OrderItemResponse>,
        val totalPrice: BigDecimal,
        val orderDate: ZonedDateTime,
    ) {
        companion object {
            fun from(order: Order): OrderDetailResponse = OrderDetailResponse(
                orderId = order.id!!,
                userId = order.user.id!!,
                status = order.status.name,
                items = order.orderItems.map { OrderItemResponse.from(it) },
                totalPrice = order.getTotalPrice(),
                orderDate = order.createdAt.atZone(ZoneId.of("Asia/Seoul")),
            )
        }
    }

    data class OrderItemResponse(
        val itemId: Long,
        val productId: Long,
        val productName: String,
        val quantity: Int,
        val price: BigDecimal,
    ) {
        companion object {
            fun from(item: OrderItem): OrderItemResponse = OrderItemResponse(
                itemId = item.id!!,
                productId = item.productId,
                productName = item.productName,
                quantity = item.quantity,
                price = item.price,
            )
        }
    }
}

// OrderV1Dto.kt - User API 요청/응답
object OrderV1Dto {
    data class CreateOrderRequest(
        val items: List<CreateOrderItemRequest>,
    )

    data class CreateOrderItemRequest(
        val productId: Long,
        val quantity: Int,
    )

    data class OrderedInfo(
        val orderId: Long,
        val status: String,
        val totalPrice: BigDecimal,
        val items: List<OrderItemInfo>,
    ) {
        companion object {
            fun from(order: Order): OrderedInfo = OrderedInfo(
                orderId = order.id!!,
                status = order.status.name,
                totalPrice = order.getTotalPrice(),
                items = order.orderItems.map { OrderItemInfo.from(it) },
            )
        }
    }

    data class OrderItemInfo(
        val productId: Long,
        val productName: String,
        val quantity: Int,
        val subtotal: BigDecimal,
    ) {
        companion object {
            fun from(item: OrderItem): OrderItemInfo = OrderItemInfo(
                productId = item.productId,
                productName = item.productName,
                quantity = item.quantity,
                subtotal = item.getSubtotal(),
            )
        }
    }
}
```

**고민한 점:**
- DTO 변환 로직이 매번 반복됩니다. 이를 개선하려면 MapStruct 같은 라이브러리를 사용하거나, 확장 함수로 변환 로직을 일반화할 수 있습니다.

- Admin API는 더 많은 정보를 노출하고, User API는 필요한 정보만 노출합니다. 이 기준이 명확하지 않으면, 나중에 DTO가 증가할 수 있습니다.

- Timestamp 필드의 시간대(timezone) 처리가 중요합니다. 현재는 `Asia/Seoul`로 고정했지만, 사용자의 시간대에 따라 다르게 반환해야 할 수도 있습니다.

---

## ✅ Checklist

### Order 도메인 (7/7)
- [x] **Order 엔티티**: `apps/commerce-api/src/main/kotlin/com/loopers/domain/order/Order.kt`
  - status (OrderStatus) 필드
  - orderItems (OneToMany) 관계
  - getTotalPrice(), changeStatus() 메서드

- [x] **OrderItem 엔티티**: `apps/commerce-api/src/main/kotlin/com/loopers/domain/order/OrderItem.kt`
  - productId, quantity, price, productName 필드
  - getSubtotal() 메서드

- [x] **OrderStatus enum**: `apps/commerce-api/src/main/kotlin/com/loopers/domain/order/OrderStatus.kt`
  - PENDING, PAID, SHIPPED, DELIVERED, RETURNED

- [x] **OrderService**: `apps/commerce-api/src/main/kotlin/com/loopers/domain/order/OrderService.kt`
  - createOrder, getOrderById, getUserOrders
  - getAdminOrders (정렬 지원)

- [x] **OrderRepository & Impl**:
  - `apps/commerce-api/src/main/kotlin/com/loopers/domain/order/OrderRepository.kt`
  - `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/order/OrderRepositoryImpl.kt`
  - findAdminOrders, findOrderWithItemsById

- [x] **Order DTO들**:
  - `apps/commerce-api/src/main/kotlin/com/loopers/domain/order/dto/OrderedInfo.kt`
  - `apps/commerce-api/src/main/kotlin/com/loopers/domain/order/dto/AdminOrderInfo.kt`
  - `apps/commerce-api/src/main/kotlin/com/loopers/domain/order/dto/OrderItemInfo.kt`

### Admin Order API (5/5)
- [x] **AdminOrderV1ApiSpec**: `apps/commerce-api/src/main/kotlin/com/loopers/interfaces/admin/order/AdminOrderV1ApiSpec.kt`
  - getOrders (페이징, 정렬)
  - getOrderById

- [x] **AdminOrderV1Dto**: `apps/commerce-api/src/main/kotlin/com/loopers/interfaces/admin/order/AdminOrderV1Dto.kt`
  - OrderResponse, OrderDetailResponse, OrderItemResponse

- [x] **AdminOrderV1Controller**: `apps/commerce-api/src/main/kotlin/com/loopers/interfaces/admin/order/AdminOrderV1Controller.kt`
  - GET /admin/v1/orders
  - GET /admin/v1/orders/{orderId}

- [x] **AdminOrderFacade**: `apps/commerce-api/src/main/kotlin/com/loopers/application/admin/order/AdminOrderFacade.kt`
  - getOrders, getOrderDetail

- [x] **AdminOrderSortOption**: `apps/commerce-api/src/main/kotlin/com/loopers/interfaces/admin/order/AdminOrderSortOption.kt`
  - RECENT, PRICE_DESC, PRICE_ASC

### User Order API (5/5)
- [x] **OrderV1ApiSpec**: `apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/order/OrderV1ApiSpec.kt`
  - createOrder
  - getOrders
  - getOrderById

- [x] **OrderV1Dto**: `apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/order/OrderV1Dto.kt`
  - CreateOrderRequest, OrderedInfo, OrderItemInfo

- [x] **OrderV1Controller**: `apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/order/OrderV1Controller.kt`
  - POST /api/v1/orders
  - GET /api/v1/orders
  - GET /api/v1/orders/{orderId}

- [x] **OrderFacade**: `apps/commerce-api/src/main/kotlin/com/loopers/application/api/order/OrderFacade.kt`
  - createOrder, getUserOrders, getOrderById

- [x] **PageResponse**: `apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/PageResponse.kt`
  - 페이징 응답 wrapper

### 인증 시스템 (5/5)
- [x] **LdapAuthInterceptor**: `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/interceptor/LdapAuthInterceptor.kt`
  - LDAP 기반 인증
  - Basic Auth 파싱

- [x] **UserAuthInterceptor**: `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/interceptor/UserAuthInterceptor.kt`
  - JWT/Session 기반 인증
  - userId 추출 및 request 속성에 저장

- [x] **WebConfig**: `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/config/WebConfig.kt`
  - Interceptor 등록
  - `/admin/**` → LdapAuthInterceptor
  - `/api/**` → UserAuthInterceptor

- [x] **SecurityConfig 제거**: 기존 Spring Security 설정 삭제
- [x] **PasswordEncoderConfig 제거**: 더 이상 필요 없음

### 테스트 (6/6)
- [x] **OrderServiceTest**: `apps/commerce-api/src/test/kotlin/com/loopers/domain/order/OrderServiceTest.kt` (88줄)
  - 주문 생성 테스트
  - 상태 변경 테스트

- [x] **AdminOrderFacadeTest**: `apps/commerce-api/src/test/kotlin/com/loopers/application/api/order/AdminOrderFacadeTest.kt` (69줄)
  - Admin 주문 조회 테스트
  - 정렬 옵션 테스트

- [x] **AdminOrderV1ControllerE2ETest**: `apps/commerce-api/src/test/kotlin/com/loopers/interfaces/admin/order/AdminOrderV1ControllerE2ETest.kt` (144줄)
  - POST /admin/v1/orders
  - GET /admin/v1/orders (페이징, 정렬)
  - GET /admin/v1/orders/{orderId}

- [x] **OrderV1ApiE2ETest**: `apps/commerce-api/src/test/kotlin/com/loopers/interfaces/api/order/OrderV1ApiE2ETest.kt` (360줄)
  - POST /api/v1/orders
  - GET /api/v1/orders
  - GET /api/v1/orders/{orderId}

- [x] **LdapAuthInterceptorTest**: `apps/commerce-api/src/test/kotlin/com/loopers/infrastructure/interceptor/LdapAuthInterceptorTest.kt` (119줄)
  - LDAP 인증 성공/실패
  - Basic Auth 파싱

- [x] **ProductLikeV1ControllerE2ETest**: 확장 (259줄)
  - Like 관련 API E2E 테스트

### 배치 애플리케이션 (2/2)
- [x] **CommerceBatchApplicationTest**: `@TestPropertySource` 추가
  - Spring Batch 자동 실행 방지
  - 테스트 격리 보장

- [x] **StepMonitorListener**: 포맷팅 개선
  - 문자열 끝 쉼표 추가

### 개발 문서 (1/1)
- [x] **CLAUDE.md**: Priority 섹션 개선
  - "0. 절대 커밋하지 말 것" 규칙 추가

## 📎 References

- **설계 문서**:
  - [Order 도메인 설계](./docs/plans/2026-02-19-order-domain-design.md)
  - [Order 도메인 구현](./docs/plans/2026-02-19-order-domain-implementation.md)
  - [Admin Order API 구현](./docs/plans/2026-02-20-admin-order-controller-implementation.md)

- **관련 이슈**:
  - Round3 Implementation Quest: Product / Brand / Like / Order / Admin

- **참고 패턴**:
  - Aggregate 패턴 (Order → OrderItem)
  - Facade 패턴 (OrderFacade, AdminOrderFacade)
  - Repository 패턴 (Custom Repository with QueryDSL)
  - Interceptor 패턴 (인증)

---

## 🔄 다음 단계

1. **결제 시스템** (Payment 도메인)
   - Payment 엔티티 추가
   - Order.status → PAID 자동 전이

2. **배송 정보** (Shipping 도메인)
   - Shipping 엔티티 추가
   - Order 상태 추적

3. **인증 성능 개선**
   - LDAP 결과 캐싱
   - JWT 토큰 갱신 (refresh token)

4. **Admin 페이지**
   - 주문 상태 변경 API
   - 매출 통계 API

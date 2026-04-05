package com.loopers.interfaces.api.order

import com.loopers.domain.brand.Brand
import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponTemplate
import com.loopers.domain.coupon.CouponType
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductStatus
import com.loopers.domain.queue.QueueRepository
import com.loopers.domain.stock.Stock
import com.loopers.domain.user.User
import com.loopers.domain.user.vo.BirthDate
import com.loopers.domain.user.vo.Email
import com.loopers.domain.user.vo.LoginId
import com.loopers.domain.user.vo.Name
import com.loopers.domain.user.vo.Password
import com.loopers.domain.coupon.CouponTemplateRepository
import com.loopers.infrastructure.queue.WaitingQueueRegistry
import com.loopers.infrastructure.scheduler.QueueScheduler
import com.loopers.testcontainers.RedisTestContainersConfig
import com.loopers.utils.RedisCleanUp
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.coupon.CouponJpaRepository
import com.loopers.infrastructure.order.OrderJpaRepository
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.infrastructure.stock.StockJpaRepository
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.interfaces.api.ApiResponse
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Import
import org.springframework.core.ParameterizedTypeReference
import com.loopers.interfaces.api.PageResponse
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import java.math.BigDecimal
import kotlin.test.Test

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(RedisTestContainersConfig::class)
class OrderV1ApiE2ETest @Autowired constructor(
    private val restTemplate: TestRestTemplate,
    private val userJpaRepository: UserJpaRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val stockJpaRepository: StockJpaRepository,
    private val orderJpaRepository: OrderJpaRepository,
    private val couponJpaRepository: CouponJpaRepository,
    private val couponTemplateRepository: CouponTemplateRepository,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
    private val queueRepository: QueueRepository,
    private val waitingQueueRegistry: WaitingQueueRegistry,
    private val passwordEncoder: PasswordEncoder,
) {
    @MockBean
    private lateinit var queueScheduler: QueueScheduler

    companion object {
        private const val ENDPOINT_ORDERS = "/api/v1/orders"
        private const val QUEUE_NAME = "order-queue"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
        redisCleanUp.truncateAll()
    }

    private fun createAuthHeaders(loginId: String, password: String): HttpHeaders {
        return HttpHeaders().apply {
            set("X-Loopers-LoginId", loginId)
            set("X-Loopers-LoginPw", password)
        }
    }

    private fun createAuthHeadersWithToken(loginId: String, password: String, token: String): HttpHeaders {
        return HttpHeaders().apply {
            set("X-Loopers-LoginId", loginId)
            set("X-Loopers-LoginPw", password)
            set("X-Entry-Token", token)
        }
    }

    private fun getEntryToken(loginId: String, userId: Long): String {
        // Redis에 직접 토큰 발급 (스케줄러 역할 시뮬레이션)
        val token = java.util.UUID.randomUUID().toString()
        val config = waitingQueueRegistry.getQueueConfig(QUEUE_NAME)
        val ttlSeconds = config?.activeTokenTTLSeconds?.toLong() ?: 300L
        queueRepository.issueToken(QUEUE_NAME, userId, token, ttlSeconds)
        return token
    }

    @DisplayName("POST /api/v1/orders")
    @Nested
    inner class CreateOrder {

        @Test
        @DisplayName("유효한 주문 정보와 토큰을 전달하면, 주문을 생성하고 201 CREATED 응답을 받는다")
        fun createOrderWithValidInfo() {
            // given
            val plainPassword = "encryptedPassword"
            val user = User.create(
                loginId = LoginId.of("test123"),
                password = Password.ofEncrypted(passwordEncoder.encode(plainPassword)),
                name = Name.of("테스트"),
                birthDate = BirthDate.of("20260101"),
                email = Email.of("test@test.com"),
            )
            val savedUser = userJpaRepository.save(user)

            val brand = Brand.create(
                name = "테스트 브랜드",
                description = "테스트 브랜드 설명",
            )
            val savedBrand = brandJpaRepository.save(brand)

            val product = Product.create(
                brand = savedBrand,
                name = "테스트 상품",
                price = BigDecimal("10000"),
                status = ProductStatus.ACTIVE,
            )
            val savedProduct = productJpaRepository.save(product)

            // 재고 생성
            stockJpaRepository.save(
                Stock.create(
                    productId = savedProduct.id,
                    quantity = 100,
                ),
            )

            val request = OrderV1Dto.OrderRequest(
                items = listOf(
                    OrderV1Dto.OrderItemRequest(
                        productId = savedProduct.id,
                        quantity = 2,
                    ),
                ),
            )

            // 토큰 발급
            val token = getEntryToken("test123", savedUser.id)
            val headers = createAuthHeadersWithToken("test123", "encryptedPassword", token)

            // when
            val responseType = object : ParameterizedTypeReference<ApiResponse<Long>>() {}
            val response = restTemplate.exchange(
                ENDPOINT_ORDERS,
                HttpMethod.POST,
                HttpEntity(request, headers),
                responseType,
            )

            // then
            assertAll(
                { assertEquals(HttpStatus.CREATED, response.statusCode) },
                { assertThat(response.body?.data).isNotNull },
            )

            val savedOrder = orderJpaRepository.findById(response.body?.data!!)
            assertThat(savedOrder).isPresent
            assertThat(savedOrder.get().userId).isEqualTo(savedUser.id)
        }

        @Test
        @DisplayName("주문 상품이 비어있으면 400 BAD_REQUEST 응답을 받는다")
        fun createOrderWithEmptyItems() {
            // given
            val plainPassword = "encryptedPassword"
            val user = User.create(
                loginId = LoginId.of("test123"),
                password = Password.ofEncrypted(passwordEncoder.encode(plainPassword)),
                name = Name.of("테스트"),
                birthDate = BirthDate.of("20260101"),
                email = Email.of("test@test.com"),
            )
            val savedUser = userJpaRepository.save(user)

            val request = OrderV1Dto.OrderRequest(
                items = emptyList(),
            )

            // 토큰 발급
            val token = getEntryToken("test123", savedUser.id)
            val headers = createAuthHeadersWithToken("test123", "encryptedPassword", token)

            // when
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = restTemplate.exchange(
                ENDPOINT_ORDERS,
                HttpMethod.POST,
                HttpEntity(request, headers),
                responseType,
            )

            // then
            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        }

        @Test
        @DisplayName("인증 헤더 없이 요청하면 401 Unauthorized 응답을 받는다")
        fun createOrderWithoutAuthHeader() {
            // given
            val request = OrderV1Dto.OrderRequest(
                items = listOf(
                    OrderV1Dto.OrderItemRequest(
                        productId = 1L,
                        quantity = 1,
                    ),
                ),
            )

            // when
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = restTemplate.exchange(
                ENDPOINT_ORDERS,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // then
            assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        }
    }

    @DisplayName("GET /api/v1/orders")
    @Nested
    inner class GetOrders {

        @Test
        @DisplayName("사용자의 주문 목록을 페이징으로 조회할 수 있다")
        fun getOrdersWithPagination() {
            // given
            val plainPassword = "encryptedPassword"
            val user = User.create(
                loginId = LoginId.of("test123"),
                password = Password.ofEncrypted(passwordEncoder.encode(plainPassword)),
                name = Name.of("테스트"),
                birthDate = BirthDate.of("20260101"),
                email = Email.of("test@test.com"),
            )
            val savedUser = userJpaRepository.save(user)

            val brand = Brand.create(
                name = "테스트 브랜드",
                description = "테스트 브랜드",
            )
            val savedBrand = brandJpaRepository.save(brand)

            val product1 = Product.create(
                brand = savedBrand,
                name = "상품1",
                price = BigDecimal("10000"),
                status = ProductStatus.ACTIVE,
            )
            val savedProduct1 = productJpaRepository.save(product1)

            // 재고 생성
            stockJpaRepository.save(
                Stock.create(
                    productId = savedProduct1.id,
                    quantity = 100,
                ),
            )

            val product2 = Product.create(
                brand = savedBrand,
                name = "상품2",
                price = BigDecimal("20000"),
                status = ProductStatus.ACTIVE,
            )
            val savedProduct2 = productJpaRepository.save(product2)

            // 재고 생성
            stockJpaRepository.save(
                Stock.create(
                    productId = savedProduct2.id,
                    quantity = 100,
                ),
            )

            // API를 통해 주문 생성 (1번 주문) - 토큰 직전 생성
            val request1 = OrderV1Dto.OrderRequest(
                items = listOf(
                    OrderV1Dto.OrderItemRequest(
                        productId = savedProduct1.id,
                        quantity = 1,
                    ),
                ),
            )
            val token1 = getEntryToken("test123", savedUser.id)
            val headers1 = createAuthHeadersWithToken("test123", "encryptedPassword", token1)
            val responseType1 = object : ParameterizedTypeReference<ApiResponse<Long>>() {}
            val response1 = restTemplate.exchange(
                ENDPOINT_ORDERS,
                HttpMethod.POST,
                HttpEntity(request1, headers1),
                responseType1,
            )
            assertThat(response1.statusCode).isEqualTo(HttpStatus.CREATED) // 검증용

            // API를 통해 주문 생성 (2번 주문) - 토큰 직전 생성
            val request2 = OrderV1Dto.OrderRequest(
                items = listOf(
                    OrderV1Dto.OrderItemRequest(
                        productId = savedProduct2.id,
                        quantity = 2,
                    ),
                ),
            )
            val token2 = getEntryToken("test123", savedUser.id)
            val headers2 = createAuthHeadersWithToken("test123", "encryptedPassword", token2)
            val responseType2 = object : ParameterizedTypeReference<ApiResponse<Long>>() {}
            restTemplate.exchange(
                ENDPOINT_ORDERS,
                HttpMethod.POST,
                HttpEntity(request2, headers2),
                responseType2,
            )

            // 조회는 토큰 불필요
            val authHeaders = createAuthHeaders("test123", "encryptedPassword")

            // when
            val responseType = object : ParameterizedTypeReference<ApiResponse<PageResponse<OrderV1Dto.OrderResponse>>>() {}
            val response = restTemplate.exchange(
                "$ENDPOINT_ORDERS?page=0&size=10",
                HttpMethod.GET,
                HttpEntity<Any>(authHeaders),
                responseType,
            )

            // then
            assertAll(
                { assertEquals(HttpStatus.OK, response.statusCode) },
                { assertThat(response.body?.data?.content).hasSize(2) },
            )
        }

        @Test
        @DisplayName("인증 헤더 없이 요청하면 401 Unauthorized 응답을 받는다")
        fun getOrdersWithoutAuthHeader() {
            // when
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = restTemplate.exchange(
                "$ENDPOINT_ORDERS?page=0&size=10",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                responseType,
            )

            // then
            assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        }
    }

    @DisplayName("GET /api/v1/orders/{orderId}")
    @Nested
    inner class GetOrder {

        @Test
        @DisplayName("특정 주문을 조회할 수 있다")
        fun getOrderById() {
            // given
            val plainPassword = "encryptedPassword"
            val user = User.create(
                loginId = LoginId.of("test123"),
                password = Password.ofEncrypted(passwordEncoder.encode(plainPassword)),
                name = Name.of("테스트"),
                birthDate = BirthDate.of("20260101"),
                email = Email.of("test@test.com"),
            )
            val savedUser = userJpaRepository.save(user)

            val brand = Brand.create(
                name = "테스트 브랜드",
                description = "테스트 브랜드",
            )
            val savedBrand = brandJpaRepository.save(brand)

            val product = Product.create(
                brand = savedBrand,
                name = "상품1",
                price = BigDecimal("10000"),
                status = ProductStatus.ACTIVE,
            )
            val savedProduct = productJpaRepository.save(product)

            // 재고 생성
            stockJpaRepository.save(
                Stock.create(
                    productId = savedProduct.id,
                    quantity = 100,
                ),
            )

            // 토큰 발급
            val token = getEntryToken("test123", savedUser.id)
            val createHeaders = createAuthHeadersWithToken("test123", "encryptedPassword", token)

            // API를 통해 주문 생성
            val createRequest = OrderV1Dto.OrderRequest(
                items = listOf(
                    OrderV1Dto.OrderItemRequest(
                        productId = savedProduct.id,
                        quantity = 1,
                    ),
                ),
            )
            val createResponseType = object : ParameterizedTypeReference<ApiResponse<Long>>() {}
            val createResponse = restTemplate.exchange(
                ENDPOINT_ORDERS,
                HttpMethod.POST,
                HttpEntity(createRequest, createHeaders),
                createResponseType,
            )
            val orderId = createResponse.body?.data!!

            // 조회는 토큰 불필요
            val authHeaders = createAuthHeaders("test123", "encryptedPassword")

            // when
            val responseType = object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            val response = restTemplate.exchange(
                "$ENDPOINT_ORDERS/$orderId",
                HttpMethod.GET,
                HttpEntity<Any>(authHeaders),
                responseType,
            )

            // then
            assertAll(
                { assertEquals(HttpStatus.OK, response.statusCode) },
                { assertThat(response.body?.data?.orderId).isEqualTo(orderId) },
                { assertThat(response.body?.data?.orderItems).hasSize(1) },
            )
        }

        @Test
        @DisplayName("존재하지 않는 주문을 조회하면 404 NOT_FOUND 응답을 받는다")
        fun getOrderByIdNotFound() {
            // given
            val plainPassword = "encryptedPassword"
            val user = User.create(
                loginId = LoginId.of("test123"),
                password = Password.ofEncrypted(passwordEncoder.encode(plainPassword)),
                name = Name.of("테스트"),
                birthDate = BirthDate.of("20260101"),
                email = Email.of("test@test.com"),
            )
            userJpaRepository.save(user)

            val headers = createAuthHeaders("test123", "encryptedPassword")

            // when
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = restTemplate.exchange(
                "$ENDPOINT_ORDERS/99999",
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                responseType,
            )

            // then
            assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        }

        @Test
        @DisplayName("인증 헤더 없이 요청하면 401 Unauthorized 응답을 받는다")
        fun getOrderByIdWithoutAuthHeader() {
            // when
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = restTemplate.exchange(
                "$ENDPOINT_ORDERS/1",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                responseType,
            )

            // then
            assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        }
    }

    @DisplayName("쿠폰 적용 시나리오")
    @Nested
    inner class CouponApply {

        @Test
        @DisplayName("쿠폰 ID를 포함한 주문 요청을 처리할 수 있다")
        fun createOrderWithCouponId() {
            // given
            val plainPassword = "encryptedPassword"
            val user = User.create(
                loginId = LoginId.of("test456"),
                password = Password.ofEncrypted(passwordEncoder.encode(plainPassword)),
                name = Name.of("테스트2"),
                birthDate = BirthDate.of("20260101"),
                email = Email.of("test2@test.com"),
            )
            val savedUser = userJpaRepository.save(user)

            val brand = Brand.create(
                name = "테스트 브랜드2",
                description = "테스트 브랜드2 설명",
            )
            val savedBrand = brandJpaRepository.save(brand)

            val product = Product.create(
                brand = savedBrand,
                name = "테스트 상품2",
                price = BigDecimal("10000"),
                status = ProductStatus.ACTIVE,
            )
            val savedProduct = productJpaRepository.save(product)

            // 재고 생성
            stockJpaRepository.save(
                Stock.create(
                    productId = savedProduct.id,
                    quantity = 100,
                ),
            )

            val request = OrderV1Dto.OrderRequest(
                items = listOf(
                    OrderV1Dto.OrderItemRequest(
                        productId = savedProduct.id,
                        quantity = 1,
                    ),
                ),
                // 쿠폰이 없어도 정상 처리되어야 함
                couponId = null,
            )

            // 토큰 발급
            val token = getEntryToken("test456", savedUser.id)
            val headers = createAuthHeadersWithToken("test456", "encryptedPassword", token)

            // when
            val responseType = object : ParameterizedTypeReference<ApiResponse<Long>>() {}
            val response = restTemplate.exchange(
                ENDPOINT_ORDERS,
                HttpMethod.POST,
                HttpEntity(request, headers),
                responseType,
            )

            // then
            assertAll(
                { assertEquals(HttpStatus.CREATED, response.statusCode) },
                { assertThat(response.body?.data).isNotNull },
            )

            val savedOrder = orderJpaRepository.findById(response.body?.data!!)
            assertThat(savedOrder).isPresent
            assertThat(savedOrder.get().couponId).isNull()
        }

        @Test
        @DisplayName("쿠폰을 적용하여 주문을 생성할 수 있다")
        fun createOrderWithCoupon() {
            // given
            val plainPassword = "encryptedPassword"
            val user = User.create(
                loginId = LoginId.of("coupon01"),
                password = Password.ofEncrypted(passwordEncoder.encode(plainPassword)),
                name = Name.of("쿠폰 테스트"),
                birthDate = BirthDate.of("20260101"),
                email = Email.of("coupon@test.com"),
            )
            val savedUser = userJpaRepository.save(user)

            val brand = Brand.create(
                name = "쿠폰 테스트 브랜드",
                description = "쿠폰 테스트 브랜드",
            )
            val savedBrand = brandJpaRepository.save(brand)

            val product = Product.create(
                brand = savedBrand,
                name = "쿠폰 테스트 상품",
                price = BigDecimal("10000"),
                status = ProductStatus.ACTIVE,
            )
            val savedProduct = productJpaRepository.save(product)

            // 재고 생성
            stockJpaRepository.save(
                Stock.create(
                    productId = savedProduct.id,
                    quantity = 100,
                ),
            )

            // 쿠폰 생성
            val couponTemplate = CouponTemplate.create(
                name = "1000원 할인 쿠폰",
                type = CouponType.FIXED,
                value = BigDecimal("1000"),
                minOrderAmount = BigDecimal("5000"),
                expiredAt = java.time.ZonedDateTime.now().plusDays(30),
            )
            val savedTemplate = couponTemplateRepository.save(couponTemplate)

            val coupon = Coupon.issue(userId = savedUser.id, template = savedTemplate)
            val savedCoupon = couponJpaRepository.save(coupon)

            val request = OrderV1Dto.OrderRequest(
                items = listOf(
                    OrderV1Dto.OrderItemRequest(
                        productId = savedProduct.id,
                        quantity = 2,
                    ),
                ),
                couponId = savedCoupon.id,
            )

            // 토큰 발급
            val token = getEntryToken("coupon01", savedUser.id)
            val headers = createAuthHeadersWithToken("coupon01", plainPassword, token)

            // when
            val responseType = object : ParameterizedTypeReference<ApiResponse<Long>>() {}
            val response = restTemplate.exchange(
                ENDPOINT_ORDERS,
                HttpMethod.POST,
                HttpEntity(request, headers),
                responseType,
            )

            // then
            assertAll(
                { assertEquals(HttpStatus.CREATED, response.statusCode) },
                { assertThat(response.body?.data).isNotNull },
            )

            val savedOrder = orderJpaRepository.findById(response.body?.data!!)
            assertThat(savedOrder).isPresent
            assertThat(savedOrder.get().couponId).isEqualTo(savedCoupon.id)

            // 쿠폰이 사용 상태로 변경됐는지 확인
            val usedCoupon = couponJpaRepository.findById(savedCoupon.id)
            assertThat(usedCoupon).isPresent
            assertThat(usedCoupon.get().status.name).isEqualTo("USED")
        }

        @Test
        @DisplayName("여러 상품을 주문할 수 있고 각 상품의 재고가 올바르게 감소한다")
        fun createOrderWithMultipleProducts() {
            // given
            val plainPassword = "encryptedPassword"
            val user = User.create(
                loginId = LoginId.of("multi001"),
                password = Password.ofEncrypted(passwordEncoder.encode(plainPassword)),
                name = Name.of("다중 상품 테스트"),
                birthDate = BirthDate.of("20260101"),
                email = Email.of("multi@test.com"),
            )
            val savedUser = userJpaRepository.save(user)

            val brand = Brand.create(
                name = "다중 상품 테스트 브랜드",
                description = "다중 상품 테스트 브랜드",
            )
            val savedBrand = brandJpaRepository.save(brand)

            // 상품 1: 가격 10000, 재고 100
            val product1 = Product.create(
                brand = savedBrand,
                name = "상품1",
                price = BigDecimal("10000"),
                status = ProductStatus.ACTIVE,
            )
            val savedProduct1 = productJpaRepository.save(product1)
            stockJpaRepository.save(
                Stock.create(
                    productId = savedProduct1.id,
                    quantity = 100,
                ),
            )

            // 상품 2: 가격 20000, 재고 50
            val product2 = Product.create(
                brand = savedBrand,
                name = "상품2",
                price = BigDecimal("20000"),
                status = ProductStatus.ACTIVE,
            )
            val savedProduct2 = productJpaRepository.save(product2)
            stockJpaRepository.save(
                Stock.create(
                    productId = savedProduct2.id,
                    quantity = 50,
                ),
            )

            // 상품 3: 가격 15000, 재고 30
            val product3 = Product.create(
                brand = savedBrand,
                name = "상품3",
                price = BigDecimal("15000"),
                status = ProductStatus.ACTIVE,
            )
            val savedProduct3 = productJpaRepository.save(product3)
            stockJpaRepository.save(
                Stock.create(
                    productId = savedProduct3.id,
                    quantity = 30,
                ),
            )

            val request = OrderV1Dto.OrderRequest(
                items = listOf(
                    OrderV1Dto.OrderItemRequest(
                        productId = savedProduct1.id,
                        quantity = 5,
                    ),
                    OrderV1Dto.OrderItemRequest(
                        productId = savedProduct2.id,
                        quantity = 3,
                    ),
                    OrderV1Dto.OrderItemRequest(
                        productId = savedProduct3.id,
                        quantity = 7,
                    ),
                ),
            )

            // 토큰 발급
            val token = getEntryToken("multi001", savedUser.id)
            val headers = createAuthHeadersWithToken("multi001", plainPassword, token)

            // when
            val responseType = object : ParameterizedTypeReference<ApiResponse<Long>>() {}
            val response = restTemplate.exchange(
                ENDPOINT_ORDERS,
                HttpMethod.POST,
                HttpEntity(request, headers),
                responseType,
            )

            // then
            assertAll(
                { assertEquals(HttpStatus.CREATED, response.statusCode) },
                { assertThat(response.body?.data).isNotNull },
            )

            val savedOrder = orderJpaRepository.findById(response.body?.data!!)
            assertThat(savedOrder).isPresent
            assertThat(savedOrder.get().userId).isEqualTo(savedUser.id)

            // 각 상품의 재고 감소 확인
            val stock1After = stockJpaRepository.findByProductId(savedProduct1.id)
            assertThat(stock1After?.quantity).isEqualTo(95) // 100 - 5

            val stock2After = stockJpaRepository.findByProductId(savedProduct2.id)
            assertThat(stock2After?.quantity).isEqualTo(47) // 50 - 3

            val stock3After = stockJpaRepository.findByProductId(savedProduct3.id)
            assertThat(stock3After?.quantity).isEqualTo(23) // 30 - 7
        }
    }

    @DisplayName("토큰 TTL 만료 및 검증 테스트")
    @Nested
    inner class TokenExpiryTest {

        @DisplayName("유효한 토큰으로는 주문 생성 성공, 무효한 토큰으로는 FORBIDDEN")
        @Test
        fun createOrderWithTokenValidation() {
            // given
            val plainPassword = "tokenTestPassword"
            val user = User.create(
                loginId = LoginId.of("tokentest"),
                password = Password.ofEncrypted(passwordEncoder.encode(plainPassword)),
                name = Name.of("토큰 테스트"),
                birthDate = BirthDate.of("20260101"),
                email = Email.of("token@test.com"),
            )
            val savedUser = userJpaRepository.save(user)

            val brand = Brand.create(
                name = "토큰 테스트 브랜드",
                description = "토큰 테스트 브랜드",
            )
            val savedBrand = brandJpaRepository.save(brand)

            val product = Product.create(
                brand = savedBrand,
                name = "토큰 테스트 상품",
                price = BigDecimal("10000"),
                status = ProductStatus.ACTIVE,
            )
            val savedProduct = productJpaRepository.save(product)

            stockJpaRepository.save(
                Stock.create(
                    productId = savedProduct.id,
                    quantity = 100,
                ),
            )

            val request = OrderV1Dto.OrderRequest(
                items = listOf(
                    OrderV1Dto.OrderItemRequest(
                        productId = savedProduct.id,
                        quantity = 1,
                    ),
                ),
            )

            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}

            // 1: 유효한 토큰으로는 성공
            val validToken = java.util.UUID.randomUUID().toString()
            queueRepository.issueToken(QUEUE_NAME, savedUser.id, validToken, 600L) // 충분한 TTL

            val validHeaders = createAuthHeadersWithToken("tokentest", plainPassword, validToken)
            val validResponse = restTemplate.exchange(
                ENDPOINT_ORDERS,
                HttpMethod.POST,
                HttpEntity(request, validHeaders),
                responseType,
            )
            assertThat(validResponse.statusCode).isEqualTo(HttpStatus.CREATED)
            assertThat(validResponse.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS)

            // 2: 무효한/존재하지 않는 토큰으로는 FORBIDDEN
            val invalidToken = java.util.UUID.randomUUID().toString() // 발급되지 않은 토큰
            val invalidHeaders = createAuthHeadersWithToken("tokentest", plainPassword, invalidToken)
            val invalidResponse = restTemplate.exchange(
                ENDPOINT_ORDERS,
                HttpMethod.POST,
                HttpEntity(request, invalidHeaders),
                responseType,
            )

            // 토큰이 Redis에 없으면 FORBIDDEN
            assertThat(invalidResponse.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            assertThat(invalidResponse.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL)
            assertThat(invalidResponse.body?.meta?.errorCode).isEqualTo("Entry Token Invalid")
        }
    }
}

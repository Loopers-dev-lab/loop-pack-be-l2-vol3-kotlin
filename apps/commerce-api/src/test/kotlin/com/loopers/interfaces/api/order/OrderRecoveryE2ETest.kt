package com.loopers.interfaces.api.order

import com.loopers.domain.brand.Brand
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
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.infrastructure.queue.WaitingQueueRegistry
import com.loopers.infrastructure.scheduler.QueueScheduler
import com.loopers.infrastructure.stock.StockJpaRepository
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.interfaces.api.ApiResponse
import com.loopers.testcontainers.RedisTestContainersConfig
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Import
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import java.math.BigDecimal
import kotlin.test.Test

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(RedisTestContainersConfig::class)
class OrderRecoveryE2ETest @Autowired constructor(
    private val restTemplate: TestRestTemplate,
    private val userJpaRepository: UserJpaRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val stockJpaRepository: StockJpaRepository,
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
        val token = java.util.UUID.randomUUID().toString()
        val config = waitingQueueRegistry.getQueueConfig(QUEUE_NAME)
        val ttlSeconds = config?.activeTokenTTLSeconds?.toLong() ?: 300L
        queueRepository.issueToken(QUEUE_NAME, userId, token, ttlSeconds)
        return token
    }

    @Test
    @DisplayName("주문 생성 중 실패하면 재고가 복구된다")
    fun testStockRecoveryOnOrderCreationFailure() {
        // Arrange
        val plainPassword = "password123"

        val user = User.create(
            loginId = LoginId.of("recovery01"),
            password = Password.ofEncrypted(passwordEncoder.encode(plainPassword)),
            name = Name.of("복구 테스트"),
            birthDate = BirthDate.of("20260101"),
            email = Email.of("recovery@test.com"),
        )
        val savedUser = userJpaRepository.save(user)

        val brand = Brand.create(
            name = "복구 테스트 브랜드",
            description = "복구 테스트 브랜드",
        )
        val savedBrand = brandJpaRepository.save(brand)

        // 상품 1: 재고 충분
        val product1 = Product.create(
            brand = savedBrand,
            name = "복구 테스트 상품1",
            price = BigDecimal("10000"),
            status = ProductStatus.ACTIVE,
        )
        val savedProduct1 = productJpaRepository.save(product1)

        val initialStock1 = 100
        stockJpaRepository.save(
            Stock.create(
                productId = savedProduct1.id,
                quantity = initialStock1,
            ),
        )

        // 상품 2: 재고 부족 (주문량 > 재고)
        val product2 = Product.create(
            brand = savedBrand,
            name = "복구 테스트 상품2",
            price = BigDecimal("20000"),
            status = ProductStatus.ACTIVE,
        )
        val savedProduct2 = productJpaRepository.save(product2)

        val initialStock2 = 5
        stockJpaRepository.save(
            Stock.create(
                productId = savedProduct2.id,
                quantity = initialStock2,
            ),
        )

        // Act: 재고 부족 상품이 포함된 주문 시도
        val request = OrderV1Dto.OrderRequest(
            items = listOf(
                OrderV1Dto.OrderItemRequest(
                    productId = savedProduct1.id,
                    quantity = 10,
                ),
                OrderV1Dto.OrderItemRequest(
                    productId = savedProduct2.id,
                    quantity = 10,
                ),
            ),
        )

        val token = getEntryToken("recovery01", savedUser.id)
        val headers = createAuthHeadersWithToken("recovery01", plainPassword, token)
        val responseType = object : ParameterizedTypeReference<ApiResponse<Long>>() {}
        val response = restTemplate.exchange(
            ENDPOINT_ORDERS,
            HttpMethod.POST,
            HttpEntity(request, headers),
            responseType,
        )

        // Assert: 주문 생성 실패 - 정확한 상태 코드와 에러 코드 검증
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL)
        assertThat(response.body?.meta?.errorCode).isEqualTo("Bad Request")

        // 재고 부족 에러일 때만 롤백 검증 실행
        if (response.body?.meta?.errorCode == "Bad Request") {
            // 재고 1은 복구되어야 함 (감소했다가 롤백)
            val stock1After = stockJpaRepository.findByProductId(savedProduct1.id)
            assertThat(stock1After?.quantity).isEqualTo(initialStock1)

            // 재고 2는 변화 없음 (감소 시도도 실패)
            val stock2After = stockJpaRepository.findByProductId(savedProduct2.id)
            assertThat(stock2After?.quantity).isEqualTo(initialStock2)
        }
    }

    @Test
    @DisplayName("주문 생성 성공 시 재고는 복구되지 않는다")
    fun testNoStockRecoveryOnOrderCreationSuccess() {
        // Arrange
        val plainPassword = "password123"

        val user = User.create(
            loginId = LoginId.of("success01"),
            password = Password.ofEncrypted(passwordEncoder.encode(plainPassword)),
            name = Name.of("성공 테스트"),
            birthDate = BirthDate.of("20260101"),
            email = Email.of("success@test.com"),
        )
        val savedUser = userJpaRepository.save(user)

        val brand = Brand.create(
            name = "성공 테스트 브랜드",
            description = "성공 테스트 브랜드",
        )
        val savedBrand = brandJpaRepository.save(brand)

        val product = Product.create(
            brand = savedBrand,
            name = "성공 테스트 상품",
            price = BigDecimal("10000"),
            status = ProductStatus.ACTIVE,
        )
        val savedProduct = productJpaRepository.save(product)

        val initialStock = 100
        stockJpaRepository.save(
            Stock.create(
                productId = savedProduct.id,
                quantity = initialStock,
            ),
        )

        // Act: 정상 주문
        val request = OrderV1Dto.OrderRequest(
            items = listOf(
                OrderV1Dto.OrderItemRequest(
                    productId = savedProduct.id,
                    quantity = 10,
                ),
            ),
        )

        val token = getEntryToken("success01", savedUser.id)
        val headers = createAuthHeadersWithToken("success01", plainPassword, token)
        val responseType = object : ParameterizedTypeReference<ApiResponse<Long>>() {}
        val response = restTemplate.exchange(
            ENDPOINT_ORDERS,
            HttpMethod.POST,
            HttpEntity(request, headers),
            responseType,
        )

        // Assert: 주문 성공
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)

        // 재고는 감소한 상태 (복구 안 됨)
        val stockAfter = stockJpaRepository.findByProductId(savedProduct.id)
        assertThat(stockAfter?.quantity).isEqualTo(initialStock - 10)
    }

    @Test
    @DisplayName("토큰 없이 주문하면 UNAUTHORIZED 응답을 받는다")
    fun testOrderCreationWithoutToken() {
        // Arrange
        val plainPassword = "password123"

        val user = User.create(
            loginId = LoginId.of("notoken01"),
            password = Password.ofEncrypted(passwordEncoder.encode(plainPassword)),
            name = Name.of("토큰 없음 테스트"),
            birthDate = BirthDate.of("20260101"),
            email = Email.of("notoken@test.com"),
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

        // Act: 토큰 없이 요청
        val request = OrderV1Dto.OrderRequest(
            items = listOf(
                OrderV1Dto.OrderItemRequest(
                    productId = savedProduct.id,
                    quantity = 10,
                ),
            ),
        )

        val headers = createAuthHeaders("notoken01", plainPassword) // 토큰 헤더 없음
        val responseType = object : ParameterizedTypeReference<ApiResponse<Long>>() {}
        val response = restTemplate.exchange(
            ENDPOINT_ORDERS,
            HttpMethod.POST,
            HttpEntity(request, headers),
            responseType,
        )

        // Assert: 토큰 미포함으로 인한 실패
        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL)
        assertThat(response.body?.meta?.errorCode).isEqualTo("Entry Token Missing")
    }

    @Test
    @DisplayName("유효하지 않은 토큰으로 주문하면 UNAUTHORIZED 응답을 받는다")
    fun testOrderCreationWithInvalidToken() {
        // Arrange
        val plainPassword = "password123"

        val user = User.create(
            loginId = LoginId.of("invalid01"),
            password = Password.ofEncrypted(passwordEncoder.encode(plainPassword)),
            name = Name.of("유효하지 않은 토큰 테스트"),
            birthDate = BirthDate.of("20260101"),
            email = Email.of("invalid@test.com"),
        )
        val savedUser = userJpaRepository.save(user)

        val brand = Brand.create(
            name = "유효하지 않은 토큰 테스트 브랜드",
            description = "유효하지 않은 토큰 테스트 브랜드",
        )
        val savedBrand = brandJpaRepository.save(brand)

        val product = Product.create(
            brand = savedBrand,
            name = "유효하지 않은 토큰 테스트 상품",
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

        // Act: 유효하지 않은 토큰으로 요청
        val request = OrderV1Dto.OrderRequest(
            items = listOf(
                OrderV1Dto.OrderItemRequest(
                    productId = savedProduct.id,
                    quantity = 10,
                ),
            ),
        )

        val invalidToken = java.util.UUID.randomUUID().toString() // 발급되지 않은 토큰
        val headers = createAuthHeadersWithToken("invalid01", plainPassword, invalidToken)
        val responseType = object : ParameterizedTypeReference<ApiResponse<Long>>() {}
        val response = restTemplate.exchange(
            ENDPOINT_ORDERS,
            HttpMethod.POST,
            HttpEntity(request, headers),
            responseType,
        )

        // Assert: 유효하지 않은 토큰으로 인한 실패
        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL)
        assertThat(response.body?.meta?.errorCode).isEqualTo("Entry Token Invalid")
    }
}

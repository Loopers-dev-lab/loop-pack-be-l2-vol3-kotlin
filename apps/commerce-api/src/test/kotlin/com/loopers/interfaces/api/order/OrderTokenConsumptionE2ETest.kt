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
@DisplayName("OrderTokenConsumptionE2ETest - 토큰 소비 및 재사용 방지")
class OrderTokenConsumptionE2ETest @Autowired constructor(
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

    private fun createAuthHeadersWithToken(loginId: String, password: String, token: String): HttpHeaders {
        return HttpHeaders().apply {
            set("X-Loopers-LoginId", loginId)
            set("X-Loopers-LoginPw", password)
            set("X-Entry-Token", token)
        }
    }

    private fun getEntryToken(userId: Long): String {
        val token = java.util.UUID.randomUUID().toString()
        val config = waitingQueueRegistry.getQueueConfig(QUEUE_NAME)
        val ttlSeconds = config?.activeTokenTTLSeconds?.toLong() ?: 300L
        queueRepository.issueToken(QUEUE_NAME, userId, token, ttlSeconds)
        return token
    }

    @DisplayName("유효한 토큰으로 주문 생성 실패 시 토큰이 소비되어 재요청 시 거절된다")
    @Test
    fun `토큰으로 주문 생성 실패 후 동일 토큰 재요청 시 ENTRY_TOKEN_INVALID 받는다`() {
        // arrange: 사용자 및 상품 설정
        val plainPassword = "password123"
        val user = User.create(
            loginId = LoginId.of("tokenconsumption"),
            password = Password.ofEncrypted(passwordEncoder.encode(plainPassword)),
            name = Name.of("토큰 소비 테스트"),
            birthDate = BirthDate.of("20260101"),
            email = Email.of("consumption@test.com"),
        )
        val savedUser = userJpaRepository.save(user)

        val brand = Brand.create(
            name = "테스트 브랜드",
            description = "테스트 브랜드",
        )
        val savedBrand = brandJpaRepository.save(brand)

        val product = Product.create(
            brand = savedBrand,
            name = "테스트 상품",
            price = BigDecimal("10000"),
            status = ProductStatus.ACTIVE,
        )
        val savedProduct = productJpaRepository.save(product)

        // 재고 부족 상황 설정 (주문량 > 재고)
        stockJpaRepository.save(
            Stock.create(
                productId = savedProduct.id,
                quantity = 5,
            ),
        )

        val token = getEntryToken(savedUser.id)

        // act 1: 유효한 토큰으로 첫 번째 요청 (재고 부족으로 실패)
        // 재고(5)보다 많음 - 실패
        val request = OrderV1Dto.OrderRequest(
            items = listOf(
                OrderV1Dto.OrderItemRequest(
                    productId = savedProduct.id,
                    quantity = 10,
                ),
            ),
        )

        val headers = createAuthHeadersWithToken("tokenconsumption", plainPassword, token)
        val responseType = object : ParameterizedTypeReference<ApiResponse<Long>>() {}

        val firstResponse = restTemplate.exchange(
            ENDPOINT_ORDERS,
            HttpMethod.POST,
            HttpEntity(request, headers),
            responseType,
        )

        // assert 1: 첫 번째 요청이 주문 생성 실패로 거절됨
        assertThat(firstResponse.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(firstResponse.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL)

        // act 2: 동일 토큰으로 재요청 (이번엔 재고 충분)
        // 재고(5) 이내
        val retryRequest = OrderV1Dto.OrderRequest(
            items = listOf(
                OrderV1Dto.OrderItemRequest(
                    productId = savedProduct.id,
                    quantity = 1,
                ),
            ),
        )

        val secondResponse = restTemplate.exchange(
            ENDPOINT_ORDERS,
            HttpMethod.POST,
            HttpEntity(retryRequest, headers),
            responseType,
        )

        // assert 2: 토큰이 이미 소비되었으므로 두 번째 요청도 거절되어야 함
        // (토큰이 남아있으면 요청이 성공하는 문제 발생)
        assertThat(secondResponse.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        assertThat(secondResponse.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL)
        assertThat(secondResponse.body?.meta?.errorCode).isEqualTo("Entry Token Invalid")
    }

    @DisplayName("주문 생성 성공 시에도 토큰이 소비되어 재요청 시 거절된다")
    @Test
    fun `토큰으로 주문 생성 성공 후 동일 토큰 재요청 시 ENTRY_TOKEN_INVALID 받는다`() {
        // arrange: 사용자 및 상품 설정
        val plainPassword = "password123"
        val user = User.create(
            loginId = LoginId.of("successtoken"),
            password = Password.ofEncrypted(passwordEncoder.encode(plainPassword)),
            name = Name.of("성공 토큰 테스트"),
            birthDate = BirthDate.of("20260101"),
            email = Email.of("successtoken@test.com"),
        )
        val savedUser = userJpaRepository.save(user)

        val brand = Brand.create(
            name = "테스트 브랜드",
            description = "테스트 브랜드",
        )
        val savedBrand = brandJpaRepository.save(brand)

        val product = Product.create(
            brand = savedBrand,
            name = "테스트 상품",
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

        val token = getEntryToken(savedUser.id)

        // act 1: 유효한 토큰으로 주문 생성 (성공)
        val request = OrderV1Dto.OrderRequest(
            items = listOf(
                OrderV1Dto.OrderItemRequest(
                    productId = savedProduct.id,
                    quantity = 10,
                ),
            ),
        )

        val headers = createAuthHeadersWithToken("successtoken", plainPassword, token)
        val responseType = object : ParameterizedTypeReference<ApiResponse<Long>>() {}

        val firstResponse = restTemplate.exchange(
            ENDPOINT_ORDERS,
            HttpMethod.POST,
            HttpEntity(request, headers),
            responseType,
        )

        // assert 1: 첫 번째 요청이 성공
        assertThat(firstResponse.statusCode).isEqualTo(HttpStatus.CREATED)

        // act 2: 동일 토큰으로 재요청
        val secondResponse = restTemplate.exchange(
            ENDPOINT_ORDERS,
            HttpMethod.POST,
            HttpEntity(request, headers),
            responseType,
        )

        // assert 2: 토큰이 이미 소비되었으므로 재요청도 거절되어야 함
        assertThat(secondResponse.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        assertThat(secondResponse.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL)
        assertThat(secondResponse.body?.meta?.errorCode).isEqualTo("Entry Token Invalid")
    }
}

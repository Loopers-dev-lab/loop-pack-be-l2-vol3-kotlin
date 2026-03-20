package com.loopers.interfaces.api.user.payment

import com.loopers.application.user.order.OrderCreateCommand
import com.loopers.application.user.order.OrderCreateUseCase
import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.Money
import com.loopers.domain.common.Quantity
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductStock
import com.loopers.domain.product.ProductStockRepository
import com.loopers.domain.user.User
import com.loopers.domain.user.UserPasswordHasher
import com.loopers.domain.user.UserRepository
import com.loopers.domain.payment.PgPaymentPort
import com.loopers.domain.payment.PgPaymentResponse
import com.loopers.interfaces.api.ApiResponse
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.kotlin.check
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@DisplayName("POST /api/v1/payments - 결제 요청 E2E")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserPaymentV1CreateE2ETest
@Autowired
constructor(
    private val testRestTemplate: TestRestTemplate,
    private val userRepository: UserRepository,
    private val brandRepository: BrandRepository,
    private val productRepository: ProductRepository,
    private val productStockRepository: ProductStockRepository,
    private val orderCreateUseCase: OrderCreateUseCase,
    private val passwordHasher: UserPasswordHasher,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @MockitoBean
    private lateinit var pgPaymentPort: PgPaymentPort
    companion object {
        private const val ADMIN = "loopers.admin"
        private const val LOGIN_ID = "testuser1"
        private const val PASSWORD = "Password1!"
        private const val ENDPOINT = "/api/v1/payments"
    }

    private var orderId: Long = 0

    @BeforeEach
    fun setUp() {
        given(
            pgPaymentPort.requestPayment(
                check { },
            ),
        ).willReturn(PgPaymentResponse.Accepted("txn-e2e-test"))
        given(pgPaymentPort.isAvailable()).willReturn(true)

        val user = User.register(
            loginId = LOGIN_ID,
            rawPassword = PASSWORD,
            name = "홍길동",
            birthDate = LocalDate.of(1990, 1, 1),
            email = "test@example.com",
            passwordHasher = passwordHasher,
        )
        userRepository.save(user)

        val brand = brandRepository.save(Brand.register(name = "나이키"), ADMIN)
        val activeBrand = brandRepository.save(brand.update("나이키", "ACTIVE"), ADMIN)

        val product = Product.register(
            name = "테스트 상품",
            regularPrice = Money(BigDecimal.valueOf(10000)),
            sellingPrice = Money(BigDecimal.valueOf(8000)),
            brandId = activeBrand.id!!,
        )
        val saved = productRepository.save(product, ADMIN)
        val activeProduct = productRepository.save(saved.activate(), ADMIN)
        val productId = activeProduct.id!!

        productStockRepository.save(
            ProductStock.create(productId = productId, initialQuantity = Quantity(100)),
            ADMIN,
        )

        val result = orderCreateUseCase.create(
            OrderCreateCommand(
                userId = 1L,
                idempotencyKey = UUID.randomUUID().toString(),
                items = listOf(OrderCreateCommand.Item(productId = productId, quantity = 2)),
            ),
        )
        orderId = result.orderId
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createRequest(
        idempotencyKey: String = UUID.randomUUID().toString(),
        body: UserPaymentV1Request.Create = createBody(),
    ): HttpEntity<UserPaymentV1Request.Create> {
        val headers = HttpHeaders().apply {
            set("X-Loopers-LoginId", LOGIN_ID)
            set("X-Loopers-LoginPw", PASSWORD)
            set("X-Payment-Idempotency-Key", idempotencyKey)
            contentType = MediaType.APPLICATION_JSON
        }
        return HttpEntity(body, headers)
    }

    private fun createBody(
        orderId: Long = this.orderId,
        cardType: String = "VISA",
        cardNo: String = "4111111111111234",
    ): UserPaymentV1Request.Create = UserPaymentV1Request.Create(
        orderId = orderId,
        cardType = cardType,
        cardNo = cardNo,
    )

    @Nested
    @DisplayName("정상 결제 요청 시")
    inner class WhenCreateSuccess {

        @Test
        @DisplayName("201 Created와 Payment 정보를 반환한다")
        fun create_success_returns201() {
            // act
            val response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                createRequest(),
                object : ParameterizedTypeReference<ApiResponse<Map<String, Any?>>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            assertThat(response.body?.meta?.result?.name).isEqualTo("SUCCESS")
            val data = response.body?.data!!
            assertThat(data["paymentId"]).isNotNull()
            assertThat(data["status"]).isEqualTo("PENDING")
            assertThat(data["displayStatus"]).isEqualTo("AWAITING_PAYMENT_RESULT")
            assertThat(data["transactionKey"]).isEqualTo("txn-e2e-test")
        }
    }

    @Nested
    @DisplayName("동일 멱등키 + 동일 요청 재전송 시")
    inner class WhenIdempotentReplay {

        @Test
        @DisplayName("200 OK와 기존 Payment를 반환한다")
        fun create_replay_returns200() {
            // arrange
            val idempotencyKey = UUID.randomUUID().toString()
            testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                createRequest(idempotencyKey = idempotencyKey),
                object : ParameterizedTypeReference<ApiResponse<Map<String, Any?>>>() {},
            )

            // act
            val response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                createRequest(idempotencyKey = idempotencyKey),
                object : ParameterizedTypeReference<ApiResponse<Map<String, Any?>>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body?.meta?.result?.name).isEqualTo("SUCCESS")
        }
    }

    @Nested
    @DisplayName("동일 멱등키 + 다른 요청 시")
    inner class WhenIdempotencyConflict {

        @Test
        @DisplayName("409 Conflict를 반환한다")
        fun create_conflict_returns409() {
            // arrange
            val idempotencyKey = UUID.randomUUID().toString()
            testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                createRequest(
                    idempotencyKey = idempotencyKey,
                    body = createBody(cardType = "VISA", cardNo = "4111111111111234"),
                ),
                object : ParameterizedTypeReference<ApiResponse<Any?>>() {},
            )

            // act
            val response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                createRequest(
                    idempotencyKey = idempotencyKey,
                    body = createBody(cardType = "MASTER", cardNo = "5500000000001234"),
                ),
                object : ParameterizedTypeReference<ApiResponse<Any?>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
            assertThat(response.body?.meta?.errorCode).isEqualTo("PAYMENT_IDEMPOTENCY_CONFLICT")
        }
    }

    @Nested
    @DisplayName("멱등성 키 헤더 누락 시")
    inner class WhenMissingIdempotencyKey {

        @Test
        @DisplayName("400을 반환한다")
        fun create_missingKey_returns400() {
            // act
            val headers = HttpHeaders().apply {
                set("X-Loopers-LoginId", LOGIN_ID)
                set("X-Loopers-LoginPw", PASSWORD)
                contentType = MediaType.APPLICATION_JSON
            }
            val entity = HttpEntity(createBody(), headers)
            val response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                entity,
                object : ParameterizedTypeReference<ApiResponse<Any?>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @Nested
    @DisplayName("존재하지 않는 주문으로 결제 시")
    inner class WhenOrderNotFound {

        @Test
        @DisplayName("404를 반환한다")
        fun create_orderNotFound_returns404() {
            // act
            val response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                createRequest(body = createBody(orderId = 999999L)),
                object : ParameterizedTypeReference<ApiResponse<Any?>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
            assertThat(response.body?.meta?.errorCode).isEqualTo("ORDER_NOT_FOUND")
        }
    }
}

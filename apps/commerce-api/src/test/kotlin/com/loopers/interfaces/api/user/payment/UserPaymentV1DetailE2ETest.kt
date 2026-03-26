package com.loopers.interfaces.api.user.payment

import com.loopers.application.user.order.OrderCreateCommand
import com.loopers.application.user.order.OrderCreateUseCase
import com.loopers.application.user.payment.PaymentCreateUseCase
import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.Money
import com.loopers.domain.common.Quantity
import com.loopers.domain.payment.Payment
import com.loopers.domain.payment.PaymentIdempotencyKey
import com.loopers.domain.payment.PaymentRepository
import com.loopers.domain.payment.PgPaymentPort
import com.loopers.domain.payment.PgPaymentStatusResponse
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductStock
import com.loopers.domain.product.ProductStockRepository
import com.loopers.domain.user.User
import com.loopers.domain.user.UserPasswordHasher
import com.loopers.domain.user.UserRepository
import com.loopers.interfaces.api.ApiResponse
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.reset
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@DisplayName("GET /api/v1/payments/{paymentId} - 결제 상세 E2E")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserPaymentV1DetailE2ETest
@Autowired
constructor(
    private val testRestTemplate: TestRestTemplate,
    private val userRepository: UserRepository,
    private val brandRepository: BrandRepository,
    private val productRepository: ProductRepository,
    private val productStockRepository: ProductStockRepository,
    private val orderCreateUseCase: OrderCreateUseCase,
    private val paymentRepository: PaymentRepository,
    private val passwordHasher: UserPasswordHasher,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @MockitoBean
    private lateinit var pgPaymentPort: PgPaymentPort

    companion object {
        private const val ADMIN = "loopers.admin"
        private const val LOGIN_ID = "testuser1"
        private const val PASSWORD = "Password1!"
        private const val OTHER_LOGIN_ID = "testuser2"
        private const val OTHER_PASSWORD = "Password2!"
        private const val ENDPOINT = "/api/v1/payments"
        private const val CARD_TYPE = "VISA"
        private const val CARD_NO = "4111111111111234"
        private const val AMOUNT = "16000"
    }

    private var userId: Long = 0
    private var otherUserId: Long = 0
    private var orderId: Long = 0

    @BeforeEach
    fun setUp() {
        reset(pgPaymentPort)
        given(pgPaymentPort.isAvailable()).willReturn(true)

        val savedUser = userRepository.save(
            User.register(
                loginId = LOGIN_ID,
                rawPassword = PASSWORD,
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 1),
                email = "test@example.com",
                passwordHasher = passwordHasher,
            ),
        )
        userId = savedUser.id!!

        val savedOtherUser = userRepository.save(
            User.register(
                loginId = OTHER_LOGIN_ID,
                rawPassword = OTHER_PASSWORD,
                name = "김철수",
                birthDate = LocalDate.of(1991, 2, 2),
                email = "other@example.com",
                passwordHasher = passwordHasher,
            ),
        )
        otherUserId = savedOtherUser.id!!

        val brand = brandRepository.save(Brand.register(name = "나이키"), ADMIN)
        val activeBrand = brandRepository.save(brand.update("나이키", "ACTIVE"), ADMIN)
        val product = Product.register(
            name = "테스트 상품",
            regularPrice = Money(BigDecimal.valueOf(10000)),
            sellingPrice = Money(BigDecimal.valueOf(8000)),
            brandId = activeBrand.id!!,
        )
        val savedProduct = productRepository.save(product, ADMIN)
        val activeProduct = productRepository.save(savedProduct.activate(), ADMIN)

        productStockRepository.save(
            ProductStock.create(productId = activeProduct.id!!, initialQuantity = Quantity(100)),
            ADMIN,
        )

        orderId = orderCreateUseCase.create(
            OrderCreateCommand(
                userId = userId,
                idempotencyKey = UUID.randomUUID().toString(),
                items = listOf(OrderCreateCommand.Item(productId = activeProduct.id!!, quantity = 2)),
            ),
        ).orderId
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createPayment(transactionKey: String?): Payment {
        val payment = Payment.create(
            orderId = orderId,
            userId = userId,
            idempotencyKey = PaymentIdempotencyKey(UUID.randomUUID().toString()),
            cardType = CARD_TYPE,
            maskedCardNo = PaymentCreateUseCase.maskCardNo(CARD_NO),
            amount = Money(BigDecimal(AMOUNT)),
            requestFingerprint = UUID.randomUUID().toString(),
        )
        val saved = paymentRepository.save(payment)
        return if (transactionKey != null) {
            paymentRepository.save(saved.updateTransactionKey(transactionKey))
        } else {
            paymentRepository.save(saved.applyTimeoutFallback())
        }
    }

    private fun requestHeaders(
        loginId: String = LOGIN_ID,
        password: String = PASSWORD,
    ): HttpHeaders = HttpHeaders().apply {
        set("X-Loopers-LoginId", loginId)
        set("X-Loopers-LoginPw", password)
    }

    @Nested
    @DisplayName("PENDING + transactionKey 있음")
    inner class PendingWithTransactionKey {

        @Test
        @DisplayName("PG SUCCESS면 read-repair 후 SUCCESS를 반환한다")
        fun detail_successAfterReadRepair() {
            val payment = createPayment(transactionKey = "txn-detail-success")
            given(pgPaymentPort.queryPaymentStatus("txn-detail-success", userId))
                .willReturn(PgPaymentStatusResponse("txn-detail-success", "SUCCESS", null))

            val response = testRestTemplate.exchange(
                "$ENDPOINT/${payment.id}",
                HttpMethod.GET,
                HttpEntity<Void>(requestHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Map<String, Any?>>>() {},
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val data = response.body?.data!!
            assertThat(data["status"]).isEqualTo("SUCCESS")
            assertThat(data["displayStatus"]).isEqualTo("ORDER_CONFIRMED")

            val savedPayment = paymentRepository.findById(payment.id!!)!!
            assertThat(savedPayment.status).isEqualTo(Payment.Status.SUCCESS)
        }

        @Test
        @DisplayName("PG FAILED면 read-repair 후 FAILED를 반환한다")
        fun detail_failedAfterReadRepair() {
            val payment = createPayment(transactionKey = "txn-detail-failed")
            given(pgPaymentPort.queryPaymentStatus("txn-detail-failed", userId))
                .willReturn(PgPaymentStatusResponse("txn-detail-failed", "FAILED", "한도초과"))

            val response = testRestTemplate.exchange(
                "$ENDPOINT/${payment.id}",
                HttpMethod.GET,
                HttpEntity<Void>(requestHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Map<String, Any?>>>() {},
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val data = response.body?.data!!
            assertThat(data["status"]).isEqualTo("FAILED")
            assertThat(data["displayStatus"]).isEqualTo("REQUIRES_REPAYMENT")
            assertThat(data["reasonCode"]).isEqualTo("LIMIT_EXCEEDED")

            val savedPayment = paymentRepository.findById(payment.id!!)!!
            assertThat(savedPayment.status).isEqualTo(Payment.Status.FAILED)
        }
    }

    @Nested
    @DisplayName("PENDING + transactionKey 없음")
    inner class PendingWithoutTransactionKey {

        @Test
        @DisplayName("PG 조회 없이 기존 PENDING을 반환한다")
        fun detail_staysPending() {
            val payment = createPayment(transactionKey = null)

            val response = testRestTemplate.exchange(
                "$ENDPOINT/${payment.id}",
                HttpMethod.GET,
                HttpEntity<Void>(requestHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Map<String, Any?>>>() {},
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val data = response.body?.data!!
            assertThat(data["status"]).isEqualTo("PENDING")
            assertThat(data["transactionKey"]).isNull()
            assertThat(data["displayStatus"]).isEqualTo("AWAITING_PAYMENT_RESULT")

            val savedPayment = paymentRepository.findById(payment.id!!)!!
            assertThat(savedPayment.status).isEqualTo(Payment.Status.PENDING)
            assertThat(savedPayment.transactionKey).isNull()
        }
    }
}

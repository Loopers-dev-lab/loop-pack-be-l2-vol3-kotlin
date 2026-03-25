package com.loopers.interfaces.api.user.payment

import com.loopers.application.user.order.OrderCreateCommand
import com.loopers.application.user.order.OrderCreateUseCase
import com.loopers.application.user.payment.PaymentCreateUseCase
import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.Money
import com.loopers.domain.common.Quantity
import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderStatusQueryRepository
import com.loopers.domain.payment.Payment
import com.loopers.domain.payment.PaymentIdempotencyKey
import com.loopers.domain.payment.PaymentRepository
import com.loopers.domain.payment.PgPaymentOrderResponse
import com.loopers.domain.payment.PgPaymentPort
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

@DisplayName("POST /api/v1/payments/{paymentId}/reconcile - 결제 수동 복구 E2E")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserPaymentV1ReconcileE2ETest
@Autowired
constructor(
    private val testRestTemplate: TestRestTemplate,
    private val userRepository: UserRepository,
    private val brandRepository: BrandRepository,
    private val productRepository: ProductRepository,
    private val productStockRepository: ProductStockRepository,
    private val orderCreateUseCase: OrderCreateUseCase,
    private val orderStatusQueryRepository: OrderStatusQueryRepository,
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

        userRepository.save(
            User.register(
                loginId = OTHER_LOGIN_ID,
                rawPassword = OTHER_PASSWORD,
                name = "김철수",
                birthDate = LocalDate.of(1991, 2, 2),
                email = "other@example.com",
                passwordHasher = passwordHasher,
            ),
        )

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

    private fun createTimeoutPayment(): Payment {
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
        return paymentRepository.save(saved.applyTimeoutFallback())
    }

    private fun requestHeaders(
        loginId: String = LOGIN_ID,
        password: String = PASSWORD,
    ): HttpHeaders = HttpHeaders().apply {
        set("X-Loopers-LoginId", loginId)
        set("X-Loopers-LoginPw", password)
    }

    private fun pgOrderResponse(vararg transactions: PgPaymentOrderResponse.Transaction): PgPaymentOrderResponse =
        PgPaymentOrderResponse(
            orderId = orderId.toString(),
            transactions = transactions.toList(),
        )

    private fun transaction(
        transactionKey: String,
        status: String,
        reason: String? = null,
        cardNo: String = CARD_NO,
    ): PgPaymentOrderResponse.Transaction = PgPaymentOrderResponse.Transaction(
        transactionKey = transactionKey,
        orderId = orderId.toString(),
        cardType = CARD_TYPE,
        cardNo = cardNo,
        amount = 16000L,
        status = status,
        reason = reason,
    )

    @Nested
    @DisplayName("단건 매칭 가능할 때")
    inner class SingleCandidate {

        @Test
        @DisplayName("SUCCESS면 결제 성공과 주문 확정을 반영한다")
        fun reconcile_success() {
            val payment = createTimeoutPayment()
            given(pgPaymentPort.queryPaymentsByOrderId(orderId, userId)).willReturn(
                pgOrderResponse(transaction(transactionKey = "txn-reconcile-success", status = "SUCCESS")),
            )

            val response = testRestTemplate.exchange(
                "$ENDPOINT/${payment.id}/reconcile",
                HttpMethod.POST,
                HttpEntity<Void>(requestHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Map<String, Any?>>>() {},
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val data = response.body?.data!!
            assertThat(data["status"]).isEqualTo("SUCCESS")
            assertThat(data["reconcileStatus"]).isEqualTo("RESOLVED_SUCCESS")
            assertThat(data["displayStatus"]).isEqualTo("ORDER_CONFIRMED")

            val savedPayment = paymentRepository.findById(payment.id!!)!!
            assertThat(savedPayment.status).isEqualTo(Payment.Status.SUCCESS)
            val savedOrderStatus = orderStatusQueryRepository.findStatusById(orderId)
            assertThat(savedOrderStatus).isEqualTo(Order.Status.CREATED)
        }

        @Test
        @DisplayName("PENDING이면 transactionKey를 저장하고 PENDING을 유지한다")
        fun reconcile_stillPending() {
            val payment = createTimeoutPayment()
            given(pgPaymentPort.queryPaymentsByOrderId(orderId, userId)).willReturn(
                pgOrderResponse(transaction(transactionKey = "txn-reconcile-pending", status = "PENDING")),
            )

            val response = testRestTemplate.exchange(
                "$ENDPOINT/${payment.id}/reconcile",
                HttpMethod.POST,
                HttpEntity<Void>(requestHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Map<String, Any?>>>() {},
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val data = response.body?.data!!
            assertThat(data["status"]).isEqualTo("PENDING")
            assertThat(data["reconcileStatus"]).isEqualTo("STILL_PENDING")
            assertThat(data["transactionKey"]).isEqualTo("txn-reconcile-pending")

            val savedPayment = paymentRepository.findById(payment.id!!)!!
            assertThat(savedPayment.status).isEqualTo(Payment.Status.PENDING)
            assertThat(savedPayment.transactionKey).isEqualTo("txn-reconcile-pending")
            assertThat(savedPayment.reasonCode).isNull()
        }
    }

    @Nested
    @DisplayName("매칭이 애매하거나 접근 권한이 없을 때")
    inner class AmbiguousOrNotOwned {

        @Test
        @DisplayName("후보가 여러 건이면 상태를 바꾸지 않고 AMBIGUOUS를 반환한다")
        fun reconcile_ambiguous() {
            val payment = createTimeoutPayment()
            given(pgPaymentPort.queryPaymentsByOrderId(orderId, userId)).willReturn(
                pgOrderResponse(
                    transaction(transactionKey = "txn-a", status = "SUCCESS"),
                    transaction(transactionKey = "txn-b", status = "SUCCESS"),
                ),
            )

            val response = testRestTemplate.exchange(
                "$ENDPOINT/${payment.id}/reconcile",
                HttpMethod.POST,
                HttpEntity<Void>(requestHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Map<String, Any?>>>() {},
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val data = response.body?.data!!
            assertThat(data["status"]).isEqualTo("PENDING")
            assertThat(data["reconcileStatus"]).isEqualTo("AMBIGUOUS")
            assertThat(data["transactionKey"]).isNull()

            val savedPayment = paymentRepository.findById(payment.id!!)!!
            assertThat(savedPayment.status).isEqualTo(Payment.Status.PENDING)
            assertThat(savedPayment.transactionKey).isNull()
        }

        @Test
        @DisplayName("본인 결제가 아니면 404를 반환한다")
        fun reconcile_notOwned() {
            val payment = createTimeoutPayment()

            val response = testRestTemplate.exchange(
                "$ENDPOINT/${payment.id}/reconcile",
                HttpMethod.POST,
                HttpEntity<Void>(requestHeaders(loginId = OTHER_LOGIN_ID, password = OTHER_PASSWORD)),
                object : ParameterizedTypeReference<ApiResponse<Any?>>() {},
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
            assertThat(response.body?.meta?.errorCode).isEqualTo("PAYMENT_NOT_FOUND")
        }
    }
}

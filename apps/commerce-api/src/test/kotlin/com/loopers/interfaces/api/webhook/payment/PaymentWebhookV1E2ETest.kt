package com.loopers.interfaces.api.webhook.payment

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
import com.loopers.domain.payment.PaymentReasonCode
import com.loopers.domain.payment.PaymentRepository
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductStock
import com.loopers.domain.product.ProductStockRepository
import com.loopers.domain.user.User
import com.loopers.domain.user.UserPasswordHasher
import com.loopers.domain.user.UserRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@DisplayName("POST /webhook/v1/payments/{paymentId} - 결제 콜백 E2E")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentWebhookV1E2ETest
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
    companion object {
        private const val ADMIN = "loopers.admin"
        private const val LOGIN_ID = "testuser1"
        private const val PASSWORD = "Password1!"
        private const val ENDPOINT = "/webhook/v1/payments"
        private const val CARD_TYPE = "VISA"
        private const val CARD_NO = "4111111111111234"
        private const val AMOUNT = "16000"
    }

    private var userId: Long = 0
    private var orderId: Long = 0

    @BeforeEach
    fun setUp() {
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

    private fun createPendingPayment(transactionKey: String?): Payment {
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

    private fun requestEntity(
        transactionKey: String,
        status: String,
        reason: String? = null,
    ): HttpEntity<PaymentWebhookV1Request.Callback> {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }
        val body = PaymentWebhookV1Request.Callback(
            transactionKey = transactionKey,
            status = status,
            reason = reason,
        )
        return HttpEntity(body, headers)
    }

    @Nested
    @DisplayName("callback 결과 반영")
    inner class CallbackResult {

        @Test
        @DisplayName("SUCCESS callback이면 Payment=SUCCESS, Order=CREATED")
        fun webhook_success() {
            val payment = createPendingPayment(transactionKey = "txn-webhook-success")

            val response = testRestTemplate.exchange(
                "$ENDPOINT/${payment.id}",
                HttpMethod.POST,
                requestEntity(transactionKey = "txn-webhook-success", status = "SUCCESS"),
                Void::class.java,
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)

            val savedPayment = paymentRepository.findById(payment.id!!)!!
            assertThat(savedPayment.status).isEqualTo(Payment.Status.SUCCESS)
            val savedOrderStatus = orderStatusQueryRepository.findStatusById(orderId)
            assertThat(savedOrderStatus).isEqualTo(Order.Status.CREATED)
        }

        @Test
        @DisplayName("FAILED callback이면 Payment=FAILED, Order=PENDING")
        fun webhook_failed() {
            val payment = createPendingPayment(transactionKey = null)

            val response = testRestTemplate.exchange(
                "$ENDPOINT/${payment.id}",
                HttpMethod.POST,
                requestEntity(transactionKey = "txn-webhook-failed", status = "FAILED", reason = "한도초과"),
                Void::class.java,
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)

            val savedPayment = paymentRepository.findById(payment.id!!)!!
            assertThat(savedPayment.status).isEqualTo(Payment.Status.FAILED)
            assertThat(savedPayment.transactionKey).isEqualTo("txn-webhook-failed")
            assertThat(savedPayment.reasonCode).isEqualTo(PaymentReasonCode.LIMIT_EXCEEDED)
            val savedOrderStatus = orderStatusQueryRepository.findStatusById(orderId)
            assertThat(savedOrderStatus).isEqualTo(Order.Status.PENDING)
        }
    }

    @Nested
    @DisplayName("중복 callback")
    inner class DuplicateCallback {

        @Test
        @DisplayName("이미 terminal 상태면 200 OK + no-op")
        fun webhook_duplicateNoOp() {
            val payment = createPendingPayment(transactionKey = "txn-webhook-duplicate")

            val firstResponse = testRestTemplate.exchange(
                "$ENDPOINT/${payment.id}",
                HttpMethod.POST,
                requestEntity(transactionKey = "txn-webhook-duplicate", status = "SUCCESS"),
                Void::class.java,
            )

            assertThat(firstResponse.statusCode).isEqualTo(HttpStatus.OK)

            val response = testRestTemplate.exchange(
                "$ENDPOINT/${payment.id}",
                HttpMethod.POST,
                requestEntity(transactionKey = "txn-webhook-duplicate", status = "SUCCESS"),
                Void::class.java,
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)

            val savedPayment = paymentRepository.findById(payment.id!!)!!
            assertThat(savedPayment.status).isEqualTo(Payment.Status.SUCCESS)
            assertThat(savedPayment.transactionKey).isEqualTo("txn-webhook-duplicate")
            val savedOrderStatus = orderStatusQueryRepository.findStatusById(orderId)
            assertThat(savedOrderStatus).isEqualTo(Order.Status.CREATED)
        }
    }
}

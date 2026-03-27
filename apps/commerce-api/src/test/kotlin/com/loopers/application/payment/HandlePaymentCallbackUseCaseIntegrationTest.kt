package com.loopers.application.payment

import com.loopers.application.brand.RegisterBrandCommand
import com.loopers.application.brand.RegisterBrandUseCase
import com.loopers.application.order.CreateOrderCommand
import com.loopers.application.order.CreateOrderUseCase
import com.loopers.application.order.OrderItemCommand
import com.loopers.application.product.RegisterProductCommand
import com.loopers.application.product.RegisterProductUseCase
import com.loopers.application.user.RegisterUserCommand
import com.loopers.application.user.RegisterUserUseCase
import com.loopers.domain.order.OrderRepository
import com.loopers.domain.order.OrderStatus
import com.loopers.domain.payment.PaymentRepository
import com.loopers.domain.payment.PaymentStatus
import com.loopers.testcontainers.MySqlTestContainersConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql

@SpringBootTest
@Import(MySqlTestContainersConfig::class)
@Sql(
    statements = [
        "DELETE FROM payments",
        "DELETE FROM order_item",
        "DELETE FROM orders",
        "DELETE FROM likes",
        "DELETE FROM product_image",
        "DELETE FROM product",
        "DELETE FROM brand",
        "DELETE FROM users",
    ],
    executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
)
class HandlePaymentCallbackUseCaseIntegrationTest {

    @Autowired
    private lateinit var handlePaymentCallbackUseCase: HandlePaymentCallbackUseCase

    @Autowired
    private lateinit var requestPaymentUseCase: RequestPaymentUseCase

    @Autowired
    private lateinit var registerUserUseCase: RegisterUserUseCase

    @Autowired
    private lateinit var registerBrandUseCase: RegisterBrandUseCase

    @Autowired
    private lateinit var registerProductUseCase: RegisterProductUseCase

    @Autowired
    private lateinit var createOrderUseCase: CreateOrderUseCase

    @Autowired
    private lateinit var paymentRepository: PaymentRepository

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @MockitoBean
    private lateinit var paymentGatewayPort: PaymentGatewayPort

    @MockitoBean
    private lateinit var callbackUrlProvider: CallbackUrlProvider

    private var userId: Long = 0
    private var orderId: Long = 0
    private var paymentId: Long = 0

    @BeforeEach
    fun setUp() {
        userId = registerUserUseCase.register(createUserCommand())
        val brandId = registerBrandUseCase.register(
            RegisterBrandCommand(name = "테스트브랜드", description = null, logoUrl = null),
        )
        val productId = registerProductUseCase.register(createProductCommand(brandId))
        orderId = createOrderUseCase.create(
            userId,
            CreateOrderCommand(items = listOf(OrderItemCommand(productId = productId, quantity = 2))),
        )

        org.mockito.Mockito.`when`(callbackUrlProvider.getCallbackUrl())
            .thenReturn("http://localhost:8080/api/v1/payments/callback")
        org.mockito.Mockito.`when`(paymentGatewayPort.requestPayment(org.mockito.kotlin.any()))
            .thenReturn(
                PgPaymentResponse(
                    success = true,
                    transactionKey = TRANSACTION_KEY,
                    status = "REQUESTED",
                    reason = null,
                ),
            )

        paymentId = requestPaymentUseCase.request(userId, createPaymentCommand())
    }

    @Test
    fun `SUCCESS 콜백 수신 시 결제가 승인되어야 한다`() {
        val callbackInfo = PaymentCallbackInfo(
            transactionKey = TRANSACTION_KEY,
            orderId = orderId.toString(),
            status = "SUCCESS",
            reason = null,
        )

        handlePaymentCallbackUseCase.handle(callbackInfo)

        val payment = paymentRepository.findById(paymentId)
        assertThat(payment!!.status).isEqualTo(PaymentStatus.SUCCESS)
    }

    @Test
    fun `SUCCESS 콜백 수신 시 주문이 COMPLETED 상태가 되어야 한다`() {
        val callbackInfo = PaymentCallbackInfo(
            transactionKey = TRANSACTION_KEY,
            orderId = orderId.toString(),
            status = "SUCCESS",
            reason = null,
        )

        handlePaymentCallbackUseCase.handle(callbackInfo)

        val order = orderRepository.findById(orderId)
        assertThat(order!!.status).isEqualTo(OrderStatus.COMPLETED)
    }

    @Test
    fun `FAILED 콜백 수신 시 결제가 실패 처리되어야 한다`() {
        val callbackInfo = PaymentCallbackInfo(
            transactionKey = TRANSACTION_KEY,
            orderId = orderId.toString(),
            status = "FAILED",
            reason = "잔액 부족",
        )

        handlePaymentCallbackUseCase.handle(callbackInfo)

        val payment = paymentRepository.findById(paymentId)
        assertThat(payment!!.status).isEqualTo(PaymentStatus.FAILED)
        assertThat(payment.failureReason).isEqualTo("잔액 부족")
    }

    @Test
    fun `존재하지 않는 transactionKey의 콜백은 무시해야 한다`() {
        val callbackInfo = PaymentCallbackInfo(
            transactionKey = "non-existent-key",
            orderId = orderId.toString(),
            status = "SUCCESS",
            reason = null,
        )

        handlePaymentCallbackUseCase.handle(callbackInfo)

        val payment = paymentRepository.findById(paymentId)
        assertThat(payment!!.status).isEqualTo(PaymentStatus.REQUESTED)
    }

    @Test
    fun `이미 최종 상태인 결제에 대한 콜백은 무시해야 한다`() {
        val successCallback = PaymentCallbackInfo(
            transactionKey = TRANSACTION_KEY,
            orderId = orderId.toString(),
            status = "SUCCESS",
            reason = null,
        )
        handlePaymentCallbackUseCase.handle(successCallback)

        val duplicateCallback = PaymentCallbackInfo(
            transactionKey = TRANSACTION_KEY,
            orderId = orderId.toString(),
            status = "FAILED",
            reason = "중복 처리 시도",
        )
        handlePaymentCallbackUseCase.handle(duplicateCallback)

        val payment = paymentRepository.findById(paymentId)
        assertThat(payment!!.status).isEqualTo(PaymentStatus.SUCCESS)
    }

    private fun createPaymentCommand() = RequestPaymentCommand(
        orderId = orderId,
        cardType = "SAMSUNG",
        cardNo = "1234-5678-9012-3456",
    )

    private fun createUserCommand() = RegisterUserCommand(
        loginId = "testuser",
        password = "Password1!",
        name = "테스트",
        birthDate = "1993-04-01",
        email = "test@example.com",
        gender = "MALE",
    )

    private fun createProductCommand(brandId: Long) = RegisterProductCommand(
        brandId = brandId,
        name = "테스트상품",
        description = "설명",
        price = 10000L,
        stock = 100,
        thumbnailUrl = null,
        images = emptyList(),
    )

    companion object {
        private const val TRANSACTION_KEY = "tx-test-123"
    }
}

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
import com.loopers.domain.payment.PaymentRepository
import com.loopers.domain.payment.PaymentStatus
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.testcontainers.MySqlTestContainersConfig
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
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
class RequestPaymentUseCaseIntegrationTest {

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

    @MockitoBean
    private lateinit var paymentGatewayPort: PaymentGatewayPort

    @MockitoBean
    private lateinit var callbackUrlProvider: CallbackUrlProvider

    private var userId: Long = 0
    private var orderId: Long = 0

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
    }

    @Test
    fun `정상적인 경우 결제가 생성되고 ID를 반환해야 한다`() {
        org.mockito.Mockito.`when`(paymentGatewayPort.requestPayment(org.mockito.kotlin.any()))
            .thenReturn(
                PgPaymentResponse(
                    success = true,
                    transactionKey = "tx-123",
                    status = "REQUESTED",
                    reason = null,
                ),
            )

        val paymentId = requestPaymentUseCase.request(userId, createPaymentCommand())

        assertThat(paymentId).isPositive()
        val payment = paymentRepository.findById(paymentId)
        assertThat(payment).isNotNull
        assertThat(payment!!.status).isEqualTo(PaymentStatus.REQUESTED)
        assertThat(payment.transactionKey).isEqualTo("tx-123")
    }

    @Test
    fun `PG 요청 실패 시 결제 상태가 PENDING으로 유지되어야 한다`() {
        org.mockito.Mockito.`when`(paymentGatewayPort.requestPayment(org.mockito.kotlin.any()))
            .thenReturn(
                PgPaymentResponse(
                    success = false,
                    transactionKey = null,
                    status = "FAILED",
                    reason = "잔액 부족",
                ),
            )

        val paymentId = requestPaymentUseCase.request(userId, createPaymentCommand())

        val payment = paymentRepository.findById(paymentId)
        assertThat(payment).isNotNull
        assertThat(payment!!.status).isEqualTo(PaymentStatus.PENDING)
    }

    @Test
    fun `존재하지 않는 주문에 대해 NOT_FOUND 예외가 발생한다`() {
        val command = RequestPaymentCommand(orderId = 9999L, cardType = "SAMSUNG", cardNo = "1234-5678-9012-3456")

        assertThatThrownBy { requestPaymentUseCase.request(userId, command) }
            .isInstanceOfSatisfying(CoreException::class.java) { ex ->
                assertThat(ex.errorType).isEqualTo(ErrorType.NOT_FOUND)
            }
    }

    @Test
    fun `타인의 주문에 대해 예외가 발생한다`() {
        val otherUserId = registerUserUseCase.register(
            RegisterUserCommand(
                loginId = "other1234",
                password = "Password1!",
                name = "타인",
                birthDate = "1990-01-01",
                email = "other@example.com",
                gender = "MALE",
            ),
        )

        assertThatThrownBy { requestPaymentUseCase.request(otherUserId, createPaymentCommand()) }
            .isInstanceOf(Exception::class.java)
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
}

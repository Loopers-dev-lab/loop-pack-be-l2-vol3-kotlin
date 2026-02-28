package com.loopers.application.order

import com.loopers.application.brand.RegisterBrandCommand
import com.loopers.application.brand.RegisterBrandUseCase
import com.loopers.application.product.RegisterProductCommand
import com.loopers.application.product.RegisterProductUseCase
import com.loopers.application.user.RegisterUserCommand
import com.loopers.application.user.RegisterUserUseCase
import com.loopers.domain.order.OrderException
import com.loopers.domain.product.ProductRepository
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
import org.springframework.test.context.jdbc.Sql

@SpringBootTest
@Import(MySqlTestContainersConfig::class)
@Sql(
    statements = [
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
class CancelOrderUseCaseIntegrationTest {

    @Autowired
    private lateinit var cancelOrderUseCase: CancelOrderUseCase

    @Autowired
    private lateinit var createOrderUseCase: CreateOrderUseCase

    @Autowired
    private lateinit var registerUserUseCase: RegisterUserUseCase

    @Autowired
    private lateinit var registerBrandUseCase: RegisterBrandUseCase

    @Autowired
    private lateinit var registerProductUseCase: RegisterProductUseCase

    private var userId: Long = 0
    private var otherUserId: Long = 0
    private var orderId: Long = 0

    @BeforeEach
    fun setUp() {
        userId = registerUserUseCase.register(createUserCommand("testuser"))
        otherUserId = registerUserUseCase.register(createUserCommand("otheruser"))
        val brandId = registerBrandUseCase.register(
            RegisterBrandCommand(name = "테스트브랜드", description = null, logoUrl = null),
        )
        val productId = registerProductUseCase.register(createProductCommand(brandId))
        orderId = createOrderUseCase.create(
            userId,
            CreateOrderCommand(items = listOf(OrderItemCommand(productId = productId, quantity = 1))),
        )
    }

    @Autowired
    private lateinit var getOrderUseCase: GetOrderUseCase

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Test
    fun `정상적인 경우 주문이 취소되고 재고가 복구된다`() {
        cancelOrderUseCase.cancel(userId, orderId)

        val order = getOrderUseCase.getById(userId, orderId)
        assertThat(order.status).isEqualTo("CANCELLED")
    }

    @Test
    fun `취소된 주문을 다시 취소하면 OrderException이 발생한다`() {
        cancelOrderUseCase.cancel(userId, orderId)

        assertThatThrownBy { cancelOrderUseCase.cancel(userId, orderId) }
            .isInstanceOf(OrderException::class.java)
    }

    @Test
    fun `타인의 주문을 취소하면 OrderException이 발생한다`() {
        assertThatThrownBy { cancelOrderUseCase.cancel(otherUserId, orderId) }
            .isInstanceOf(OrderException::class.java)
    }

    @Test
    fun `존재하지 않는 주문을 취소하면 NOT_FOUND 예외가 발생한다`() {
        assertThatThrownBy { cancelOrderUseCase.cancel(userId, 9999L) }
            .isInstanceOfSatisfying(CoreException::class.java) { ex ->
                assertThat(ex.errorType).isEqualTo(ErrorType.NOT_FOUND)
            }
    }

    private fun createUserCommand(loginId: String) = RegisterUserCommand(
        loginId = loginId,
        password = "Password1!",
        name = "테스트",
        birthDate = "1993-04-01",
        email = "$loginId@example.com",
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

package com.loopers.application.order

import com.loopers.application.brand.RegisterBrandCommand
import com.loopers.application.brand.RegisterBrandUseCase
import com.loopers.application.product.RegisterProductCommand
import com.loopers.application.product.RegisterProductUseCase
import com.loopers.application.user.RegisterUserCommand
import com.loopers.application.user.RegisterUserUseCase
import com.loopers.domain.order.OrderException
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
class GetOrderUseCaseIntegrationTest {

    @Autowired
    private lateinit var getOrderUseCase: GetOrderUseCase

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
            CreateOrderCommand(items = listOf(OrderItemCommand(productId = productId, quantity = 2))),
        )
    }

    @Test
    fun `본인 주문을 조회할 수 있다`() {
        val result = getOrderUseCase.getById(userId, orderId)

        assertThat(result.id).isEqualTo(orderId)
        assertThat(result.userId).isEqualTo(userId)
    }

    @Test
    fun `타인의 주문을 조회하면 OrderException이 발생한다`() {
        assertThatThrownBy { getOrderUseCase.getById(otherUserId, orderId) }
            .isInstanceOf(OrderException::class.java)
    }

    @Test
    fun `존재하지 않는 주문을 조회하면 NOT_FOUND 예외가 발생한다`() {
        assertThatThrownBy { getOrderUseCase.getById(userId, 9999L) }
            .isInstanceOfSatisfying(CoreException::class.java) { ex ->
                assertThat(ex.errorType).isEqualTo(ErrorType.NOT_FOUND)
            }
    }

    @Test
    fun `getAllByUserId는 본인 주문만 반환한다`() {
        val result = getOrderUseCase.getAllByUserId(userId)

        assertThat(result).hasSize(1)
        assertThat(result[0].userId).isEqualTo(userId)
    }

    @Test
    fun `getAll은 전체 주문을 반환한다`() {
        val brandId = registerBrandUseCase.register(
            RegisterBrandCommand(name = "브랜드B", description = null, logoUrl = null),
        )
        val productId2 = registerProductUseCase.register(createProductCommand(brandId))
        createOrderUseCase.create(
            otherUserId,
            CreateOrderCommand(items = listOf(OrderItemCommand(productId = productId2, quantity = 1))),
        )

        val result = getOrderUseCase.getAll()

        assertThat(result).hasSize(2)
    }

    @Test
    fun `getByIdForAdmin은 타인의 주문도 조회할 수 있다`() {
        val result = getOrderUseCase.getByIdForAdmin(orderId)

        assertThat(result.id).isEqualTo(orderId)
        assertThat(result.userId).isEqualTo(userId)
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

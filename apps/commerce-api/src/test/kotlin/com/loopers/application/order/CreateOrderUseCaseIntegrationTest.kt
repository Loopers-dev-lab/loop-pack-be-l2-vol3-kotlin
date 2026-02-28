package com.loopers.application.order

import com.loopers.application.brand.RegisterBrandCommand
import com.loopers.application.brand.RegisterBrandUseCase
import com.loopers.application.product.DeleteProductUseCase
import com.loopers.application.product.RegisterProductCommand
import com.loopers.application.product.RegisterProductUseCase
import com.loopers.application.user.RegisterUserCommand
import com.loopers.application.user.RegisterUserUseCase
import com.loopers.domain.product.ProductException
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
class CreateOrderUseCaseIntegrationTest {

    @Autowired
    private lateinit var createOrderUseCase: CreateOrderUseCase

    @Autowired
    private lateinit var registerUserUseCase: RegisterUserUseCase

    @Autowired
    private lateinit var registerBrandUseCase: RegisterBrandUseCase

    @Autowired
    private lateinit var registerProductUseCase: RegisterProductUseCase

    @Autowired
    private lateinit var deleteProductUseCase: DeleteProductUseCase

    @Autowired
    private lateinit var productRepository: ProductRepository

    private var userId: Long = 0
    private var brandId: Long = 0
    private var productId: Long = 0

    @BeforeEach
    fun setUp() {
        userId = registerUserUseCase.register(createUserCommand())
        brandId = registerBrandUseCase.register(
            RegisterBrandCommand(name = "테스트브랜드", description = null, logoUrl = null),
        )
        productId = registerProductUseCase.register(createProductCommand(brandId, 100))
    }

    @Test
    fun `정상적인 경우 주문이 생성되고 ID를 반환해야 한다`() {
        val command = CreateOrderCommand(
            items = listOf(OrderItemCommand(productId = productId, quantity = 2)),
        )

        val result = createOrderUseCase.create(userId, command)

        assertThat(result).isPositive()
    }

    @Test
    fun `주문 시 재고가 차감되어야 한다`() {
        val command = CreateOrderCommand(
            items = listOf(OrderItemCommand(productId = productId, quantity = 3)),
        )

        createOrderUseCase.create(userId, command)

        val product = productRepository.findById(productId)
        assertThat(product!!.stock.quantity).isEqualTo(97)
    }

    @Test
    fun `재고가 부족하면 ProductException이 발생한다`() {
        val command = CreateOrderCommand(
            items = listOf(OrderItemCommand(productId = productId, quantity = 101)),
        )

        assertThatThrownBy { createOrderUseCase.create(userId, command) }
            .isInstanceOf(ProductException::class.java)
    }

    @Test
    fun `삭제된 상품을 주문하면 ProductException이 발생한다`() {
        deleteProductUseCase.delete(productId)
        val command = CreateOrderCommand(
            items = listOf(OrderItemCommand(productId = productId, quantity = 1)),
        )

        assertThatThrownBy { createOrderUseCase.create(userId, command) }
            .isInstanceOf(ProductException::class.java)
    }

    @Test
    fun `존재하지 않는 상품을 주문하면 NOT_FOUND 예외가 발생한다`() {
        val command = CreateOrderCommand(
            items = listOf(OrderItemCommand(productId = 9999L, quantity = 1)),
        )

        assertThatThrownBy { createOrderUseCase.create(userId, command) }
            .isInstanceOfSatisfying(CoreException::class.java) { ex ->
                assertThat(ex.errorType).isEqualTo(ErrorType.NOT_FOUND)
            }
    }

    private fun createUserCommand() = RegisterUserCommand(
        loginId = "testuser",
        password = "Password1!",
        name = "테스트",
        birthDate = "1993-04-01",
        email = "test@example.com",
        gender = "MALE",
    )

    private fun createProductCommand(brandId: Long, stock: Int) = RegisterProductCommand(
        brandId = brandId,
        name = "테스트상품",
        description = "설명",
        price = 10000L,
        stock = stock,
        thumbnailUrl = null,
        images = emptyList(),
    )
}

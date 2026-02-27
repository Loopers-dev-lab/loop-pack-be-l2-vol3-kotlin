package com.loopers.domain.order

import com.loopers.domain.BaseEntity
import com.loopers.domain.Money
import com.loopers.domain.brand.Brand
import com.loopers.domain.product.Product
import com.loopers.domain.user.User
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class OrderServiceTest {

    private lateinit var orderService: OrderService
    private lateinit var fakeOrderRepository: FakeOrderRepository
    private lateinit var orderDomainService: OrderDomainService

    @BeforeEach
    fun setUp() {
        fakeOrderRepository = FakeOrderRepository()
        orderDomainService = OrderDomainService()
        orderService = OrderService(fakeOrderRepository, orderDomainService)
    }

    private fun createUser(id: Long = 1L): User {
        val user = User(
            loginId = "testuser",
            password = "password123",
            name = "홍길동",
            birthday = LocalDate.of(1990, 1, 1),
            email = "test@example.com",
        )
        setEntityId(user, id)
        return user
    }

    private fun createBrand(id: Long = 1L, name: String = "나이키"): Brand {
        val brand = Brand(name = name)
        setEntityId(brand, id)
        return brand
    }

    private fun createProduct(
        id: Long = 1L,
        brandId: Long = 1L,
        name: String = "에어맥스 90",
        price: Money = Money(139000),
        stockQuantity: Int = 100,
    ): Product {
        val product = Product(
            brandId = brandId,
            name = name,
            price = price,
            stockQuantity = stockQuantity,
        )
        setEntityId(product, id)
        return product
    }

    private fun createCommand(
        user: User = createUser(),
        products: List<Product> = listOf(createProduct()),
        quantities: Map<Long, Int> = mapOf(1L to 1),
        brands: Map<Long, Brand> = mapOf(1L to createBrand()),
    ): CreateOrderCommand {
        return CreateOrderCommand(
            user = user,
            products = products,
            quantities = quantities,
            brands = brands,
        )
    }

    @Nested
    inner class CreateOrder {

        @Test
        @DisplayName("주문이 저장되고 주문번호가 생성된다")
        fun savedWithOrderNumber() {
            // arrange
            val command = createCommand(quantities = mapOf(1L to 2))

            // act
            val order = orderService.createOrder(command)

            // assert
            assertThat(order.id).isGreaterThan(0L)
            assertThat(order.orderNumber).isNotBlank()
            assertThat(order.orderNumber).hasSize(14) // yyMMdd(6) + id(8)
        }

        @Test
        @DisplayName("저장된 주문을 조회할 수 있다")
        fun canBeFoundAfterSave() {
            // arrange
            val command = createCommand()

            // act
            val savedOrder = orderService.createOrder(command)
            val foundOrder = orderService.findById(savedOrder.id)

            // assert
            assertThat(foundOrder.id).isEqualTo(savedOrder.id)
            assertThat(foundOrder.userId).isEqualTo(1L)
        }
    }

    @Nested
    inner class FindById {

        @Test
        @DisplayName("존재하지 않는 주문을 조회하면 NOT_FOUND 예외가 발생한다")
        fun notFoundThrowsException() {
            // act
            val result = assertThrows<CoreException> {
                orderService.findById(999L)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    private fun setEntityId(entity: BaseEntity, id: Long) {
        val idField = BaseEntity::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(entity, id)
    }
}

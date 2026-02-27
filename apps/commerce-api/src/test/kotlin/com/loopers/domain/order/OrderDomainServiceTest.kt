package com.loopers.domain.order

import com.loopers.domain.BaseEntity
import com.loopers.domain.Money
import com.loopers.domain.brand.Brand
import com.loopers.domain.product.Product
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OrderDomainServiceTest {

    private lateinit var orderDomainService: OrderDomainService

    @BeforeEach
    fun setUp() {
        orderDomainService = OrderDomainService()
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

    private fun createOrderCommand(
        userId: Long = 1L,
        products: List<Product> = listOf(createProduct()),
        quantities: Map<Long, Int> = mapOf(1L to 1),
        brands: Map<Long, Brand> = mapOf(1L to createBrand()),
    ): CreateOrderCommand {
        return CreateOrderCommand(
            userId = userId,
            products = products,
            quantities = quantities,
            brands = brands,
        )
    }

    @Nested
    inner class PlaceOrder {

        @Test
        @DisplayName("정상적인 주문이 생성된다")
        fun success() {
            // arrange
            val command = createOrderCommand(quantities = mapOf(1L to 2))

            // act
            val order = orderDomainService.placeOrder(command)

            // assert
            assertThat(order.userId).isEqualTo(1L)
            assertThat(order.orderStatus).isEqualTo(OrderStatus.ORDERED)
            assertThat(order.orderItems).hasSize(1)
        }

        @Test
        @DisplayName("주문 총액이 올바르게 계산된다")
        fun totalAmountCalculated() {
            // arrange
            val brand = createBrand()
            val productA = createProduct(id = 1L, price = Money(10000), stockQuantity = 50)
            val productB = createProduct(id = 2L, name = "에어포스 1", price = Money(15000), stockQuantity = 50)
            val command = createOrderCommand(
                products = listOf(productA, productB),
                quantities = mapOf(1L to 2, 2L to 3), // 20000 + 45000
                brands = mapOf(1L to brand),
            )

            // act
            val order = orderDomainService.placeOrder(command)

            // assert
            assertThat(order.totalAmount).isEqualTo(Money(65000))
        }

        @Test
        @DisplayName("스냅샷에 주문 당시 상품/브랜드 정보가 저장된다")
        fun snapshotSaved() {
            // arrange
            val brand = createBrand(name = "나이키")
            val product = createProduct(name = "에어맥스 90", price = Money(139000))
            val command = createOrderCommand(
                products = listOf(product),
                quantities = mapOf(1L to 1),
                brands = mapOf(1L to brand),
            )

            // act
            val order = orderDomainService.placeOrder(command)

            // assert
            val item = order.orderItems[0]
            assertThat(item.productSnapshot.productName).isEqualTo("에어맥스 90")
            assertThat(item.productSnapshot.brandName).isEqualTo("나이키")
            assertThat(item.priceSnapshot.originalPrice).isEqualTo(Money(139000))
            assertThat(item.priceSnapshot.finalPrice).isEqualTo(Money(139000))
            assertThat(item.priceSnapshot.discountAmount).isEqualTo(Money.ZERO)
        }

        @Test
        @DisplayName("수량 정보가 없으면 BAD_REQUEST 예외가 발생한다")
        fun missingQuantityThrowsBadRequest() {
            // arrange
            val command = createOrderCommand(
                products = listOf(createProduct()),
                quantities = emptyMap(),
            )

            // act
            val result = assertThrows<CoreException> {
                orderDomainService.placeOrder(command)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("존재하지 않는 브랜드가 있으면 BAD_REQUEST 예외가 발생한다")
        fun missingBrandThrowsBadRequest() {
            // arrange
            val product = createProduct(brandId = 999L)
            val command = createOrderCommand(
                products = listOf(product),
                brands = emptyMap(), // 브랜드 정보 없음
            )

            // act
            val result = assertThrows<CoreException> {
                orderDomainService.placeOrder(command)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    private fun setEntityId(entity: BaseEntity, id: Long) {
        val idField = BaseEntity::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(entity, id)
    }
}

package com.loopers.application.order

import com.loopers.domain.catalog.CatalogCommand
import com.loopers.domain.catalog.CatalogService
import com.loopers.domain.catalog.brand.FakeBrandRepository
import com.loopers.domain.catalog.product.FakeProductRepository
import com.loopers.domain.order.FakeOrderRepository
import com.loopers.domain.order.OrderCommand
import com.loopers.domain.order.OrderService
import com.loopers.domain.order.entity.Order
import com.loopers.domain.point.FakePointHistoryRepository
import com.loopers.domain.point.FakeUserPointRepository
import com.loopers.domain.point.PointChargingService
import com.loopers.domain.point.UserPointService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class OrderFacadeTest {

    private lateinit var brandRepository: FakeBrandRepository
    private lateinit var productRepository: FakeProductRepository
    private lateinit var orderRepository: FakeOrderRepository
    private lateinit var userPointRepository: FakeUserPointRepository
    private lateinit var pointHistoryRepository: FakePointHistoryRepository
    private lateinit var catalogService: CatalogService
    private lateinit var orderService: OrderService
    private lateinit var userPointService: UserPointService
    private lateinit var pointChargingService: PointChargingService
    private lateinit var orderFacade: OrderFacade

    @BeforeEach
    fun setUp() {
        brandRepository = FakeBrandRepository()
        productRepository = FakeProductRepository()
        orderRepository = FakeOrderRepository()
        userPointRepository = FakeUserPointRepository()
        pointHistoryRepository = FakePointHistoryRepository()
        catalogService = CatalogService(brandRepository, productRepository)
        orderService = OrderService(orderRepository)
        userPointService = UserPointService(userPointRepository, pointHistoryRepository)
        pointChargingService = PointChargingService(userPointRepository, pointHistoryRepository)
        orderFacade = OrderFacade(orderService, catalogService, userPointService)
    }

    private fun setupBrandAndProduct(
        price: BigDecimal = BigDecimal("10000"),
        stock: Int = 100,
    ): Long {
        val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = "나이키"))
        val product = catalogService.createProduct(
            CatalogCommand.CreateProduct(
                brandId = brand.id,
                name = "에어맥스 90",
                price = price,
                stock = stock,
            ),
        )
        return product.id
    }

    private fun setupUserPoint(userId: Long, balance: Long) {
        userPointService.createUserPoint(userId)
        if (balance > 0) {
            pointChargingService.charge(userId, balance)
        }
    }

    @Nested
    @DisplayName("createOrder 시")
    inner class CreateOrder {

        @Test
        @DisplayName("정상 주문이 생성되고 재고 차감, 포인트 차감이 수행된다")
        fun createOrder_success() {
            // arrange
            val productId = setupBrandAndProduct(price = BigDecimal("10000"), stock = 100)
            setupUserPoint(1L, 50000)
            val command = OrderCommand.CreateOrder(
                items = listOf(OrderCommand.CreateOrderItem(productId = productId, quantity = 2)),
            )

            // act
            val order = orderFacade.createOrder(1L, command)

            // assert
            assertThat(order.id).isNotEqualTo(0L)
            assertThat(order.status).isEqualTo(Order.OrderStatus.CREATED)
            assertThat(order.totalPrice).isEqualByComparingTo(BigDecimal("20000"))

            // 재고 차감 확인
            val product = catalogService.getProduct(productId)
            assertThat(product.stock).isEqualTo(98)

            // 포인트 차감 확인
            val userPoint = userPointService.getBalance(1L)
            assertThat(userPoint.balance).isEqualTo(30000)
        }

        @Test
        @DisplayName("재고가 부족하면 CoreException이 발생한다")
        fun createOrder_insufficientStock_throwsException() {
            // arrange
            val productId = setupBrandAndProduct(price = BigDecimal("10000"), stock = 2)
            setupUserPoint(1L, 50000)
            val command = OrderCommand.CreateOrder(
                items = listOf(OrderCommand.CreateOrderItem(productId = productId, quantity = 5)),
            )

            // act
            val exception = assertThrows<CoreException> {
                orderFacade.createOrder(1L, command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("포인트가 부족하면 CoreException이 발생한다")
        fun createOrder_insufficientPoints_throwsException() {
            // arrange
            val productId = setupBrandAndProduct(price = BigDecimal("10000"), stock = 100)
            setupUserPoint(1L, 5000)
            val command = OrderCommand.CreateOrder(
                items = listOf(OrderCommand.CreateOrderItem(productId = productId, quantity = 2)),
            )

            // act
            val exception = assertThrows<CoreException> {
                orderFacade.createOrder(1L, command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("삭제된 상품으로 주문하면 BAD_REQUEST 예외가 발생한다")
        fun createOrder_deletedProduct_throwsException() {
            // arrange
            val productId = setupBrandAndProduct()
            setupUserPoint(1L, 50000)
            catalogService.deleteProduct(productId)
            val command = OrderCommand.CreateOrder(
                items = listOf(OrderCommand.CreateOrderItem(productId = productId, quantity = 1)),
            )

            // act
            val exception = assertThrows<CoreException> {
                orderFacade.createOrder(1L, command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("여러 상품을 포함한 주문이 정상 생성된다")
        fun createOrder_multipleItems_success() {
            // arrange
            val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = "나이키"))
            val product1 = catalogService.createProduct(
                CatalogCommand.CreateProduct(
                    brandId = brand.id,
                    name = "상품1",
                    price = BigDecimal("10000"),
                    stock = 50,
                ),
            )
            val product2 = catalogService.createProduct(
                CatalogCommand.CreateProduct(
                    brandId = brand.id,
                    name = "상품2",
                    price = BigDecimal("20000"),
                    stock = 50,
                ),
            )
            setupUserPoint(1L, 100000)
            val command = OrderCommand.CreateOrder(
                items = listOf(
                    OrderCommand.CreateOrderItem(productId = product1.id, quantity = 2),
                    OrderCommand.CreateOrderItem(productId = product2.id, quantity = 3),
                ),
            )

            // act
            val order = orderFacade.createOrder(1L, command)

            // assert
            assertThat(order.items).hasSize(2)
            assertThat(order.totalPrice).isEqualByComparingTo(BigDecimal("80000"))

            val p1 = catalogService.getProduct(product1.id)
            val p2 = catalogService.getProduct(product2.id)
            assertThat(p1.stock).isEqualTo(48)
            assertThat(p2.stock).isEqualTo(47)

            val userPoint = userPointService.getBalance(1L)
            assertThat(userPoint.balance).isEqualTo(20000)
        }
    }
}

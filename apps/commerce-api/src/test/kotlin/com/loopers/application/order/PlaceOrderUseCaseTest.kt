package com.loopers.application.order

import com.loopers.domain.catalog.CatalogCommand
import com.loopers.domain.catalog.CatalogService
import com.loopers.domain.catalog.brand.FakeBrandRepository
import com.loopers.domain.catalog.product.FakeProductRepository
import com.loopers.domain.order.FakeOrderItemRepository
import com.loopers.domain.order.FakeOrderRepository
import com.loopers.domain.order.OrderService
import com.loopers.domain.point.FakePointHistoryRepository
import com.loopers.domain.point.FakeUserPointRepository
import com.loopers.domain.point.PointChargingService
import com.loopers.domain.point.UserPointService
import com.loopers.domain.catalog.brand.vo.BrandName
import com.loopers.domain.common.Money
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class PlaceOrderUseCaseTest {

    private lateinit var brandRepository: FakeBrandRepository
    private lateinit var productRepository: FakeProductRepository
    private lateinit var orderRepository: FakeOrderRepository
    private lateinit var orderItemRepository: FakeOrderItemRepository
    private lateinit var userPointRepository: FakeUserPointRepository
    private lateinit var pointHistoryRepository: FakePointHistoryRepository
    private lateinit var catalogService: CatalogService
    private lateinit var orderService: OrderService
    private lateinit var userPointService: UserPointService
    private lateinit var pointChargingService: PointChargingService
    private lateinit var placeOrderUseCase: PlaceOrderUseCase

    @BeforeEach
    fun setUp() {
        brandRepository = FakeBrandRepository()
        productRepository = FakeProductRepository()
        orderRepository = FakeOrderRepository()
        orderItemRepository = FakeOrderItemRepository()
        userPointRepository = FakeUserPointRepository()
        pointHistoryRepository = FakePointHistoryRepository()
        catalogService = CatalogService(brandRepository, productRepository)
        orderService = OrderService(orderRepository, orderItemRepository)
        userPointService = UserPointService(userPointRepository, pointHistoryRepository)
        pointChargingService = PointChargingService(userPointRepository, pointHistoryRepository)
        placeOrderUseCase = PlaceOrderUseCase(orderService, catalogService, userPointService)
    }

    private fun setupBrandAndProduct(
        price: BigDecimal = BigDecimal("10000"),
        stock: Int = 100,
    ): Long {
        val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))
        val product = catalogService.createProduct(
            CatalogCommand.CreateProduct(
                brandId = brand.id,
                name = "에어맥스 90",
                price = Money(price),
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
    @DisplayName("execute 시")
    inner class Execute {

        @Test
        @DisplayName("정상 주문이 생성되고 재고 차감, 포인트 차감이 수행된다")
        fun execute_success() {
            // arrange
            val productId = setupBrandAndProduct(price = BigDecimal("10000"), stock = 100)
            setupUserPoint(1L, 50000)
            val command = PlaceOrderCommand(
                items = listOf(PlaceOrderCommand.PlaceOrderItemCommand(productId = productId, quantity = 2)),
            )

            // act
            val orderInfo = placeOrderUseCase.execute(1L, command)

            // assert
            assertThat(orderInfo.id).isNotEqualTo(0L)
            assertThat(orderInfo.status).isEqualTo("CREATED")
            assertThat(orderInfo.totalPrice).isEqualByComparingTo(BigDecimal("20000"))
            assertThat(orderInfo.items).hasSize(1)

            // 재고 차감 확인
            val product = catalogService.getProduct(productId)
            assertThat(product.stock).isEqualTo(98)

            // 포인트 차감 확인
            val userPoint = userPointService.getBalance(1L)
            assertThat(userPoint.balance).isEqualTo(30000)
        }

        @Test
        @DisplayName("재고가 부족하면 CoreException이 발생한다")
        fun execute_insufficientStock_throwsException() {
            // arrange
            val productId = setupBrandAndProduct(price = BigDecimal("10000"), stock = 2)
            setupUserPoint(1L, 50000)
            val command = PlaceOrderCommand(
                items = listOf(PlaceOrderCommand.PlaceOrderItemCommand(productId = productId, quantity = 5)),
            )

            // act
            val exception = assertThrows<CoreException> {
                placeOrderUseCase.execute(1L, command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("포인트가 부족하면 CoreException이 발생한다")
        fun execute_insufficientPoints_throwsException() {
            // arrange
            val productId = setupBrandAndProduct(price = BigDecimal("10000"), stock = 100)
            setupUserPoint(1L, 5000)
            val command = PlaceOrderCommand(
                items = listOf(PlaceOrderCommand.PlaceOrderItemCommand(productId = productId, quantity = 2)),
            )

            // act
            val exception = assertThrows<CoreException> {
                placeOrderUseCase.execute(1L, command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("삭제된 상품으로 주문하면 BAD_REQUEST 예외가 발생한다")
        fun execute_deletedProduct_throwsException() {
            // arrange
            val productId = setupBrandAndProduct()
            setupUserPoint(1L, 50000)
            catalogService.deleteProduct(productId)
            val command = PlaceOrderCommand(
                items = listOf(PlaceOrderCommand.PlaceOrderItemCommand(productId = productId, quantity = 1)),
            )

            // act
            val exception = assertThrows<CoreException> {
                placeOrderUseCase.execute(1L, command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("주문 항목이 비어있으면 BAD_REQUEST 예외가 발생한다")
        fun execute_emptyItems_throwsBadRequest() {
            // arrange
            setupUserPoint(1L, 50000)
            val command = PlaceOrderCommand(items = emptyList())

            // act
            val exception = assertThrows<CoreException> {
                placeOrderUseCase.execute(1L, command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("주문 수량이 0 이하이면 BAD_REQUEST 예외가 발생한다")
        fun execute_zeroQuantity_throwsBadRequest() {
            // arrange
            val productId = setupBrandAndProduct(price = BigDecimal("10000"), stock = 100)
            setupUserPoint(1L, 50000)
            val command = PlaceOrderCommand(
                items = listOf(PlaceOrderCommand.PlaceOrderItemCommand(productId = productId, quantity = 0)),
            )

            // act
            val exception = assertThrows<CoreException> {
                placeOrderUseCase.execute(1L, command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("동일한 productId가 중복으로 포함되면 BAD_REQUEST 예외가 발생한다")
        fun execute_duplicateProductId_throwsException() {
            // arrange
            val productId = setupBrandAndProduct(price = BigDecimal("10000"), stock = 100)
            setupUserPoint(1L, 50000)
            val command = PlaceOrderCommand(
                items = listOf(
                    PlaceOrderCommand.PlaceOrderItemCommand(productId = productId, quantity = 1),
                    PlaceOrderCommand.PlaceOrderItemCommand(productId = productId, quantity = 2),
                ),
            )

            // act
            val exception = assertThrows<CoreException> {
                placeOrderUseCase.execute(1L, command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("상품 가격이 소수점 0.5 이상일 때 포인트가 HALF_UP 반올림된 금액만큼 차감된다")
        fun execute_fractionalPrice_deductsRoundedPoints() {
            // arrange — 1000.50원 × 1개 = 1000.50원 → HALF_UP → 1001포인트 차감 (truncate면 1000)
            val productId = setupBrandAndProduct(price = BigDecimal("1000.50"), stock = 10)
            setupUserPoint(1L, 5000)
            val command = PlaceOrderCommand(
                items = listOf(PlaceOrderCommand.PlaceOrderItemCommand(productId = productId, quantity = 1)),
            )

            // act
            val orderInfo = placeOrderUseCase.execute(1L, command)

            // assert
            assertThat(orderInfo.totalPrice).isEqualByComparingTo(BigDecimal("1000.50"))
            val userPoint = userPointService.getBalance(1L)
            assertThat(userPoint.balance).isEqualTo(3999) // 5000 - 1001 (HALF_UP 반올림)
        }

        @Test
        @DisplayName("여러 상품을 포함한 주문이 정상 생성된다")
        fun execute_multipleItems_success() {
            // arrange
            val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))
            val product1 = catalogService.createProduct(
                CatalogCommand.CreateProduct(
                    brandId = brand.id,
                    name = "상품1",
                    price = Money(BigDecimal("10000")),
                    stock = 50,
                ),
            )
            val product2 = catalogService.createProduct(
                CatalogCommand.CreateProduct(
                    brandId = brand.id,
                    name = "상품2",
                    price = Money(BigDecimal("20000")),
                    stock = 50,
                ),
            )
            setupUserPoint(1L, 100000)
            val command = PlaceOrderCommand(
                items = listOf(
                    PlaceOrderCommand.PlaceOrderItemCommand(productId = product1.id, quantity = 2),
                    PlaceOrderCommand.PlaceOrderItemCommand(productId = product2.id, quantity = 3),
                ),
            )

            // act
            val orderInfo = placeOrderUseCase.execute(1L, command)

            // assert
            assertThat(orderInfo.items).hasSize(2)
            assertThat(orderInfo.totalPrice).isEqualByComparingTo(BigDecimal("80000"))

            val p1 = catalogService.getProduct(product1.id)
            val p2 = catalogService.getProduct(product2.id)
            assertThat(p1.stock).isEqualTo(48)
            assertThat(p2.stock).isEqualTo(47)

            val userPoint = userPointService.getBalance(1L)
            assertThat(userPoint.balance).isEqualTo(20000)
        }
    }
}

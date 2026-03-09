package com.loopers.domain.order

import com.loopers.application.catalog.AdminRegisterProductUseCase
import com.loopers.application.catalog.RegisterProductCriteria
import com.loopers.application.catalog.RegisterProductResult
import com.loopers.domain.catalog.BrandService
import com.loopers.domain.catalog.RegisterBrandCommand
import com.loopers.application.order.CreateOrderCriteria
import com.loopers.application.order.CreateOrderItemCriteria
import com.loopers.application.order.GetOrderCriteria
import com.loopers.application.order.GetOrdersCriteria
import com.loopers.application.order.UserCreateOrderUseCase
import com.loopers.application.order.UserGetOrderUseCase
import com.loopers.application.order.UserGetOrdersUseCase
import com.loopers.domain.user.RegisterCommand
import com.loopers.domain.user.UserService
import com.loopers.infrastructure.catalog.ProductJpaRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@SpringBootTest
class UserOrderUseCaseIntegrationTest @Autowired constructor(
    private val userCreateOrderUseCase: UserCreateOrderUseCase,
    private val userGetOrdersUseCase: UserGetOrdersUseCase,
    private val userGetOrderUseCase: UserGetOrderUseCase,
    private val brandService: BrandService,
    private val adminRegisterProductUseCase: AdminRegisterProductUseCase,
    private val userService: UserService,
    private val productJpaRepository: ProductJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val DEFAULT_USERNAME = "testuser"
        private const val DEFAULT_PASSWORD = "password1234!"
        private const val DEFAULT_NAME = "테스트유저"
        private const val DEFAULT_EMAIL = "test@loopers.com"
        private val DEFAULT_BIRTH_DATE = ZonedDateTime.of(1995, 5, 29, 0, 0, 0, 0, ZoneId.of("Asia/Seoul"))
        private const val DEFAULT_BRAND_NAME = "나이키"
        private const val DEFAULT_PRODUCT_NAME = "에어맥스 90"
        private const val DEFAULT_QUANTITY = 100
        private val DEFAULT_PRICE = BigDecimal("129000")
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun registerUser(username: String = DEFAULT_USERNAME) {
        userService.register(
            RegisterCommand(
                username = username,
                password = DEFAULT_PASSWORD,
                name = DEFAULT_NAME,
                email = DEFAULT_EMAIL,
                birthDate = DEFAULT_BIRTH_DATE,
            ),
        )
    }

    private fun registerBrand(name: String = DEFAULT_BRAND_NAME): Long {
        return brandService.register(RegisterBrandCommand(name = name)).id
    }

    private fun registerProduct(
        brandId: Long,
        name: String = DEFAULT_PRODUCT_NAME,
        quantity: Int = DEFAULT_QUANTITY,
        price: BigDecimal = DEFAULT_PRICE,
    ): RegisterProductResult {
        return adminRegisterProductUseCase.execute(
            RegisterProductCriteria(
                brandId = brandId,
                name = name,
                quantity = quantity,
                price = price,
            ),
        )
    }

    private fun createOrderCriteria(
        loginId: String = DEFAULT_USERNAME,
        items: List<CreateOrderItemCriteria>,
    ): CreateOrderCriteria {
        return CreateOrderCriteria(loginId = loginId, items = items)
    }

    @DisplayName("주문 생성")
    @Nested
    inner class CreateOrder {
        @DisplayName("유효한 정보가 주어지면, 주문이 생성되고 재고가 차감된다.")
        @Test
        fun createsOrderAndDecreasesStockWhenValidInfoIsProvided() {
            // arrange
            registerUser()
            val brandId = registerBrand()
            val product = registerProduct(brandId = brandId, quantity = 10)
            val orderQuantity = 3
            val criteria = createOrderCriteria(
                items = listOf(CreateOrderItemCriteria(productId = product.id, quantity = orderQuantity)),
            )

            // act
            val result = userCreateOrderUseCase.execute(criteria)

            // assert
            val updatedProduct = productJpaRepository.findById(product.id).get()
            assertAll(
                { assertThat(result.id).isNotNull() },
                { assertThat(updatedProduct.quantity).isEqualTo(10 - orderQuantity) },
            )
        }

        @DisplayName("다중 상품 주문이 정상적으로 생성된다.")
        @Test
        fun createsOrderWithMultipleItems() {
            // arrange
            registerUser()
            val brandId = registerBrand()
            val product1 = registerProduct(brandId = brandId, name = "상품1", quantity = 10, price = BigDecimal("10000"))
            val product2 = registerProduct(brandId = brandId, name = "상품2", quantity = 20, price = BigDecimal("20000"))
            val criteria = createOrderCriteria(
                items = listOf(
                    CreateOrderItemCriteria(productId = product1.id, quantity = 2),
                    CreateOrderItemCriteria(productId = product2.id, quantity = 3),
                ),
            )

            // act
            val result = userCreateOrderUseCase.execute(criteria)

            // assert
            assertThat(result.id).isNotNull()
        }

        @DisplayName("존재하지 않는 상품이면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFoundExceptionWhenProductDoesNotExist() {
            // arrange
            registerUser()
            val criteria = createOrderCriteria(
                items = listOf(CreateOrderItemCriteria(productId = 999L, quantity = 1)),
            )

            // act & assert
            val result = assertThrows<CoreException> {
                userCreateOrderUseCase.execute(criteria)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("재고가 부족하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestExceptionWhenStockIsInsufficient() {
            // arrange
            registerUser()
            val brandId = registerBrand()
            val product = registerProduct(brandId = brandId, quantity = 3)
            val criteria = createOrderCriteria(
                items = listOf(CreateOrderItemCriteria(productId = product.id, quantity = 5)),
            )

            // act & assert
            val result = assertThrows<CoreException> {
                userCreateOrderUseCase.execute(criteria)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("존재하지 않는 사용자이면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFoundExceptionWhenUserDoesNotExist() {
            // arrange
            val brandId = registerBrand()
            val product = registerProduct(brandId = brandId)
            val criteria = createOrderCriteria(
                loginId = "nonexistent",
                items = listOf(CreateOrderItemCriteria(productId = product.id, quantity = 1)),
            )

            // act & assert
            val result = assertThrows<CoreException> {
                userCreateOrderUseCase.execute(criteria)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("주문 목록 조회")
    @Nested
    inner class GetOrders {
        @DisplayName("주문이 존재하면, 기간 내 주문 목록을 반환한다.")
        @Test
        fun returnsOrdersWhenOrdersExistInPeriod() {
            // arrange
            registerUser()
            val brandId = registerBrand()
            val product = registerProduct(brandId = brandId, quantity = 100)
            userCreateOrderUseCase.execute(
                createOrderCriteria(items = listOf(CreateOrderItemCriteria(productId = product.id, quantity = 1))),
            )
            userCreateOrderUseCase.execute(
                createOrderCriteria(items = listOf(CreateOrderItemCriteria(productId = product.id, quantity = 2))),
            )
            val today = LocalDate.now()
            val criteria = GetOrdersCriteria(
                loginId = DEFAULT_USERNAME,
                startAt = today,
                endAt = today,
                page = 0,
                size = 10,
            )

            // act
            val result = userGetOrdersUseCase.execute(criteria)

            // assert
            assertAll(
                { assertThat(result.content).hasSize(2) },
                { assertThat(result.hasNext).isFalse() },
            )
        }

        @DisplayName("주문이 없으면, 빈 목록을 반환한다.")
        @Test
        fun returnsEmptyListWhenNoOrdersExist() {
            // arrange
            registerUser()
            val today = LocalDate.now()
            val criteria = GetOrdersCriteria(
                loginId = DEFAULT_USERNAME,
                startAt = today,
                endAt = today,
                page = 0,
                size = 10,
            )

            // act
            val result = userGetOrdersUseCase.execute(criteria)

            // assert
            assertThat(result.content).isEmpty()
        }
    }

    @DisplayName("주문 상세 조회")
    @Nested
    inner class GetOrder {
        @DisplayName("본인의 주문이면, 주문 상세 정보를 반환한다.")
        @Test
        fun returnsOrderDetailWhenOrderBelongsToUser() {
            // arrange
            registerUser()
            val brandId = registerBrand()
            val product = registerProduct(brandId = brandId, quantity = 100)
            val orderResult = userCreateOrderUseCase.execute(
                createOrderCriteria(items = listOf(CreateOrderItemCriteria(productId = product.id, quantity = 2))),
            )
            val criteria = GetOrderCriteria(loginId = DEFAULT_USERNAME, orderId = orderResult.id)

            // act
            val result = userGetOrderUseCase.execute(criteria)

            // assert
            assertAll(
                { assertThat(result.id).isEqualTo(orderResult.id) },
                { assertThat(result.status).isEqualTo(OrderStatus.ORDERED) },
                { assertThat(result.totalPrice).isEqualByComparingTo(DEFAULT_PRICE.multiply(BigDecimal(2))) },
                { assertThat(result.items).hasSize(1) },
                { assertThat(result.items[0].productName).isEqualTo(DEFAULT_PRODUCT_NAME) },
                { assertThat(result.items[0].quantity).isEqualTo(2) },
            )
        }

        @DisplayName("존재하지 않는 주문이면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFoundExceptionWhenOrderDoesNotExist() {
            // arrange
            registerUser()
            val criteria = GetOrderCriteria(loginId = DEFAULT_USERNAME, orderId = 999L)

            // act & assert
            val result = assertThrows<CoreException> {
                userGetOrderUseCase.execute(criteria)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("타인의 주문이면, UNAUTHORIZED 예외가 발생한다.")
        @Test
        fun throwsUnauthorizedExceptionWhenOrderBelongsToAnotherUser() {
            // arrange
            registerUser()
            registerUser(username = "otheruser")
            val brandId = registerBrand()
            val product = registerProduct(brandId = brandId, quantity = 100)
            val orderResult = userCreateOrderUseCase.execute(
                createOrderCriteria(
                    loginId = DEFAULT_USERNAME,
                    items = listOf(CreateOrderItemCriteria(productId = product.id, quantity = 1)),
                ),
            )
            val criteria = GetOrderCriteria(loginId = "otheruser", orderId = orderResult.id)

            // act & assert
            val result = assertThrows<CoreException> {
                userGetOrderUseCase.execute(criteria)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.UNAUTHORIZED)
        }
    }
}

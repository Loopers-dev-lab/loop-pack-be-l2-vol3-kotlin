package com.loopers.domain.order

import com.loopers.application.order.CancelOrderCriteria
import com.loopers.application.order.UserCancelOrderUseCase
import com.loopers.domain.catalog.BrandModel
import com.loopers.domain.catalog.ProductModel
import com.loopers.domain.coupon.CouponModel
import com.loopers.domain.coupon.CouponStatus
import com.loopers.domain.coupon.DiscountType
import com.loopers.domain.coupon.IssuedCouponModel
import com.loopers.domain.user.Email
import com.loopers.domain.user.Password
import com.loopers.domain.user.UserModel
import com.loopers.domain.user.Username
import com.loopers.infrastructure.catalog.BrandJpaRepository
import com.loopers.infrastructure.catalog.ProductJpaRepository
import com.loopers.infrastructure.coupon.CouponJpaRepository
import com.loopers.infrastructure.coupon.IssuedCouponJpaRepository
import com.loopers.infrastructure.order.OrderItemJpaRepository
import com.loopers.infrastructure.order.OrderJpaRepository
import com.loopers.infrastructure.user.UserJpaRepository
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
import java.time.ZonedDateTime

@SpringBootTest
class UserCancelOrderUseCaseIntegrationTest @Autowired constructor(
    private val userCancelOrderUseCase: UserCancelOrderUseCase,
    private val orderJpaRepository: OrderJpaRepository,
    private val orderItemJpaRepository: OrderItemJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val userJpaRepository: UserJpaRepository,
    private val couponJpaRepository: CouponJpaRepository,
    private val issuedCouponJpaRepository: IssuedCouponJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val DEFAULT_USERNAME = "testuser"
        private const val DEFAULT_PASSWORD = "password1234"
        private const val DEFAULT_NAME = "테스트유저"
        private const val DEFAULT_EMAIL = "test@example.com"
        private val DEFAULT_BIRTH_DATE: ZonedDateTime = ZonedDateTime.parse("1990-01-01T00:00:00+09:00")
        private const val DEFAULT_PRODUCT_NAME = "에어맥스 90"
        private const val DEFAULT_PRODUCT_QUANTITY = 10
        private val DEFAULT_PRODUCT_PRICE = BigDecimal("129000")
        private const val DEFAULT_ORDER_ITEM_QUANTITY = 2
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createUser(username: String = DEFAULT_USERNAME): UserModel {
        return userJpaRepository.save(
            UserModel(
                username = Username.of(username),
                password = Password.of(DEFAULT_PASSWORD, DEFAULT_BIRTH_DATE),
                name = DEFAULT_NAME,
                email = Email.of(DEFAULT_EMAIL),
                birthDate = DEFAULT_BIRTH_DATE,
            ),
        )
    }

    private fun createBrand(): BrandModel {
        return brandJpaRepository.save(BrandModel(name = "나이키"))
    }

    private fun createProduct(
        brandId: Long,
        quantity: Int = DEFAULT_PRODUCT_QUANTITY,
    ): ProductModel {
        return productJpaRepository.save(
            ProductModel(
                brandId = brandId,
                name = DEFAULT_PRODUCT_NAME,
                quantity = quantity,
                price = DEFAULT_PRODUCT_PRICE,
            ),
        )
    }

    private fun createOrder(
        userId: Long,
        status: OrderStatus = OrderStatus.ORDERED,
        issuedCouponId: Long? = null,
    ): OrderModel {
        val originalPrice = DEFAULT_PRODUCT_PRICE * BigDecimal(DEFAULT_ORDER_ITEM_QUANTITY)
        return orderJpaRepository.save(
            OrderModel(
                userId = userId,
                status = status,
                originalPrice = originalPrice,
                totalPrice = originalPrice,
                issuedCouponId = issuedCouponId,
            ),
        )
    }

    private fun createOrderItem(orderId: Long, productId: Long): OrderItemModel {
        return orderItemJpaRepository.save(
            OrderItemModel(
                orderId = orderId,
                productId = productId,
                productName = DEFAULT_PRODUCT_NAME,
                quantity = DEFAULT_ORDER_ITEM_QUANTITY,
                price = DEFAULT_PRODUCT_PRICE,
            ),
        )
    }

    private fun createCoupon(): CouponModel {
        return couponJpaRepository.save(
            CouponModel(
                name = "테스트 쿠폰",
                discountType = DiscountType.FIXED,
                discountValue = 5000,
                totalQuantity = 100,
                expiredAt = ZonedDateTime.now().plusDays(30),
            ),
        )
    }

    private fun createIssuedCoupon(couponId: Long, userId: Long, used: Boolean = false): IssuedCouponModel {
        val issuedCoupon = IssuedCouponModel(couponId = couponId, userId = userId)
        if (used) issuedCoupon.use()
        return issuedCouponJpaRepository.save(issuedCoupon)
    }

    @DisplayName("주문 취소")
    @Nested
    inner class CancelOrder {

        @DisplayName("ORDERED 상태 주문을 취소하면, 상태가 CANCELLED로 변경되고 재고가 복구된다.")
        @Test
        fun cancelsOrderAndRestoresStockWhenStatusIsOrdered() {
            // arrange
            val expectedRestoredQuantity = DEFAULT_PRODUCT_QUANTITY
            val stockAfterOrder = DEFAULT_PRODUCT_QUANTITY - DEFAULT_ORDER_ITEM_QUANTITY

            val user = createUser()
            val brand = createBrand()
            val product = createProduct(brandId = brand.id, quantity = stockAfterOrder)
            val order = createOrder(userId = user.id)
            createOrderItem(orderId = order.id, productId = product.id)

            val criteria = CancelOrderCriteria(loginId = DEFAULT_USERNAME, orderId = order.id)

            // act
            val result = userCancelOrderUseCase.execute(criteria)

            // assert
            val cancelledOrder = orderJpaRepository.findById(result.id).get()
            val restoredProduct = productJpaRepository.findById(product.id).get()

            assertAll(
                { assertThat(cancelledOrder.status).isEqualTo(OrderStatus.CANCELLED) },
                { assertThat(restoredProduct.quantity).isEqualTo(expectedRestoredQuantity) },
            )
        }

        @DisplayName("쿠폰 사용 주문을 취소하면, 쿠폰이 AVAILABLE 상태로 복구된다.")
        @Test
        fun restoresCouponWhenCancellingOrderWithCoupon() {
            // arrange
            val stockAfterOrder = DEFAULT_PRODUCT_QUANTITY - DEFAULT_ORDER_ITEM_QUANTITY

            val user = createUser()
            val brand = createBrand()
            val product = createProduct(brandId = brand.id, quantity = stockAfterOrder)
            val coupon = createCoupon()
            val issuedCoupon = createIssuedCoupon(couponId = coupon.id, userId = user.id, used = true)

            val order = createOrder(userId = user.id, issuedCouponId = issuedCoupon.id)
            createOrderItem(orderId = order.id, productId = product.id)

            val criteria = CancelOrderCriteria(loginId = DEFAULT_USERNAME, orderId = order.id)

            // act
            userCancelOrderUseCase.execute(criteria)

            // assert
            val restoredCoupon = issuedCouponJpaRepository.findById(issuedCoupon.id).get()
            assertAll(
                { assertThat(restoredCoupon.status).isEqualTo(CouponStatus.AVAILABLE) },
                { assertThat(restoredCoupon.usedAt).isNull() },
            )
        }

        @DisplayName("다른 사용자의 주문을 취소하면, UNAUTHORIZED 예외가 발생한다.")
        @Test
        fun throwsUnauthorizedExceptionWhenCancellingOtherUsersOrder() {
            // arrange
            val owner = createUser("owner")
            val other = createUser("other")
            val brand = createBrand()
            val product = createProduct(brandId = brand.id)
            val order = createOrder(userId = owner.id)
            createOrderItem(orderId = order.id, productId = product.id)

            val criteria = CancelOrderCriteria(loginId = "other", orderId = order.id)

            // act & assert
            val result = assertThrows<CoreException> {
                userCancelOrderUseCase.execute(criteria)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.UNAUTHORIZED)
        }

        @DisplayName("존재하지 않는 주문을 취소하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFoundExceptionWhenOrderDoesNotExist() {
            // arrange
            createUser()
            val criteria = CancelOrderCriteria(loginId = DEFAULT_USERNAME, orderId = 999L)

            // act & assert
            val result = assertThrows<CoreException> {
                userCancelOrderUseCase.execute(criteria)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("CANCELLED 상태 주문을 취소하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestExceptionWhenStatusIsCancelled() {
            // arrange
            val user = createUser()
            val brand = createBrand()
            val product = createProduct(brandId = brand.id)
            val order = createOrder(userId = user.id, status = OrderStatus.CANCELLED)
            createOrderItem(orderId = order.id, productId = product.id)

            val criteria = CancelOrderCriteria(loginId = DEFAULT_USERNAME, orderId = order.id)

            // act & assert
            val result = assertThrows<CoreException> {
                userCancelOrderUseCase.execute(criteria)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}

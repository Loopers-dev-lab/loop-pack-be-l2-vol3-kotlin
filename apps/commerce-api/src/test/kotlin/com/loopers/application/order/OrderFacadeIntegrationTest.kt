package com.loopers.application.order

import com.loopers.domain.brand.Brand
import com.loopers.domain.coupon.CouponTemplate
import com.loopers.domain.coupon.CouponType
import com.loopers.domain.coupon.IssuedCoupon
import com.loopers.domain.product.Product
import com.loopers.domain.user.User
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.coupon.CouponTemplateJpaRepository
import com.loopers.infrastructure.coupon.IssuedCouponJpaRepository
import com.loopers.infrastructure.order.OrderJpaRepository
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import java.time.LocalDate
import java.time.ZonedDateTime

@SpringBootTest
class OrderFacadeIntegrationTest @Autowired constructor(
    private val orderFacade: OrderFacade,
    private val userJpaRepository: UserJpaRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val couponTemplateJpaRepository: CouponTemplateJpaRepository,
    private val issuedCouponJpaRepository: IssuedCouponJpaRepository,
    private val orderJpaRepository: OrderJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    private lateinit var user: User
    private lateinit var product: Product

    companion object {
        private const val PASSWORD = "abcd1234"
    }

    @BeforeEach
    fun setUp() {
        user = userJpaRepository.save(User(loginId = "testuser1", password = PASSWORD, name = "테스트유저", birth = "2000-01-01", email = "test@test.com"))
        val brand = brandJpaRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
        product = productJpaRepository.save(Product(brandId = brand.id, name = "에어맥스", description = "운동화", price = 139000, stockQuantity = 100))
    }

    @AfterEach
    fun cleanUp() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("주문을 생성할 때, ")
    @Nested
    inner class CreateOrder {
        @DisplayName("쿠폰 없이 주문하면, 주문이 생성되고 재고가 차감된다.")
        @Test
        fun createsOrder_whenValidRequestWithoutCoupon() {
            // arrange
            val initialStock = product.stockQuantity

            // act
            val result = orderFacade.createOrder(
                loginId = user.loginId,
                password = PASSWORD,
                itemRequests = listOf(OrderItemRequest(productId = product.id, quantity = 2)),
            )

            // assert
            val updatedProduct = productJpaRepository.findById(product.id).get()
            assertAll(
                { assertThat(result.userId).isEqualTo(user.id) },
                { assertThat(result.items).hasSize(1) },
                { assertThat(result.originalTotalPrice).isEqualTo(139000 * 2L) },
                { assertThat(result.discountAmount).isEqualTo(0L) },
                { assertThat(result.totalPrice).isEqualTo(139000 * 2L) },
                { assertThat(result.couponId).isNull() },
                { assertThat(updatedProduct.stockQuantity).isEqualTo(initialStock - 2) },
            )
        }

        @DisplayName("정액 쿠폰과 함께 주문하면, 할인이 적용된 주문이 생성된다.")
        @Test
        fun createsOrderWithFixedDiscount_whenFixedCouponProvided() {
            // arrange
            val template = couponTemplateJpaRepository.save(
                CouponTemplate(name = "10000원 할인", type = CouponType.FIXED, value = 10000, expiredAt = ZonedDateTime.now().plusDays(30)),
            )
            val coupon = issuedCouponJpaRepository.save(IssuedCoupon(userId = user.id, couponTemplateId = template.id))

            // act
            val result = orderFacade.createOrder(
                loginId = user.loginId,
                password = PASSWORD,
                itemRequests = listOf(OrderItemRequest(productId = product.id, quantity = 1)),
                couponId = coupon.id,
            )

            // assert
            val updatedCoupon = issuedCouponJpaRepository.findById(coupon.id).get()
            assertAll(
                { assertThat(result.originalTotalPrice).isEqualTo(139000L) },
                { assertThat(result.discountAmount).isEqualTo(10000L) },
                { assertThat(result.totalPrice).isEqualTo(129000L) },
                { assertThat(result.couponId).isEqualTo(coupon.id) },
                { assertThat(updatedCoupon.used).isTrue() },
            )
        }

        @DisplayName("정률 쿠폰과 함께 주문하면, 비율에 맞게 할인이 적용된다.")
        @Test
        fun createsOrderWithRateDiscount_whenRateCouponProvided() {
            // arrange
            val template = couponTemplateJpaRepository.save(
                CouponTemplate(name = "10% 할인", type = CouponType.RATE, value = 10, expiredAt = ZonedDateTime.now().plusDays(30)),
            )
            val coupon = issuedCouponJpaRepository.save(IssuedCoupon(userId = user.id, couponTemplateId = template.id))

            // act
            val result = orderFacade.createOrder(
                loginId = user.loginId,
                password = PASSWORD,
                itemRequests = listOf(OrderItemRequest(productId = product.id, quantity = 1)),
                couponId = coupon.id,
            )

            // assert
            assertAll(
                { assertThat(result.originalTotalPrice).isEqualTo(139000L) },
                { assertThat(result.discountAmount).isEqualTo(13900L) },
                { assertThat(result.totalPrice).isEqualTo(125100L) },
            )
        }

        @DisplayName("여러 상품을 주문하면, 모든 상품의 재고가 차감된다.")
        @Test
        fun decreasesAllProductStocks_whenMultipleItemsOrdered() {
            // arrange
            val brand = brandJpaRepository.findAll().first()
            val product2 = productJpaRepository.save(Product(brandId = brand.id, name = "에어포스", description = "스니커즈", price = 119000, stockQuantity = 50))

            // act
            val result = orderFacade.createOrder(
                loginId = user.loginId,
                password = PASSWORD,
                itemRequests = listOf(
                    OrderItemRequest(productId = product.id, quantity = 2),
                    OrderItemRequest(productId = product2.id, quantity = 3),
                ),
            )

            // assert
            val updatedProduct1 = productJpaRepository.findById(product.id).get()
            val updatedProduct2 = productJpaRepository.findById(product2.id).get()
            assertAll(
                { assertThat(result.items).hasSize(2) },
                { assertThat(result.totalPrice).isEqualTo(139000 * 2L + 119000 * 3L) },
                { assertThat(updatedProduct1.stockQuantity).isEqualTo(98) },
                { assertThat(updatedProduct2.stockQuantity).isEqualTo(47) },
            )
        }

        @DisplayName("재고가 부족하면, 주문이 실패하고 재고가 롤백된다.")
        @Test
        fun orderFails_andStockIsRolledBack_whenStockInsufficient() {
            // arrange
            val initialStock = product.stockQuantity

            // act
            val exception = assertThrows<CoreException> {
                orderFacade.createOrder(
                    loginId = user.loginId,
                    password = PASSWORD,
                    itemRequests = listOf(OrderItemRequest(productId = product.id, quantity = 999)),
                )
            }

            // assert
            val updatedProduct = productJpaRepository.findById(product.id).get()
            val orders = orderJpaRepository.findAll()
            assertAll(
                { assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST) },
                { assertThat(orders).isEmpty() },
                { assertThat(updatedProduct.stockQuantity).isEqualTo(initialStock) },
            )
        }

        @DisplayName("인증에 실패하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenAuthenticationFails() {
            // act
            val exception = assertThrows<CoreException> {
                orderFacade.createOrder(
                    loginId = "wronguser1",
                    password = "wrongpass1",
                    itemRequests = listOf(OrderItemRequest(productId = product.id, quantity = 1)),
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("이미 사용된 쿠폰으로 주문하면, 주문이 실패하고 재고가 롤백된다.")
        @Test
        fun orderFails_andStockIsRolledBack_whenCouponAlreadyUsed() {
            // arrange
            val template = couponTemplateJpaRepository.save(
                CouponTemplate(name = "1000원 할인", type = CouponType.FIXED, value = 1000, expiredAt = ZonedDateTime.now().plusDays(30)),
            )
            val coupon = issuedCouponJpaRepository.save(IssuedCoupon(userId = user.id, couponTemplateId = template.id))
            val initialStock = product.stockQuantity

            orderFacade.createOrder(
                loginId = user.loginId,
                password = PASSWORD,
                itemRequests = listOf(OrderItemRequest(productId = product.id, quantity = 1)),
                couponId = coupon.id,
            )

            // act
            val exception = assertThrows<CoreException> {
                orderFacade.createOrder(
                    loginId = user.loginId,
                    password = PASSWORD,
                    itemRequests = listOf(OrderItemRequest(productId = product.id, quantity = 1)),
                    couponId = coupon.id,
                )
            }

            // assert
            val updatedProduct = productJpaRepository.findById(product.id).get()
            val orders = orderJpaRepository.findAll()
            assertAll(
                { assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST) },
                { assertThat(orders).hasSize(1) },
                { assertThat(updatedProduct.stockQuantity).isEqualTo(initialStock - 1) },
            )
        }

        @DisplayName("만료된 쿠폰으로 주문하면, 주문이 실패하고 재고가 롤백된다.")
        @Test
        fun orderFails_andStockIsRolledBack_whenCouponExpired() {
            // arrange
            val template = couponTemplateJpaRepository.save(
                CouponTemplate(name = "1000원 할인", type = CouponType.FIXED, value = 1000, expiredAt = ZonedDateTime.now().minusDays(1)),
            )
            val coupon = issuedCouponJpaRepository.save(IssuedCoupon(userId = user.id, couponTemplateId = template.id))
            val initialStock = product.stockQuantity

            // act
            val exception = assertThrows<CoreException> {
                orderFacade.createOrder(
                    loginId = user.loginId,
                    password = PASSWORD,
                    itemRequests = listOf(OrderItemRequest(productId = product.id, quantity = 1)),
                    couponId = coupon.id,
                )
            }

            // assert
            val updatedProduct = productJpaRepository.findById(product.id).get()
            val updatedCoupon = issuedCouponJpaRepository.findById(coupon.id).get()
            val orders = orderJpaRepository.findAll()
            assertAll(
                { assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST) },
                { assertThat(orders).isEmpty() },
                { assertThat(updatedProduct.stockQuantity).isEqualTo(initialStock) },
                { assertThat(updatedCoupon.used).isFalse() },
            )
        }

        @DisplayName("최소 주문 금액 미달 쿠폰으로 주문하면, 주문이 실패하고 재고가 롤백된다.")
        @Test
        fun orderFails_andStockIsRolledBack_whenMinOrderAmountNotMet() {
            // arrange
            val template = couponTemplateJpaRepository.save(
                CouponTemplate(
                    name = "1000원 할인",
                    type = CouponType.FIXED,
                    value = 1000,
                    minOrderAmount = 999_999,
                    expiredAt = ZonedDateTime.now().plusDays(30),
                ),
            )
            val coupon = issuedCouponJpaRepository.save(IssuedCoupon(userId = user.id, couponTemplateId = template.id))
            val initialStock = product.stockQuantity

            // act
            val exception = assertThrows<CoreException> {
                orderFacade.createOrder(
                    loginId = user.loginId,
                    password = PASSWORD,
                    itemRequests = listOf(OrderItemRequest(productId = product.id, quantity = 1)),
                    couponId = coupon.id,
                )
            }

            // assert
            val updatedProduct = productJpaRepository.findById(product.id).get()
            val updatedCoupon = issuedCouponJpaRepository.findById(coupon.id).get()
            val orders = orderJpaRepository.findAll()
            assertAll(
                { assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST) },
                { assertThat(orders).isEmpty() },
                { assertThat(updatedProduct.stockQuantity).isEqualTo(initialStock) },
                { assertThat(updatedCoupon.used).isFalse() },
            )
        }

        @DisplayName("다른 사용자의 쿠폰으로 주문하면, 주문이 실패하고 재고가 롤백된다.")
        @Test
        fun orderFails_andStockIsRolledBack_whenCouponBelongsToOtherUser() {
            // arrange
            val otherUser = userJpaRepository.save(User(loginId = "testuser2", password = PASSWORD, name = "다른유저", birth = "2000-01-01", email = "other@test.com"))
            val template = couponTemplateJpaRepository.save(
                CouponTemplate(name = "1000원 할인", type = CouponType.FIXED, value = 1000, expiredAt = ZonedDateTime.now().plusDays(30)),
            )
            val otherUserCoupon = issuedCouponJpaRepository.save(IssuedCoupon(userId = otherUser.id, couponTemplateId = template.id))
            val initialStock = product.stockQuantity

            // act
            val exception = assertThrows<CoreException> {
                orderFacade.createOrder(
                    loginId = user.loginId,
                    password = PASSWORD,
                    itemRequests = listOf(OrderItemRequest(productId = product.id, quantity = 1)),
                    couponId = otherUserCoupon.id,
                )
            }

            // assert
            val updatedProduct = productJpaRepository.findById(product.id).get()
            val updatedCoupon = issuedCouponJpaRepository.findById(otherUserCoupon.id).get()
            val orders = orderJpaRepository.findAll()
            assertAll(
                { assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST) },
                { assertThat(orders).isEmpty() },
                { assertThat(updatedProduct.stockQuantity).isEqualTo(initialStock) },
                { assertThat(updatedCoupon.used).isFalse() },
            )
        }
    }

    @DisplayName("유저의 주문 목록을 조회할 때, ")
    @Nested
    inner class GetUserOrders {
        @DisplayName("날짜 범위 내 주문이 있으면, 목록을 반환한다.")
        @Test
        fun returnsOrders_whenOrdersExistInDateRange() {
            // arrange
            orderFacade.createOrder(
                loginId = user.loginId,
                password = PASSWORD,
                itemRequests = listOf(OrderItemRequest(productId = product.id, quantity = 1)),
            )
            val today = LocalDate.now()

            // act
            val result = orderFacade.getUserOrders(user.loginId, PASSWORD, today.minusDays(1), today.plusDays(1))

            // assert
            assertThat(result).hasSize(1)
        }

        @DisplayName("날짜 범위 밖의 주문은 반환하지 않는다.")
        @Test
        fun returnsEmptyList_whenNoOrdersInDateRange() {
            // arrange
            orderFacade.createOrder(
                loginId = user.loginId,
                password = PASSWORD,
                itemRequests = listOf(OrderItemRequest(productId = product.id, quantity = 1)),
            )

            // act
            val result = orderFacade.getUserOrders(user.loginId, PASSWORD, LocalDate.now().plusDays(10), LocalDate.now().plusDays(20))

            // assert
            assertThat(result).isEmpty()
        }

        @DisplayName("인증에 실패하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenAuthenticationFails() {
            // act
            val exception = assertThrows<CoreException> {
                orderFacade.getUserOrders("wronguser1", "wrongpass1", LocalDate.now(), LocalDate.now())
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("주문을 단건 조회할 때, ")
    @Nested
    inner class GetOrder {
        @DisplayName("본인의 주문이면, 주문 정보를 반환한다.")
        @Test
        fun returnsOrder_whenOrderBelongsToUser() {
            // arrange
            val created = orderFacade.createOrder(
                loginId = user.loginId,
                password = PASSWORD,
                itemRequests = listOf(OrderItemRequest(productId = product.id, quantity = 1)),
            )

            // act
            val result = orderFacade.getOrder(user.loginId, PASSWORD, created.id)

            // assert
            assertAll(
                { assertThat(result.id).isEqualTo(created.id) },
                { assertThat(result.userId).isEqualTo(user.id) },
            )
        }

        @DisplayName("다른 사용자의 주문이면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenOrderBelongsToOtherUser() {
            // arrange
            val created = orderFacade.createOrder(
                loginId = user.loginId,
                password = PASSWORD,
                itemRequests = listOf(OrderItemRequest(productId = product.id, quantity = 1)),
            )
            val otherUser = userJpaRepository.save(User(loginId = "testuser2", password = PASSWORD, name = "다른유저", birth = "2000-01-01", email = "other@test.com"))

            // act
            val exception = assertThrows<CoreException> {
                orderFacade.getOrder(otherUser.loginId, PASSWORD, created.id)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("존재하지 않는 주문 ID이면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenOrderNotExists() {
            // act
            val exception = assertThrows<CoreException> {
                orderFacade.getOrder(user.loginId, PASSWORD, 999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("전체 주문 목록을 조회할 때, ")
    @Nested
    inner class GetOrders {
        @DisplayName("주문이 있으면, 페이지네이션된 목록을 반환한다.")
        @Test
        fun returnsPagedOrders_whenOrdersExist() {
            // arrange
            repeat(3) {
                orderFacade.createOrder(
                    loginId = user.loginId,
                    password = PASSWORD,
                    itemRequests = listOf(OrderItemRequest(productId = product.id, quantity = 1)),
                )
            }

            // act
            val result = orderFacade.getOrders(PageRequest.of(0, 2))

            // assert
            assertAll(
                { assertThat(result.content).hasSize(2) },
                { assertThat(result.totalElements).isEqualTo(3) },
                { assertThat(result.totalPages).isEqualTo(2) },
            )
        }
    }

    @DisplayName("관리자가 주문을 조회할 때, ")
    @Nested
    inner class GetOrderForAdmin {
        @DisplayName("주문 ID로 조회하면, 사용자 관계없이 주문 정보를 반환한다.")
        @Test
        fun returnsOrder_regardlessOfUser() {
            // arrange
            val created = orderFacade.createOrder(
                loginId = user.loginId,
                password = PASSWORD,
                itemRequests = listOf(OrderItemRequest(productId = product.id, quantity = 1)),
            )

            // act
            val result = orderFacade.getOrderForAdmin(created.id)

            // assert
            assertAll(
                { assertThat(result.id).isEqualTo(created.id) },
                { assertThat(result.userId).isEqualTo(user.id) },
            )
        }

        @DisplayName("존재하지 않는 주문 ID이면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenOrderNotExists() {
            // act
            val exception = assertThrows<CoreException> {
                orderFacade.getOrderForAdmin(999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}

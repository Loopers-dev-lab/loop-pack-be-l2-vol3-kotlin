package com.loopers.application.order

import com.loopers.application.brand.BrandCommand
import com.loopers.application.brand.RegisterBrandUseCase
import com.loopers.application.coupon.CouponCommand
import com.loopers.application.coupon.IssueCouponUseCase
import com.loopers.application.coupon.RegisterCouponUseCase
import com.loopers.application.product.ProductCommand
import com.loopers.application.product.RegisterProductUseCase
import com.loopers.application.user.RegisterUserUseCase
import com.loopers.application.user.UserCommand
import com.loopers.domain.coupon.CouponType
import com.loopers.domain.coupon.UserCoupon
import com.loopers.domain.coupon.UserCouponRepository
import com.loopers.domain.product.ProductRepository
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.CouponErrorCode
import com.loopers.support.error.OrderErrorCode
import com.loopers.support.error.OrderValidationException
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
import java.time.LocalDate
import java.time.ZonedDateTime

@SpringBootTest
class CreateOrderUseCaseTest @Autowired constructor(
    private val createOrderUseCase: CreateOrderUseCase,
    private val registerUserUseCase: RegisterUserUseCase,
    private val registerBrandUseCase: RegisterBrandUseCase,
    private val registerProductUseCase: RegisterProductUseCase,
    private val registerCouponUseCase: RegisterCouponUseCase,
    private val issueCouponUseCase: IssueCouponUseCase,
    private val productRepository: ProductRepository,
    private val userCouponRepository: UserCouponRepository,
    private val userJpaRepository: UserJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun registerUser(loginId: String = "testuser"): Long {
        registerUserUseCase.execute(
            UserCommand.Register(
                loginId = loginId,
                rawPassword = "Test123!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 1),
                email = "test@example.com",
            ),
        )
        return userJpaRepository.findByLoginId(loginId)!!.id
    }

    private fun registerBrand(name: String = "나이키"): Long {
        return registerBrandUseCase.execute(BrandCommand.Register(name = name)).id
    }

    private fun registerProduct(brandId: Long, name: String = "테스트 상품", price: Long = 10000, stock: Int = 100): Long {
        return registerProductUseCase.execute(
            ProductCommand.Register(
                brandId = brandId,
                name = name,
                description = "상품 설명",
                price = price,
                stock = stock,
                imageUrl = "https://example.com/image.jpg",
            ),
        ).id
    }

    private fun registerCoupon(
        type: CouponType = CouponType.FIXED,
        value: Long = 1000,
        minOrderAmount: Long? = null,
        expiredAt: ZonedDateTime = ZonedDateTime.now().plusDays(30),
    ): Long {
        return registerCouponUseCase.execute(
            CouponCommand.Register(
                name = "테스트쿠폰",
                type = type,
                value = value,
                minOrderAmount = minOrderAmount,
                expiredAt = expiredAt,
            ),
        ).id
    }

    private fun issueCoupon(couponId: Long, userId: Long): Long {
        return issueCouponUseCase.execute(
            CouponCommand.Issue(couponId = couponId, userId = userId),
        ).id
    }

    @DisplayName("주문 생성")
    @Nested
    inner class Execute {

        @DisplayName("정상 주문 시 성공하고 재고가 차감된다")
        @Test
        fun success() {
            // arrange
            val userId = registerUser()
            val brandId = registerBrand()
            val productId = registerProduct(brandId, price = 10000, stock = 50)

            // act
            val result = createOrderUseCase.execute(
                OrderCommand.Create(
                    userId = userId,
                    items = listOf(
                        OrderCommand.Create.OrderLineItem(productId = productId, quantity = 3),
                    ),
                ),
            )

            // assert
            assertAll(
                { assertThat(result.originalAmount).isEqualTo(30000) },
                { assertThat(result.discountAmount).isEqualTo(0) },
                { assertThat(result.totalAmount).isEqualTo(30000) },
                { assertThat(result.status).isEqualTo("ORDERED") },
            )

            val product = productRepository.findByIdOrNull(productId)
            assertThat(product?.stock?.quantity).isEqualTo(47)
        }

        @DisplayName("정액 쿠폰 적용 시 할인이 반영된다")
        @Test
        fun successWithFixedCoupon() {
            // arrange
            val userId = registerUser()
            val brandId = registerBrand()
            val productId = registerProduct(brandId, price = 10000, stock = 50)
            val couponId = registerCoupon(type = CouponType.FIXED, value = 3000)
            val userCouponId = issueCoupon(couponId, userId)

            // act
            val result = createOrderUseCase.execute(
                OrderCommand.Create(
                    userId = userId,
                    items = listOf(
                        OrderCommand.Create.OrderLineItem(productId = productId, quantity = 3),
                    ),
                    userCouponId = userCouponId,
                ),
            )

            // assert
            assertAll(
                { assertThat(result.originalAmount).isEqualTo(30000) },
                { assertThat(result.discountAmount).isEqualTo(3000) },
                { assertThat(result.totalAmount).isEqualTo(27000) },
            )

            val userCoupon = userCouponRepository.findByIdOrNull(userCouponId)!!
            assertThat(userCoupon.usedOrderId).isEqualTo(result.orderId)
        }

        @DisplayName("정률 쿠폰 적용 시 할인이 반영된다")
        @Test
        fun successWithRateCoupon() {
            // arrange
            val userId = registerUser()
            val brandId = registerBrand()
            val productId = registerProduct(brandId, price = 10000, stock = 50)
            val couponId = registerCoupon(type = CouponType.RATE, value = 10)
            val userCouponId = issueCoupon(couponId, userId)

            // act
            val result = createOrderUseCase.execute(
                OrderCommand.Create(
                    userId = userId,
                    items = listOf(
                        OrderCommand.Create.OrderLineItem(productId = productId, quantity = 2),
                    ),
                    userCouponId = userCouponId,
                ),
            )

            // assert
            assertAll(
                { assertThat(result.originalAmount).isEqualTo(20000) },
                { assertThat(result.discountAmount).isEqualTo(2000) },
                { assertThat(result.totalAmount).isEqualTo(18000) },
            )
        }

        @DisplayName("이미 사용된 쿠폰으로 주문하면 실패한다")
        @Test
        fun failWhenCouponAlreadyUsed() {
            // arrange
            val userId = registerUser()
            val brandId = registerBrand()
            val productId = registerProduct(brandId, price = 10000, stock = 50)
            val couponId = registerCoupon(type = CouponType.FIXED, value = 1000)
            val userCouponId = issueCoupon(couponId, userId)

            createOrderUseCase.execute(
                OrderCommand.Create(
                    userId = userId,
                    items = listOf(OrderCommand.Create.OrderLineItem(productId = productId, quantity = 1)),
                    userCouponId = userCouponId,
                ),
            )

            // act & assert
            val exception = assertThrows<CoreException> {
                createOrderUseCase.execute(
                    OrderCommand.Create(
                        userId = userId,
                        items = listOf(OrderCommand.Create.OrderLineItem(productId = productId, quantity = 1)),
                        userCouponId = userCouponId,
                    ),
                )
            }
            assertThat(exception.errorCode).isEqualTo(CouponErrorCode.COUPON_ALREADY_USED)
        }

        @DisplayName("만료된 쿠폰으로 주문하면 실패한다")
        @Test
        fun failWhenCouponExpired() {
            // arrange
            val userId = registerUser()
            val brandId = registerBrand()
            val productId = registerProduct(brandId, price = 10000, stock = 50)
            val couponId = registerCoupon(expiredAt = ZonedDateTime.now().plusDays(30))
            val expiredUserCoupon = UserCoupon.create(
                couponId = couponId,
                userId = userId,
                expiredAt = ZonedDateTime.now().minusDays(1),
            )
            val savedUserCoupon = userCouponRepository.save(expiredUserCoupon)

            // act & assert
            val exception = assertThrows<CoreException> {
                createOrderUseCase.execute(
                    OrderCommand.Create(
                        userId = userId,
                        items = listOf(OrderCommand.Create.OrderLineItem(productId = productId, quantity = 1)),
                        userCouponId = savedUserCoupon.id,
                    ),
                )
            }
            assertThat(exception.errorCode).isEqualTo(CouponErrorCode.COUPON_EXPIRED)
        }

        @DisplayName("최소 주문금액 미달 시 쿠폰 적용이 실패한다")
        @Test
        fun failWhenMinOrderAmountNotMet() {
            // arrange
            val userId = registerUser()
            val brandId = registerBrand()
            val productId = registerProduct(brandId, price = 5000, stock = 50)
            val couponId = registerCoupon(value = 1000, minOrderAmount = 20000)
            val userCouponId = issueCoupon(couponId, userId)

            // act & assert
            val exception = assertThrows<CoreException> {
                createOrderUseCase.execute(
                    OrderCommand.Create(
                        userId = userId,
                        items = listOf(OrderCommand.Create.OrderLineItem(productId = productId, quantity = 1)),
                        userCouponId = userCouponId,
                    ),
                )
            }
            assertThat(exception.errorCode).isEqualTo(CouponErrorCode.MIN_ORDER_AMOUNT_NOT_MET)
        }

        @DisplayName("다른 유저의 쿠폰으로 주문하면 실패한다")
        @Test
        fun failWhenCouponNotOwned() {
            // arrange
            val userId = registerUser("testuser")
            val otherUserId = registerUser("otheruser")
            val brandId = registerBrand()
            val productId = registerProduct(brandId, price = 10000, stock = 50)
            val couponId = registerCoupon()
            val otherUserCouponId = issueCoupon(couponId, otherUserId)

            // act & assert
            val exception = assertThrows<CoreException> {
                createOrderUseCase.execute(
                    OrderCommand.Create(
                        userId = userId,
                        items = listOf(OrderCommand.Create.OrderLineItem(productId = productId, quantity = 1)),
                        userCouponId = otherUserCouponId,
                    ),
                )
            }
            assertThat(exception.errorCode).isEqualTo(CouponErrorCode.COUPON_NOT_OWNED)
        }

        @DisplayName("미존재 상품 포함 시 검증 실패")
        @Test
        fun failWhenProductNotFound() {
            val userId = registerUser()

            val exception = assertThrows<OrderValidationException> {
                createOrderUseCase.execute(
                    OrderCommand.Create(
                        userId = userId,
                        items = listOf(
                            OrderCommand.Create.OrderLineItem(productId = 999L, quantity = 1),
                        ),
                    ),
                )
            }

            assertThat(exception.errors[0].reason).isEqualTo("PRODUCT_NOT_FOUND")
        }

        @DisplayName("삭제된 상품 포함 시 검증 실패")
        @Test
        fun failWhenProductDeleted() {
            val userId = registerUser()
            val brandId = registerBrand()
            val productId = registerProduct(brandId)

            val product = productRepository.findByIdOrNull(productId)!!
            product.delete()
            productRepository.save(product)

            val exception = assertThrows<OrderValidationException> {
                createOrderUseCase.execute(
                    OrderCommand.Create(
                        userId = userId,
                        items = listOf(
                            OrderCommand.Create.OrderLineItem(productId = productId, quantity = 1),
                        ),
                    ),
                )
            }

            assertThat(exception.errors[0].reason).isEqualTo("PRODUCT_DELETED")
        }

        @DisplayName("재고 부족 시 검증 실패")
        @Test
        fun failWhenInsufficientStock() {
            val userId = registerUser()
            val brandId = registerBrand()
            val productId = registerProduct(brandId, stock = 5)

            val exception = assertThrows<OrderValidationException> {
                createOrderUseCase.execute(
                    OrderCommand.Create(
                        userId = userId,
                        items = listOf(
                            OrderCommand.Create.OrderLineItem(productId = productId, quantity = 10),
                        ),
                    ),
                )
            }

            assertThat(exception.errors[0].reason).isEqualTo("INSUFFICIENT_STOCK")
        }

        @DisplayName("복합 오류(삭제 + 재고 부족) 시 오류 목록을 수집한다")
        @Test
        fun collectMultipleErrors() {
            val userId = registerUser()
            val brandId = registerBrand()
            val deletedProductId = registerProduct(brandId, name = "삭제 상품")
            val lowStockProductId = registerProduct(brandId, name = "재고 부족 상품", stock = 3)

            val deletedProduct = productRepository.findByIdOrNull(deletedProductId)!!
            deletedProduct.delete()
            productRepository.save(deletedProduct)

            val exception = assertThrows<OrderValidationException> {
                createOrderUseCase.execute(
                    OrderCommand.Create(
                        userId = userId,
                        items = listOf(
                            OrderCommand.Create.OrderLineItem(productId = deletedProductId, quantity = 1),
                            OrderCommand.Create.OrderLineItem(productId = lowStockProductId, quantity = 10),
                        ),
                    ),
                )
            }

            assertThat(exception.errors).hasSize(2)
        }

        @DisplayName("중복 상품 시 검증 실패")
        @Test
        fun failWhenDuplicateProduct() {
            val userId = registerUser()
            val brandId = registerBrand()
            val productId = registerProduct(brandId)

            val exception = assertThrows<CoreException> {
                createOrderUseCase.execute(
                    OrderCommand.Create(
                        userId = userId,
                        items = listOf(
                            OrderCommand.Create.OrderLineItem(productId = productId, quantity = 1),
                            OrderCommand.Create.OrderLineItem(productId = productId, quantity = 2),
                        ),
                    ),
                )
            }

            assertThat(exception.errorCode).isEqualTo(OrderErrorCode.DUPLICATE_ORDER_ITEM)
        }

        @DisplayName("빈 주문 시 검증 실패")
        @Test
        fun failWhenEmptyItems() {
            val userId = registerUser()

            val exception = assertThrows<CoreException> {
                createOrderUseCase.execute(
                    OrderCommand.Create(
                        userId = userId,
                        items = emptyList(),
                    ),
                )
            }

            assertThat(exception.errorCode).isEqualTo(OrderErrorCode.EMPTY_ORDER_ITEMS)
        }
    }
}

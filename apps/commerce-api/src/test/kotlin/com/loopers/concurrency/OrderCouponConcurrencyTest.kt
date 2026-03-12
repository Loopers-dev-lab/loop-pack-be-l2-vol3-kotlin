package com.loopers.concurrency

import com.loopers.application.brand.BrandCommand
import com.loopers.application.brand.RegisterBrandUseCase
import com.loopers.application.coupon.CouponCommand
import com.loopers.application.coupon.IssueCouponUseCase
import com.loopers.application.coupon.RegisterCouponUseCase
import com.loopers.application.order.CreateOrderUseCase
import com.loopers.application.order.OrderCommand
import com.loopers.application.product.ProductCommand
import com.loopers.application.product.RegisterProductUseCase
import com.loopers.application.user.RegisterUserUseCase
import com.loopers.application.user.UserCommand
import com.loopers.domain.coupon.CouponType
import com.loopers.domain.coupon.UserCouponRepository
import com.loopers.domain.coupon.UserCouponStatus
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.support.error.CoreException
import com.loopers.testcontainers.MySqlTestContainersConfig
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.time.LocalDate
import java.time.ZonedDateTime

@SpringBootTest
@Import(MySqlTestContainersConfig::class)
class OrderCouponConcurrencyTest @Autowired constructor(
    private val createOrderUseCase: CreateOrderUseCase,
    private val registerUserUseCase: RegisterUserUseCase,
    private val registerBrandUseCase: RegisterBrandUseCase,
    private val registerProductUseCase: RegisterProductUseCase,
    private val registerCouponUseCase: RegisterCouponUseCase,
    private val issueCouponUseCase: IssueCouponUseCase,
    private val userCouponRepository: UserCouponRepository,
    private val userJpaRepository: UserJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun registerUser(loginId: String): Long {
        registerUserUseCase.execute(
            UserCommand.Register(
                loginId = loginId,
                rawPassword = "Test123!",
                name = "테스트",
                birthDate = LocalDate.of(1990, 1, 1),
                email = "$loginId@example.com",
            ),
        )
        return userJpaRepository.findByLoginId(loginId)!!.id
    }

    private fun registerBrand(): Long {
        return registerBrandUseCase.execute(BrandCommand.Register(name = "테스트브랜드")).id
    }

    private fun registerProduct(brandId: Long): Long {
        return registerProductUseCase.execute(
            ProductCommand.Register(
                brandId = brandId,
                name = "테스트상품",
                description = "설명",
                price = 10000,
                stock = 100,
                imageUrl = "https://example.com/image.jpg",
            ),
        ).id
    }

    private fun registerCoupon(): Long {
        return registerCouponUseCase.execute(
            CouponCommand.Register(
                name = "테스트쿠폰",
                type = CouponType.FIXED,
                value = 1000,
                minOrderAmount = null,
                expiredAt = ZonedDateTime.now().plusDays(30),
            ),
        ).id
    }

    private fun issueCoupon(couponId: Long, userId: Long): Long {
        return issueCouponUseCase.execute(
            CouponCommand.Issue(couponId = couponId, userId = userId),
        ).id
    }

    @DisplayName("같은 쿠폰으로 동시에 주문하면 1건만 성공하고 나머지는 CoreException으로 실패해야 한다")
    @Test
    fun onlyOneOrderShouldSucceedWhenSameCouponUsedConcurrently() {
        // arrange
        val threadCount = 10
        val userId = registerUser("testuser")
        val brandId = registerBrand()
        val productId = registerProduct(brandId)
        val couponId = registerCoupon()
        issueCoupon(couponId, userId)

        // act
        val actions = (1..threadCount).map {
            {
                createOrderUseCase.execute(
                    OrderCommand.Create(
                        userId = userId,
                        items = listOf(OrderCommand.Create.OrderLineItem(productId = productId, quantity = 1)),
                        couponId = couponId,
                    ),
                )
            }
        }
        val results = ConcurrencyTestHelper.executeConcurrently(actions)

        val successes = results.filter { it.isSuccess }
        val failures = results.filter { it.isFailure }

        // assert
        val userCoupon = userCouponRepository.findByUserIdAndCouponId(userId, couponId)!!

        assertAll(
            { assertThat(successes).`as`("1건만 주문 성공해야 한다").hasSize(1) },
            { assertThat(userCoupon.status).`as`("쿠폰이 사용 상태여야 한다").isEqualTo(UserCouponStatus.USED) },
            {
                assertThat(failures).`as`("실패는 모두 CoreException이어야 한다")
                    .allSatisfy { result ->
                        assertThat(result.exceptionOrNull()).isInstanceOf(CoreException::class.java)
                    }
            },
        )
    }
}

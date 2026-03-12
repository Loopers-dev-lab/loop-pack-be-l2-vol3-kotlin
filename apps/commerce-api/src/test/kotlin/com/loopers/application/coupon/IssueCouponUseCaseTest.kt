package com.loopers.application.coupon

import com.loopers.application.user.RegisterUserUseCase
import com.loopers.application.user.UserCommand
import com.loopers.domain.coupon.CouponType
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.CouponErrorCode
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
class IssueCouponUseCaseTest @Autowired constructor(
    private val issueCouponUseCase: IssueCouponUseCase,
    private val registerCouponUseCase: RegisterCouponUseCase,
    private val registerUserUseCase: RegisterUserUseCase,
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
                email = "$loginId@example.com",
            ),
        )
        return userJpaRepository.findByLoginId(loginId)!!.id
    }

    private fun registerCoupon(expiredAt: ZonedDateTime = ZonedDateTime.now().plusDays(30)): CouponInfo {
        return registerCouponUseCase.execute(
            CouponCommand.Register(
                name = "테스트 쿠폰",
                type = CouponType.FIXED,
                value = 1000,
                minOrderAmount = null,
                expiredAt = expiredAt,
            ),
        )
    }

    @DisplayName("쿠폰 발급")
    @Nested
    inner class Execute {

        @DisplayName("정상 발급 시 AVAILABLE 상태로 생성된다")
        @Test
        fun success() {
            val userId = registerUser()
            val coupon = registerCoupon()

            val result = issueCouponUseCase.execute(
                CouponCommand.Issue(couponId = coupon.id, userId = userId),
            )

            assertAll(
                { assertThat(result.couponId).isEqualTo(coupon.id) },
                { assertThat(result.userId).isEqualTo(userId) },
                { assertThat(result.status).isEqualTo("AVAILABLE") },
            )
        }

        @DisplayName("이미 발급받은 쿠폰이면 ALREADY_ISSUED_COUPON 에러가 발생한다")
        @Test
        fun failWhenAlreadyIssued() {
            val userId = registerUser()
            val coupon = registerCoupon()
            issueCouponUseCase.execute(CouponCommand.Issue(couponId = coupon.id, userId = userId))

            val exception = assertThrows<CoreException> {
                issueCouponUseCase.execute(CouponCommand.Issue(couponId = coupon.id, userId = userId))
            }
            assertThat(exception.errorCode).isEqualTo(CouponErrorCode.ALREADY_ISSUED_COUPON)
        }

        @DisplayName("존재하지 않는 쿠폰이면 COUPON_NOT_FOUND 에러가 발생한다")
        @Test
        fun failWhenCouponNotFound() {
            val userId = registerUser()

            val exception = assertThrows<CoreException> {
                issueCouponUseCase.execute(CouponCommand.Issue(couponId = 999L, userId = userId))
            }
            assertThat(exception.errorCode).isEqualTo(CouponErrorCode.COUPON_NOT_FOUND)
        }

        @DisplayName("만료된 쿠폰이면 COUPON_EXPIRED 에러가 발생한다")
        @Test
        fun failWhenExpired() {
            val userId = registerUser()
            val coupon = registerCoupon(expiredAt = ZonedDateTime.now().minusDays(1))

            val exception = assertThrows<CoreException> {
                issueCouponUseCase.execute(CouponCommand.Issue(couponId = coupon.id, userId = userId))
            }
            assertThat(exception.errorCode).isEqualTo(CouponErrorCode.COUPON_EXPIRED)
        }
    }
}

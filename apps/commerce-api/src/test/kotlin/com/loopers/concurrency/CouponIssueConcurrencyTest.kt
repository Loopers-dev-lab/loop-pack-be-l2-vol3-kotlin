package com.loopers.concurrency

import com.loopers.application.coupon.CouponCommand
import com.loopers.application.coupon.IssueCouponUseCase
import com.loopers.application.coupon.RegisterCouponUseCase
import com.loopers.application.user.RegisterUserUseCase
import com.loopers.application.user.UserCommand
import com.loopers.domain.coupon.CouponType
import com.loopers.domain.coupon.UserCouponRepository
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
class CouponIssueConcurrencyTest @Autowired constructor(
    private val issueCouponUseCase: IssueCouponUseCase,
    private val registerCouponUseCase: RegisterCouponUseCase,
    private val registerUserUseCase: RegisterUserUseCase,
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

    @DisplayName("같은 유저가 같은 쿠폰을 동시에 발급 요청하면 1건만 성공하고 나머지는 CoreException으로 실패해야 한다")
    @Test
    fun onlyOneIssueShouldSucceedWhenSameUserRequestsConcurrently() {
        // arrange
        val threadCount = 10
        val userId = registerUser("testuser")
        val couponId = registerCoupon()

        // act
        val actions = (1..threadCount).map {
            { issueCouponUseCase.execute(CouponCommand.Issue(couponId = couponId, userId = userId)) }
        }
        val results = ConcurrencyTestHelper.executeConcurrently(actions)

        val successes = results.filter { it.isSuccess }
        val failures = results.filter { it.isFailure }

        // assert
        val issuedCoupon = userCouponRepository.findByUserIdAndCouponId(userId, couponId)

        assertAll(
            { assertThat(successes).`as`("1건만 발급 성공해야 한다").hasSize(1) },
            { assertThat(issuedCoupon).`as`("발급된 쿠폰이 존재해야 한다").isNotNull() },
            {
                assertThat(failures).`as`("실패는 모두 CoreException이어야 한다")
                    .allSatisfy { result ->
                        assertThat(result.exceptionOrNull()).isInstanceOf(CoreException::class.java)
                    }
            },
        )
    }
}

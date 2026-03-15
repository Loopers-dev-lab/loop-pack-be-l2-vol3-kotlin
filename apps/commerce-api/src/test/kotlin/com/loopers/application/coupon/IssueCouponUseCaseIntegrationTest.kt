package com.loopers.application.coupon

import com.loopers.application.user.RegisterUserCommand
import com.loopers.application.user.RegisterUserUseCase
import com.loopers.domain.coupon.CouponException
import com.loopers.domain.coupon.UserCouponRepository
import com.loopers.testcontainers.MySqlTestContainersConfig
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.jdbc.Sql
import java.time.ZonedDateTime

@SpringBootTest
@Import(MySqlTestContainersConfig::class)
@Sql(
    statements = [
        "DELETE FROM order_item",
        "DELETE FROM orders",
        "DELETE FROM user_coupon",
        "DELETE FROM coupon",
        "DELETE FROM users",
    ],
    executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
)
class IssueCouponUseCaseIntegrationTest {

    @Autowired
    private lateinit var issueCouponUseCase: IssueCouponUseCase

    @Autowired
    private lateinit var registerCouponUseCase: RegisterCouponUseCase

    @Autowired
    private lateinit var registerUserUseCase: RegisterUserUseCase

    @Autowired
    private lateinit var userCouponRepository: UserCouponRepository

    private var userId: Long = 0
    private var couponId: Long = 0

    @BeforeEach
    fun setUp() {
        userId = registerUserUseCase.register(createUserCommand("testuser"))
        couponId = registerCouponUseCase.register(createCouponCommand())
    }

    @Test
    fun `정상적인 경우 쿠폰이 발급되고 ID를 반환해야 한다`() {
        val result = issueCouponUseCase.issue(userId, couponId)

        assertThat(result).isPositive()
    }

    @Test
    fun `발급된 쿠폰의 정보가 올바르게 저장되어야 한다`() {
        val userCouponId = issueCouponUseCase.issue(userId, couponId)

        val userCoupon = userCouponRepository.findById(userCouponId)!!
        assertThat(userCoupon.refCouponId).isEqualTo(couponId)
        assertThat(userCoupon.refUserId).isEqualTo(userId)
        assertThat(userCoupon.status.name).isEqualTo("AVAILABLE")
    }

    @Test
    fun `이미 발급받은 쿠폰을 재발급하면 CouponException이 발생한다`() {
        issueCouponUseCase.issue(userId, couponId)

        assertThatThrownBy { issueCouponUseCase.issue(userId, couponId) }
            .isInstanceOf(CouponException::class.java)
    }

    @Test
    fun `만료된 쿠폰을 발급하면 CouponException이 발생한다`() {
        val expiredCouponId = registerCouponUseCase.register(
            createCouponCommand(expiredAt = ZonedDateTime.now().minusDays(1)),
        )

        assertThatThrownBy { issueCouponUseCase.issue(userId, expiredCouponId) }
            .isInstanceOf(CouponException::class.java)
    }

    private fun createUserCommand(loginId: String) = RegisterUserCommand(
        loginId = loginId,
        password = "Password1!",
        name = "테스트",
        birthDate = "1993-04-01",
        email = "$loginId@example.com",
        gender = "MALE",
    )

    private fun createCouponCommand(
        expiredAt: ZonedDateTime = ZonedDateTime.now().plusDays(30),
    ) = RegisterCouponCommand(
        name = "테스트쿠폰",
        discountType = "FIXED",
        discountValue = 3000L,
        minOrderAmount = 10000L,
        maxIssueCount = 100,
        expiredAt = expiredAt,
    )
}

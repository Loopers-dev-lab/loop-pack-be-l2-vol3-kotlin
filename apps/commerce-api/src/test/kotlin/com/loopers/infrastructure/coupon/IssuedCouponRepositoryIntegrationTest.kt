package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponRepository
import com.loopers.domain.coupon.IssuedCoupon
import com.loopers.domain.coupon.IssuedCouponRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import java.time.ZonedDateTime

@DisplayName("IssuedCouponRepository integration")
@SpringBootTest
class IssuedCouponRepositoryIntegrationTest
@Autowired
constructor(
    private val couponRepository: CouponRepository,
    private val issuedCouponRepository: IssuedCouponRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Test
    @DisplayName("저장 후 id와 조회 API가 모두 동작한다")
    fun save_andFind() {
        val coupon = couponRepository.save(
            Coupon.register(
                name = "테스트 쿠폰",
                type = Coupon.Type.FIXED,
                discountValue = 1000L,
                minOrderAmount = null,
                expiredAt = ZonedDateTime.now().plusDays(30),
            ),
        )

        val saved = issuedCouponRepository.save(
            IssuedCoupon.issue(
                couponId = coupon.id!!,
                userId = 1L,
                expiredAt = coupon.expiredAt,
            ),
        )

        assertThat(saved.id).isNotNull()
        assertThat(issuedCouponRepository.findById(saved.id!!)).isNotNull()
        assertThat(issuedCouponRepository.findByCouponIdAndUserId(coupon.id!!, 1L)).isNotNull()
    }

    @Test
    @DisplayName("coupon_id + user_id 중복 저장은 unique constraint에 걸린다")
    fun save_duplicate_throws() {
        val coupon = couponRepository.save(
            Coupon.register(
                name = "테스트 쿠폰",
                type = Coupon.Type.FIXED,
                discountValue = 1000L,
                minOrderAmount = null,
                expiredAt = ZonedDateTime.now().plusDays(30),
            ),
        )

        issuedCouponRepository.save(
            IssuedCoupon.issue(
                couponId = coupon.id!!,
                userId = 1L,
                expiredAt = coupon.expiredAt,
            ),
        )

        assertThrows<DataIntegrityViolationException> {
            issuedCouponRepository.save(
                IssuedCoupon.issue(
                    couponId = coupon.id!!,
                    userId = 1L,
                    expiredAt = coupon.expiredAt,
                ),
            )
        }
    }
}

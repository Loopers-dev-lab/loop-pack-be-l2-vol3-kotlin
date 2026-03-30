package com.loopers.infrastructure.order

import com.loopers.domain.order.CouponReservationRepository
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class CouponReservationRedisRepositoryTest @Autowired constructor(
    private val couponReservationRepository: CouponReservationRepository,
    private val redisCleanUp: RedisCleanUp,
) {

    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
    }

    @Nested
    @DisplayName("reserve")
    inner class Reserve {

        @Test
        @DisplayName("첫 선점 시 true를 반환한다")
        fun `첫 선점 시 true를 반환한다`() {
            // given
            val couponId = 1L
            val userId = 100L

            // when
            val result = couponReservationRepository.reserve(couponId, userId)

            // then
            assertThat(result).isTrue()
        }

        @Test
        @DisplayName("동일 쿠폰+유저 중복 선점 시 false를 반환한다")
        fun `동일 쿠폰+유저 중복 선점 시 false를 반환한다`() {
            // given
            val couponId = 1L
            val userId = 100L
            couponReservationRepository.reserve(couponId, userId)

            // when
            val result = couponReservationRepository.reserve(couponId, userId)

            // then
            assertThat(result).isFalse()
        }

        @Test
        @DisplayName("다른 유저는 같은 쿠폰을 선점할 수 있다")
        fun `다른 유저는 같은 쿠폰을 선점할 수 있다`() {
            // given
            val couponId = 1L
            couponReservationRepository.reserve(couponId, 100L)

            // when
            val result = couponReservationRepository.reserve(couponId, 200L)

            // then
            assertThat(result).isTrue()
        }
    }

    @Nested
    @DisplayName("restore")
    inner class Restore {

        @Test
        @DisplayName("복원 후 재선점에 성공한다")
        fun `복원 후 재선점에 성공한다`() {
            // given
            val couponId = 1L
            val userId = 100L
            couponReservationRepository.reserve(couponId, userId)

            // when
            couponReservationRepository.restore(couponId, userId)

            // then
            val result = couponReservationRepository.reserve(couponId, userId)
            assertThat(result).isTrue()
        }
    }
}

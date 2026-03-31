package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponIssueRequest
import com.loopers.domain.coupon.CouponIssueRequestRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException

@DisplayName("CouponIssueRequestRepository integration")
@SpringBootTest
class CouponIssueRequestRepositoryIntegrationTest
@Autowired
constructor(
    private val couponIssueRequestRepository: CouponIssueRequestRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Test
    @DisplayName("저장 후 id와 조회 API가 모두 동작한다")
    fun save_andFind() {
        val saved = couponIssueRequestRepository.save(
            CouponIssueRequest.request(
                couponId = 11L,
                userId = 1L,
            ),
        )

        assertThat(saved.id).isNotNull()
        assertThat(couponIssueRequestRepository.findById(saved.id!!)).isNotNull()
        assertThat(couponIssueRequestRepository.findByIdAndUserId(saved.id!!, 1L)).isNotNull()
        assertThat(couponIssueRequestRepository.findByCouponIdAndUserId(11L, 1L)).isNotNull()
    }

    @Test
    @DisplayName("coupon_id + user_id 중복 저장은 unique constraint에 걸린다")
    fun save_duplicate_throws() {
        couponIssueRequestRepository.save(
            CouponIssueRequest.request(
                couponId = 11L,
                userId = 1L,
            ),
        )

        assertThrows<DataIntegrityViolationException> {
            couponIssueRequestRepository.save(
                CouponIssueRequest.request(
                    couponId = 11L,
                    userId = 1L,
                ),
            )
        }
    }
}

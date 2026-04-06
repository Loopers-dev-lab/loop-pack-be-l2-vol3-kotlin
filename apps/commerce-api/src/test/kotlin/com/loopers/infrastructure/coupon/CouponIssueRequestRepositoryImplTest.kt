package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponIssueRequest
import com.loopers.domain.coupon.CouponIssueRequestRepository
import com.loopers.domain.coupon.CouponIssueStatus
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class CouponIssueRequestRepositoryImplTest @Autowired constructor(
    private val couponIssueRequestRepository: CouponIssueRequestRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("발급 요청을 저장하고 조회할 때,")
    @Nested
    inner class SaveAndFind {

        @DisplayName("저장 후 requestId로 조회하면, 저장된 발급 요청이 반환된다.")
        @Test
        fun findsByRequestId_afterSave() {
            // arrange
            val requestId = "550e8400-e29b-41d4-a716-446655440000"
            val request = CouponIssueRequest(
                requestId = requestId,
                couponId = 1L,
                userId = 100L,
            )
            couponIssueRequestRepository.save(request)

            // act
            val found = couponIssueRequestRepository.findByRequestId(requestId)

            // assert
            assertThat(found).isNotNull()
            assertAll(
                { assertThat(found!!.requestId).isEqualTo(requestId) },
                { assertThat(found!!.couponId).isEqualTo(1L) },
                { assertThat(found!!.userId).isEqualTo(100L) },
                { assertThat(found!!.status).isEqualTo(CouponIssueStatus.PENDING) },
            )
        }

        @DisplayName("존재하지 않는 requestId로 조회하면, null이 반환된다.")
        @Test
        fun returnsNull_whenRequestIdNotFound() {
            // act
            val found = couponIssueRequestRepository.findByRequestId("non-existent-id")

            // assert
            assertThat(found).isNull()
        }
    }

    @DisplayName("requestId와 userId로 조회할 때,")
    @Nested
    inner class FindByRequestIdAndUserId {

        @DisplayName("일치하는 requestId와 userId로 조회하면, 발급 요청이 반환된다.")
        @Test
        fun findsByRequestIdAndUserId() {
            // arrange
            val requestId = "550e8400-e29b-41d4-a716-446655440000"
            val userId = 100L
            couponIssueRequestRepository.save(
                CouponIssueRequest(requestId = requestId, couponId = 1L, userId = userId),
            )

            // act
            val found = couponIssueRequestRepository.findByRequestIdAndUserId(requestId, userId)

            // assert
            assertThat(found).isNotNull()
            assertThat(found!!.requestId).isEqualTo(requestId)
            assertThat(found.userId).isEqualTo(userId)
        }

        @DisplayName("requestId는 존재하지만 userId가 다르면, null이 반환된다.")
        @Test
        fun returnsNull_whenUserIdDoesNotMatch() {
            // arrange
            val requestId = "550e8400-e29b-41d4-a716-446655440000"
            couponIssueRequestRepository.save(
                CouponIssueRequest(requestId = requestId, couponId = 1L, userId = 100L),
            )

            // act
            val found = couponIssueRequestRepository.findByRequestIdAndUserId(requestId, 999L)

            // assert
            assertThat(found).isNull()
        }
    }
}

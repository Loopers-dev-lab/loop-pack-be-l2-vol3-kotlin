package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponIssueModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

class CouponIssueRepositoryImplTest {

    private val couponIssueJpaRepository: CouponIssueJpaRepository = mockk()
    private val couponIssueRepositoryImpl = CouponIssueRepositoryImpl(couponIssueJpaRepository)

    @DisplayName("발급 쿠폰을 저장할 때,")
    @Nested
    inner class Save {
        @DisplayName("JpaRepository에 위임하여 저장하고 결과를 반환한다.")
        @Test
        fun delegatesToJpaRepository() {
            // arrange
            val issue = CouponIssueModel(couponId = 1L, userId = 1L)
            every { couponIssueJpaRepository.save(issue) } returns issue

            // act
            val result = couponIssueRepositoryImpl.save(issue)

            // assert
            assertThat(result.couponId).isEqualTo(1L)
            verify(exactly = 1) { couponIssueJpaRepository.save(issue) }
        }
    }

    @DisplayName("쿠폰ID + 유저ID로 조회할 때,")
    @Nested
    inner class FindByCouponIdAndUserId {
        @DisplayName("존재하면 반환한다.")
        @Test
        fun returnsIssue_whenExists() {
            // arrange
            val issue = CouponIssueModel(couponId = 1L, userId = 1L)
            every {
                couponIssueJpaRepository.findByCouponIdAndUserIdAndDeletedAtIsNull(1L, 1L)
            } returns issue

            // act
            val result = couponIssueRepositoryImpl.findByCouponIdAndUserIdAndDeletedAtIsNull(1L, 1L)

            // assert
            assertThat(result).isNotNull
        }

        @DisplayName("존재하지 않으면 null을 반환한다.")
        @Test
        fun returnsNull_whenNotExists() {
            // arrange
            every {
                couponIssueJpaRepository.findByCouponIdAndUserIdAndDeletedAtIsNull(1L, 999L)
            } returns null

            // act
            val result = couponIssueRepositoryImpl.findByCouponIdAndUserIdAndDeletedAtIsNull(1L, 999L)

            // assert
            assertThat(result).isNull()
        }
    }

    @DisplayName("유저의 발급 쿠폰 목록을 조회할 때,")
    @Nested
    inner class FindAllByUserId {
        @DisplayName("페이징된 목록을 반환한다.")
        @Test
        fun returnsPagedIssues() {
            // arrange
            val pageable = PageRequest.of(0, 20)
            val issues = listOf(CouponIssueModel(couponId = 1L, userId = 1L))
            every {
                couponIssueJpaRepository.findAllByUserIdAndDeletedAtIsNull(1L, pageable)
            } returns PageImpl(issues)

            // act
            val result = couponIssueRepositoryImpl.findAllByUserIdAndDeletedAtIsNull(1L, pageable)

            // assert
            assertThat(result.content).hasSize(1)
        }
    }

    @DisplayName("쿠폰별 발급 내역을 조회할 때,")
    @Nested
    inner class FindAllByCouponId {
        @DisplayName("페이징된 목록을 반환한다.")
        @Test
        fun returnsPagedIssues() {
            // arrange
            val pageable = PageRequest.of(0, 20)
            val issues = listOf(CouponIssueModel(couponId = 1L, userId = 1L))
            every {
                couponIssueJpaRepository.findAllByCouponIdAndDeletedAtIsNull(1L, pageable)
            } returns PageImpl(issues)

            // act
            val result = couponIssueRepositoryImpl.findAllByCouponIdAndDeletedAtIsNull(1L, pageable)

            // assert
            assertThat(result.content).hasSize(1)
        }
    }
}

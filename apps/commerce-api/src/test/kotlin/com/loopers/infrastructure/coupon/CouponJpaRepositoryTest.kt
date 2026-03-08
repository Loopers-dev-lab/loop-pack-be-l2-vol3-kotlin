package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponStatus
import com.loopers.domain.coupon.CouponTemplate
import com.loopers.domain.coupon.CouponType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import java.math.BigDecimal
import java.time.ZonedDateTime

@SpringBootTest
@DisplayName("CouponJpaRepository")
class CouponJpaRepositoryTest @Autowired constructor(
    private val couponJpaRepository: CouponJpaRepository,
    private val couponTemplateJpaRepository: CouponTemplateJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun cleanup() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("Pagination 안정성")
    @Nested
    inner class PaginationStability {

        @DisplayName("findByUserId: 같은 createdAt을 가진 쿠폰들이 여러 페이지에서 중복이나 누락 없이 반환된다")
        @Test
        fun findByUserId_noDuplicatesOrMissingItems_whenCreatedAtIsSame() {
            // Arrange
            val userId = 1L

            // 다양한 템플릿으로 같은 userId의 쿠폰 5개 생성 (빠르게 생성되므로 createdAt이 같거나 유사)
            val coupons = (1..5).map { i ->
                val template = couponTemplateJpaRepository.save(
                    CouponTemplate.create(
                        name = "Test Template $i",
                        type = CouponType.FIXED,
                        value = BigDecimal("5000"),
                        minOrderAmount = BigDecimal("10000"),
                        expiredAt = ZonedDateTime.now().plusDays(30),
                    ),
                )
                val coupon = Coupon.issue(userId, template)
                couponJpaRepository.save(coupon)
                coupon
            }
            val createdCouponIds = coupons.map { it.id }.sorted()

            // Act & Assert
            val page0 = couponJpaRepository.findByUserId(userId, PageRequest.of(0, 2))
            val page1 = couponJpaRepository.findByUserId(userId, PageRequest.of(1, 2))
            val page2 = couponJpaRepository.findByUserId(userId, PageRequest.of(2, 2))

            val allIds = mutableListOf<Long>()
            allIds.addAll(page0.content.map { it.id })
            allIds.addAll(page1.content.map { it.id })
            allIds.addAll(page2.content.map { it.id })

            // 중복 확인
            assertThat(allIds).hasSameSizeAs(allIds.distinct())
            // 모든 쿠폰이 반환되었는지 확인
            assertThat(allIds.sorted()).isEqualTo(createdCouponIds)
        }

        @DisplayName("findByUserIdAndStatus: 같은 createdAt을 가진 쿠폰들이 여러 페이지에서 중복이나 누락 없이 반환된다")
        @Test
        fun findByUserIdAndStatus_noDuplicatesOrMissingItems_whenCreatedAtIsSame() {
            // Arrange
            val userId = 2L

            // 다양한 템플릿으로 같은 userId의 쿠폰 5개 생성
            val coupons = (1..5).map { i ->
                val template = couponTemplateJpaRepository.save(
                    CouponTemplate.create(
                        name = "Test Template $i",
                        type = CouponType.FIXED,
                        value = BigDecimal("5000"),
                        minOrderAmount = BigDecimal("10000"),
                        expiredAt = ZonedDateTime.now().plusDays(30),
                    ),
                )
                val coupon = Coupon.issue(userId, template)
                couponJpaRepository.save(coupon)
                coupon
            }
            val createdCouponIds = coupons.map { it.id }.sorted()

            // Act & Assert
            val page0 = couponJpaRepository.findByUserIdAndStatus(
                userId,
                CouponStatus.ISSUED,
                PageRequest.of(0, 2),
            )
            val page1 = couponJpaRepository.findByUserIdAndStatus(
                userId,
                CouponStatus.ISSUED,
                PageRequest.of(1, 2),
            )
            val page2 = couponJpaRepository.findByUserIdAndStatus(
                userId,
                CouponStatus.ISSUED,
                PageRequest.of(2, 2),
            )

            val allIds = mutableListOf<Long>()
            allIds.addAll(page0.content.map { it.id })
            allIds.addAll(page1.content.map { it.id })
            allIds.addAll(page2.content.map { it.id })

            // 중복 확인
            assertThat(allIds).hasSameSizeAs(allIds.distinct())
            // 모든 쿠폰이 반환되었는지 확인
            assertThat(allIds.sorted()).isEqualTo(createdCouponIds)
        }

        @DisplayName("findByTemplateId: 같은 createdAt을 가진 쿠폰들이 여러 페이지에서 중복이나 누락 없이 반환된다")
        @Test
        fun findByTemplateId_noDuplicatesOrMissingItems_whenCreatedAtIsSame() {
            // Arrange
            val template = couponTemplateJpaRepository.save(
                CouponTemplate.create(
                    name = "Test Template",
                    type = CouponType.FIXED,
                    value = BigDecimal("5000"),
                    minOrderAmount = BigDecimal("10000"),
                    expiredAt = ZonedDateTime.now().plusDays(30),
                ),
            )

            // 같은 템플릿 다른 사용자들로부터 5개 쿠폰 생성
            val coupons = (1L..5L).map { userId ->
                val coupon = Coupon.issue(userId, template)
                couponJpaRepository.save(coupon)
                coupon
            }
            val createdCouponIds = coupons.map { it.id }.sorted()

            // Act & Assert
            val page0 = couponJpaRepository.findByTemplateId(
                template.id,
                PageRequest.of(0, 2),
            )
            val page1 = couponJpaRepository.findByTemplateId(
                template.id,
                PageRequest.of(1, 2),
            )
            val page2 = couponJpaRepository.findByTemplateId(
                template.id,
                PageRequest.of(2, 2),
            )

            val allIds = mutableListOf<Long>()
            allIds.addAll(page0.content.map { it.id })
            allIds.addAll(page1.content.map { it.id })
            allIds.addAll(page2.content.map { it.id })

            // 중복 확인
            assertThat(allIds).hasSameSizeAs(allIds.distinct())
            // 모든 쿠폰이 반환되었는지 확인
            assertThat(allIds.sorted()).isEqualTo(createdCouponIds)
        }
    }
}

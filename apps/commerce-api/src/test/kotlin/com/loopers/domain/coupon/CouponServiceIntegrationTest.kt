package com.loopers.domain.coupon

import com.loopers.infrastructure.coupon.CouponTemplateJpaRepository
import com.loopers.infrastructure.coupon.IssuedCouponJpaRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
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
import org.springframework.data.domain.PageRequest
import java.time.ZonedDateTime

@SpringBootTest
class CouponServiceIntegrationTest @Autowired constructor(
    private val couponService: CouponService,
    private val couponTemplateJpaRepository: CouponTemplateJpaRepository,
    private val issuedCouponJpaRepository: IssuedCouponJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @AfterEach
    fun cleanUp() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("쿠폰 템플릿을 생성할 때, ")
    @Nested
    inner class CreateCouponTemplate {
        @DisplayName("유효한 정보가 주어지면, 쿠폰 템플릿이 생성된다.")
        @Test
        fun createsCouponTemplate_whenValidInfoProvided() {
            // arrange & act
            val template = couponService.createCouponTemplate(
                name = "1000원 할인",
                type = CouponType.FIXED,
                value = 1000,
                minOrderAmount = 5000,
                expiredAt = ZonedDateTime.now().plusDays(30),
            )

            // assert
            val saved = couponTemplateJpaRepository.findById(template.id).get()
            assertAll(
                { assertThat(saved.name).isEqualTo("1000원 할인") },
                { assertThat(saved.type).isEqualTo(CouponType.FIXED) },
                { assertThat(saved.value).isEqualTo(1000) },
                { assertThat(saved.minOrderAmount).isEqualTo(5000) },
            )
        }
    }

    @DisplayName("쿠폰 템플릿을 조회할 때, ")
    @Nested
    inner class GetCouponTemplate {
        @DisplayName("존재하는 ID로 조회하면, 쿠폰 템플릿을 반환한다.")
        @Test
        fun returnsCouponTemplate_whenExists() {
            // arrange
            val template = couponTemplateJpaRepository.save(
                CouponTemplate(name = "10% 할인", type = CouponType.RATE, value = 10, expiredAt = ZonedDateTime.now().plusDays(30)),
            )

            // act
            val result = couponService.getCouponTemplate(template.id)

            // assert
            assertThat(result.name).isEqualTo("10% 할인")
        }

        @DisplayName("존재하지 않는 ID로 조회하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenNotExists() {
            // act
            val exception = assertThrows<CoreException> {
                couponService.getCouponTemplate(999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("쿠폰 템플릿 목록을 조회할 때, ")
    @Nested
    inner class GetCouponTemplates {
        @DisplayName("템플릿이 있으면, 페이지네이션된 목록을 반환한다.")
        @Test
        fun returnsTemplateList_whenTemplatesExist() {
            // arrange
            couponTemplateJpaRepository.save(
                CouponTemplate(name = "1000원 할인", type = CouponType.FIXED, value = 1000, expiredAt = ZonedDateTime.now().plusDays(30)),
            )
            couponTemplateJpaRepository.save(
                CouponTemplate(name = "10% 할인", type = CouponType.RATE, value = 10, expiredAt = ZonedDateTime.now().plusDays(30)),
            )

            // act
            val result = couponService.getCouponTemplates(PageRequest.of(0, 20))

            // assert
            assertThat(result.content).hasSize(2)
        }
    }

    @DisplayName("쿠폰 템플릿을 수정할 때, ")
    @Nested
    inner class UpdateCouponTemplate {
        @DisplayName("유효한 수정 정보가 주어지면, 쿠폰 템플릿이 수정된다.")
        @Test
        fun updatesCouponTemplate_whenValidInfoProvided() {
            // arrange
            val template = couponTemplateJpaRepository.save(
                CouponTemplate(name = "기존 쿠폰", type = CouponType.FIXED, value = 1000, expiredAt = ZonedDateTime.now().plusDays(30)),
            )
            val newExpiredAt = ZonedDateTime.now().plusDays(60)

            // act
            val result = couponService.updateCouponTemplate(template.id, "수정된 쿠폰", 2000, 10000, newExpiredAt)

            // assert
            assertAll(
                { assertThat(result.name).isEqualTo("수정된 쿠폰") },
                { assertThat(result.value).isEqualTo(2000) },
                { assertThat(result.minOrderAmount).isEqualTo(10000) },
            )
        }
    }

    @DisplayName("쿠폰 템플릿을 삭제할 때, ")
    @Nested
    inner class DeleteCouponTemplate {
        @DisplayName("존재하는 쿠폰 템플릿을 삭제하면, soft delete 된다.")
        @Test
        fun softDeletesCouponTemplate() {
            // arrange
            val template = couponTemplateJpaRepository.save(
                CouponTemplate(name = "삭제할 쿠폰", type = CouponType.FIXED, value = 1000, expiredAt = ZonedDateTime.now().plusDays(30)),
            )

            // act
            couponService.deleteCouponTemplate(template.id)

            // assert
            val exception = assertThrows<CoreException> {
                couponService.getCouponTemplate(template.id)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("쿠폰을 발급할 때, ")
    @Nested
    inner class IssueCoupon {
        @DisplayName("유효한 템플릿이면, 쿠폰이 발급된다.")
        @Test
        fun issuesCoupon_whenTemplateIsValid() {
            // arrange
            val template = couponTemplateJpaRepository.save(
                CouponTemplate(name = "1000원 할인", type = CouponType.FIXED, value = 1000, expiredAt = ZonedDateTime.now().plusDays(30)),
            )

            // act
            val issuedCoupon = couponService.issueCoupon(1L, template.id)

            // assert
            val saved = issuedCouponJpaRepository.findById(issuedCoupon.id).get()
            assertAll(
                { assertThat(saved.userId).isEqualTo(1L) },
                { assertThat(saved.couponTemplateId).isEqualTo(template.id) },
                { assertThat(saved.used).isFalse() },
            )
        }

        @DisplayName("만료된 템플릿이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenTemplateIsExpired() {
            // arrange
            val template = couponTemplateJpaRepository.save(
                CouponTemplate(name = "만료 쿠폰", type = CouponType.FIXED, value = 1000, expiredAt = ZonedDateTime.now().minusDays(1)),
            )

            // act
            val exception = assertThrows<CoreException> {
                couponService.issueCoupon(1L, template.id)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("발급된 쿠폰을 조회할 때, ")
    @Nested
    inner class GetIssuedCoupon {
        @DisplayName("사용자의 발급 쿠폰 목록을 반환한다.")
        @Test
        fun returnsUserIssuedCoupons() {
            // arrange
            val template = couponTemplateJpaRepository.save(
                CouponTemplate(name = "1000원 할인", type = CouponType.FIXED, value = 1000, expiredAt = ZonedDateTime.now().plusDays(30)),
            )
            issuedCouponJpaRepository.save(IssuedCoupon(userId = 1L, couponTemplateId = template.id))
            issuedCouponJpaRepository.save(IssuedCoupon(userId = 1L, couponTemplateId = template.id))
            issuedCouponJpaRepository.save(IssuedCoupon(userId = 2L, couponTemplateId = template.id))

            // act
            val result = couponService.getUserIssuedCoupons(1L)

            // assert
            assertThat(result).hasSize(2)
        }
    }
}

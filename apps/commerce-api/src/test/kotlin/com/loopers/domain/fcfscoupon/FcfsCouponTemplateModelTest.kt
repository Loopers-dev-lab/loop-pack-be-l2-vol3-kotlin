package com.loopers.domain.fcfscoupon

import com.loopers.domain.coupon.CouponType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

@DisplayName("FcfsCouponTemplateModel 도메인 테스트")
class FcfsCouponTemplateModelTest {

    private fun createTemplate(
        totalQuantity: Int = 100,
        issuedQuantity: Int = 0,
        status: FcfsCouponTemplateStatus = FcfsCouponTemplateStatus.ACTIVE,
        startedAt: ZonedDateTime = ZonedDateTime.now().minusDays(1),
        endedAt: ZonedDateTime = ZonedDateTime.now().plusDays(1),
    ) = FcfsCouponTemplateModel(
        id = 1L,
        name = "테스트쿠폰",
        description = "설명",
        discountType = CouponType.FIXED,
        discountValue = 1000,
        minOrderAmount = null,
        maxDiscountAmount = null,
        totalQuantity = totalQuantity,
        issuedQuantity = issuedQuantity,
        status = status,
        startedAt = startedAt,
        endedAt = endedAt,
    )

    @Nested
    @DisplayName("isActive")
    inner class IsActive {

        @Test
        @DisplayName("ACTIVE 상태면 true를 반환한다")
        fun `ACTIVE이면 true`() {
            assertThat(createTemplate().isActive()).isTrue()
        }

        @Test
        @DisplayName("DELETED 상태면 false를 반환한다")
        fun `DELETED이면 false`() {
            assertThat(createTemplate(status = FcfsCouponTemplateStatus.DELETED).isActive()).isFalse()
        }
    }

    @Nested
    @DisplayName("isWithinPeriod")
    inner class IsWithinPeriod {

        @Test
        @DisplayName("현재 시간이 발급 기간 내이면 true를 반환한다")
        fun `기간 내이면 true`() {
            assertThat(createTemplate().isWithinPeriod()).isTrue()
        }

        @Test
        @DisplayName("발급 기간이 지나면 false를 반환한다")
        fun `기간이 지나면 false`() {
            val template = createTemplate(
                startedAt = ZonedDateTime.now().minusDays(5),
                endedAt = ZonedDateTime.now().minusDays(1),
            )
            assertThat(template.isWithinPeriod()).isFalse()
        }

        @Test
        @DisplayName("발급 기간 전이면 false를 반환한다")
        fun `기간 전이면 false`() {
            val template = createTemplate(
                startedAt = ZonedDateTime.now().plusDays(1),
                endedAt = ZonedDateTime.now().plusDays(5),
            )
            assertThat(template.isWithinPeriod()).isFalse()
        }
    }

    @Nested
    @DisplayName("hasStock")
    inner class HasStock {

        @Test
        @DisplayName("발급 수량이 총 수량 미만이면 true를 반환한다")
        fun `재고 있으면 true`() {
            assertThat(createTemplate(totalQuantity = 100, issuedQuantity = 50).hasStock()).isTrue()
        }

        @Test
        @DisplayName("발급 수량이 총 수량과 같으면 false를 반환한다")
        fun `재고 없으면 false`() {
            assertThat(createTemplate(totalQuantity = 100, issuedQuantity = 100).hasStock()).isFalse()
        }
    }

    @Nested
    @DisplayName("delete")
    inner class Delete {

        @Test
        @DisplayName("삭제하면 DELETED 상태와 deletedAt이 설정된다")
        fun `삭제 시 상태 변경`() {
            // act
            val deleted = createTemplate().delete()

            // assert
            assertThat(deleted.status).isEqualTo(FcfsCouponTemplateStatus.DELETED)
            assertThat(deleted.deletedAt).isNotNull()
        }
    }

    @Nested
    @DisplayName("update")
    inner class Update {

        @Test
        @DisplayName("수정하면 변경된 값이 반영된다")
        fun `수정 시 값 변경`() {
            // act
            val updated = createTemplate().update(
                name = "수정됨",
                description = "새설명",
                discountType = CouponType.RATE,
                discountValue = 10,
                minOrderAmount = 5000,
                maxDiscountAmount = 2000,
                totalQuantity = 50,
                startedAt = ZonedDateTime.now(),
                endedAt = ZonedDateTime.now().plusDays(7),
            )

            // assert
            assertThat(updated.name).isEqualTo("수정됨")
            assertThat(updated.discountType).isEqualTo(CouponType.RATE)
            assertThat(updated.totalQuantity).isEqualTo(50)
        }
    }
}

package com.loopers.application.api.coupon

import com.loopers.domain.coupon.CouponIssuanceResultRepository
import com.loopers.domain.coupon.CouponService
import com.loopers.domain.coupon.CouponTemplate
import com.loopers.domain.coupon.CouponType
import com.loopers.domain.outbox.OutboxPublisher
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.ZonedDateTime

@DisplayName("CouponFacade - 발급 전략")
class CouponFacadeIssuanceStrategyTest {

    private val couponService = mockk<CouponService>()
    private val outboxPublisher = mockk<OutboxPublisher>()
    private val couponIssuanceResultRepository = mockk<CouponIssuanceResultRepository>()
    private val strategySelector = CouponIssuanceStrategySelector()

    private val facade = CouponFacade(
        couponService,
        outboxPublisher,
        couponIssuanceResultRepository,
        strategySelector,
    )

    @DisplayName("선착순 쿠폰 발급 요청 → coupon-limited-events 토픽으로 발행")
    @Test
    fun requestIssuance_limitedCoupon_publishesToLimitedEventsTopic() {
        // Arrange
        val userId = 1L
        val templateId = 100L
        val template = CouponTemplate.createForTest(
            name = "선착순 100장",
            type = CouponType.FIXED,
            value = BigDecimal("1000"),
            minOrderAmount = BigDecimal("5000"),
            expiredAt = ZonedDateTime.now().plusDays(30),
            totalCount = 100,
        )

        every { couponService.validateIssuanceRequest(userId, templateId) } returns Unit
        every { couponService.getTemplateInfo(templateId) } returns template
        every { outboxPublisher.publish(any(), any(), any(), any()) } returns Unit
        every { couponIssuanceResultRepository.save(any()) } returns mockk()

        val topicSlot = slot<String>()
        val partitionKeySlot = slot<String>()

        every {
            outboxPublisher.publish(
                any(),
                any(),
                capture(topicSlot),
                capture(partitionKeySlot),
            )
        } returns Unit

        // Act
        facade.requestIssuance(userId, templateId)

        // Assert
        assertThat(topicSlot.captured).isEqualTo("coupon-limited-events")
        assertThat(partitionKeySlot.captured).isEqualTo("limited:100")
    }

    @DisplayName("일반 쿠폰 발급 요청 → coupon-normal-events 토픽으로 발행")
    @Test
    fun requestIssuance_normalCoupon_publishesToNormalEventsTopic() {
        // Arrange
        val userId = 2L
        val templateId = 200L
        val template = CouponTemplate.createForTest(
            name = "일반 쿠폰",
            type = CouponType.FIXED,
            value = BigDecimal("1000"),
            minOrderAmount = BigDecimal("5000"),
            expiredAt = ZonedDateTime.now().plusDays(30),
        )
        // totalCount = null (무제한)

        every { couponService.validateIssuanceRequest(userId, templateId) } returns Unit
        every { couponService.getTemplateInfo(templateId) } returns template
        every { outboxPublisher.publish(any(), any(), any(), any()) } returns Unit
        every { couponIssuanceResultRepository.save(any()) } returns mockk()

        val topicSlot = slot<String>()
        val partitionKeySlot = slot<String>()

        every {
            outboxPublisher.publish(
                any(),
                any(),
                capture(topicSlot),
                capture(partitionKeySlot),
            )
        } returns Unit

        // Act
        facade.requestIssuance(userId, templateId)

        // Assert
        assertThat(topicSlot.captured).isEqualTo("coupon-normal-events")
        assertThat(partitionKeySlot.captured).isEqualTo("normal:200")
    }

    @DisplayName("선착순과 일반 쿠폰은 다른 토픽으로 발행된다")
    @Test
    fun requestIssuance_differentTopicsForDifferentTypes() {
        // Arrange - 선착순 쿠폰
        val limitedTemplate = CouponTemplate.createForTest(
            name = "선착순",
            type = CouponType.FIXED,
            value = BigDecimal("1000"),
            minOrderAmount = BigDecimal("5000"),
            expiredAt = ZonedDateTime.now().plusDays(30),
            totalCount = 50,
        )

        // Arrange - 일반 쿠폰
        val normalTemplate = CouponTemplate.createForTest(
            name = "일반",
            type = CouponType.FIXED,
            value = BigDecimal("2000"),
            minOrderAmount = BigDecimal("10000"),
            expiredAt = ZonedDateTime.now().plusDays(30),
        )

        every { couponService.validateIssuanceRequest(any(), any()) } returns Unit
        every { couponService.getTemplateInfo(100L) } returns limitedTemplate
        every { couponService.getTemplateInfo(200L) } returns normalTemplate
        every { outboxPublisher.publish(any(), any(), any(), any()) } returns Unit
        every { couponIssuanceResultRepository.save(any()) } returns mockk()

        // Act & Assert
        val limitedTopicSlot = slot<String>()
        every {
            outboxPublisher.publish(any(), eq(100L), capture(limitedTopicSlot), any())
        } returns Unit

        facade.requestIssuance(1L, 100L)
        assertThat(limitedTopicSlot.captured).isEqualTo("coupon-limited-events")

        val normalTopicSlot = slot<String>()
        every {
            outboxPublisher.publish(any(), eq(200L), capture(normalTopicSlot), any())
        } returns Unit

        facade.requestIssuance(2L, 200L)
        assertThat(normalTopicSlot.captured).isEqualTo("coupon-normal-events")
    }
}

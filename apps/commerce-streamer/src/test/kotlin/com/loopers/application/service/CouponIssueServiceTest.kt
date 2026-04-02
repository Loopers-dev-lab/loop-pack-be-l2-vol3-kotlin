package com.loopers.application.service

import com.loopers.domain.coupon.CouponDto
import com.loopers.domain.coupon.CouponIssuanceResultDto
import com.loopers.domain.coupon.CouponIssuanceResultRepository
import com.loopers.domain.coupon.CouponRepository
import com.loopers.domain.coupon.CouponStatus
import com.loopers.domain.coupon.CouponTemplateDto
import com.loopers.domain.coupon.CouponTemplateRepository
import com.loopers.domain.coupon.CouponType
import com.loopers.domain.coupon.IssuanceStatus
import com.loopers.domain.event.CouponIssueRequestedEvent
import com.loopers.domain.eventhandled.EventHandledRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.ZonedDateTime

@DisplayName("CouponIssueService")
class CouponIssueServiceTest {

    private val couponRepository = mockk<CouponRepository>()
    private val couponTemplateRepository = mockk<CouponTemplateRepository>()
    private val eventHandledRepository = mockk<EventHandledRepository>()
    private val couponIssuanceResultRepository = mockk<CouponIssuanceResultRepository>()

    private val service = CouponIssueService(
        couponRepository,
        couponTemplateRepository,
        eventHandledRepository,
        couponIssuanceResultRepository,
    )

    @DisplayName("정상 쿠폰 발급 - 선착순 아직 남음")
    @Test
    fun processIssuanceRequest_normalIssuance_issuesCoupon() {
        // Arrange
        val event = CouponIssueRequestedEvent(userId = 1L, templateId = 100L)
        val template = createTestTemplate(id = 100L, totalCount = 100, issuedCount = 50)
        val issuedCoupon = CouponDto(
            id = 1L,
            userId = 1L,
            templateId = 100L,
            status = CouponStatus.ISSUED,
            requestedAt = event.requestedAt,
        )

        every { eventHandledRepository.existsByDedupeKey(event.dedupeKey) } returns false
        every { couponTemplateRepository.findById(100L) } returns template
        every { couponRepository.findByUserIdAndTemplateId(1L, 100L) } returns null
        every { couponTemplateRepository.incrementIssuedCountIfAvailable(100L) } returns 1
        every { couponRepository.save(any()) } returns issuedCoupon
        every { couponIssuanceResultRepository.findByDedupeKey(event.dedupeKey) } returns createTestIssuanceResult(
            dedupeKey = event.dedupeKey,
        )
        every { couponIssuanceResultRepository.save(any()) } returns mockk()
        every { eventHandledRepository.save(any()) } returns mockk()

        // Act
        service.processIssuanceRequest(event)

        // Assert
        verify(exactly = 1) { couponRepository.save(any()) }
        verify(exactly = 1) {
            couponIssuanceResultRepository.save(match { it.status == IssuanceStatus.ISSUED })
        }
    }

    @DisplayName("선착순 품절 - SOLD_OUT")
    @Test
    fun processIssuanceRequest_soldOut_returnsFailure() {
        // Arrange
        val event = CouponIssueRequestedEvent(userId = 2L, templateId = 101L)
        val template = createTestTemplate(id = 101L, totalCount = 100, issuedCount = 100)

        every { eventHandledRepository.existsByDedupeKey(event.dedupeKey) } returns false
        every { couponTemplateRepository.findById(101L) } returns template
        every { couponRepository.findByUserIdAndTemplateId(2L, 101L) } returns null
        every { couponTemplateRepository.incrementIssuedCountIfAvailable(101L) } returns 0 // sold out
        every { couponIssuanceResultRepository.findByDedupeKey(event.dedupeKey) } returns createTestIssuanceResult(
            dedupeKey = event.dedupeKey,
        )
        every { couponIssuanceResultRepository.save(any()) } returns mockk()
        every { eventHandledRepository.save(any()) } returns mockk()

        // Act
        service.processIssuanceRequest(event)

        // Assert
        verify(exactly = 0) { couponRepository.save(any()) } // 쿠폰 발급 안 됨
        verify(exactly = 1) {
            couponIssuanceResultRepository.save(match { it.status == IssuanceStatus.SOLD_OUT })
        }
    }

    @DisplayName("중복 발급 거절 - DUPLICATE")
    @Test
    fun processIssuanceRequest_duplicateIssuance_rejectsDuplicate() {
        // Arrange
        val event = CouponIssueRequestedEvent(userId = 3L, templateId = 102L)
        val template = createTestTemplate(id = 102L, totalCount = 100, issuedCount = 50)
        val existingCoupon = CouponDto(
            id = 999L,
            userId = 3L,
            templateId = 102L,
            status = CouponStatus.ISSUED,
            requestedAt = ZonedDateTime.now(),
        )

        every { eventHandledRepository.existsByDedupeKey(event.dedupeKey) } returns false
        every { couponTemplateRepository.findById(102L) } returns template
        every { couponRepository.findByUserIdAndTemplateId(3L, 102L) } returns existingCoupon
        every { couponIssuanceResultRepository.findByDedupeKey(event.dedupeKey) } returns createTestIssuanceResult(
            dedupeKey = event.dedupeKey,
        )
        every { couponIssuanceResultRepository.save(any()) } returns mockk()
        every { eventHandledRepository.save(any()) } returns mockk()

        // Act
        service.processIssuanceRequest(event)

        // Assert
        verify(exactly = 0) { couponTemplateRepository.incrementIssuedCountIfAvailable(any()) } // increment 호출 안 됨
        verify(exactly = 0) { couponRepository.save(any()) } // 쿠폰 발급 안 됨
        verify(exactly = 1) {
            couponIssuanceResultRepository.save(match { it.status == IssuanceStatus.DUPLICATE })
        }
    }

    @DisplayName("템플릿 없음 - TEMPLATE_NOT_FOUND")
    @Test
    fun processIssuanceRequest_templateNotFound_returnsNotFound() {
        // Arrange
        val event = CouponIssueRequestedEvent(userId = 4L, templateId = 999L)

        every { eventHandledRepository.existsByDedupeKey(event.dedupeKey) } returns false
        every { couponTemplateRepository.findById(999L) } returns null
        every { couponIssuanceResultRepository.findByDedupeKey(event.dedupeKey) } returns createTestIssuanceResult(
            dedupeKey = event.dedupeKey,
        )
        every { couponIssuanceResultRepository.save(any()) } returns mockk()
        every { eventHandledRepository.save(any()) } returns mockk()

        // Act
        service.processIssuanceRequest(event)

        // Assert
        verify(exactly = 1) {
            couponIssuanceResultRepository.save(match { it.status == IssuanceStatus.TEMPLATE_NOT_FOUND })
        }
    }

    @DisplayName("템플릿 만료 - TEMPLATE_EXPIRED")
    @Test
    fun processIssuanceRequest_expiredTemplate_returnsExpired() {
        // Arrange
        val event = CouponIssueRequestedEvent(userId = 5L, templateId = 103L)
        // 어제 만료
        val expiredTemplate = createTestTemplate(
            id = 103L,
            totalCount = 100,
            issuedCount = 50,
            expiredAt = ZonedDateTime.now().minusDays(1),
        )

        every { eventHandledRepository.existsByDedupeKey(event.dedupeKey) } returns false
        every { couponTemplateRepository.findById(103L) } returns expiredTemplate
        every { couponIssuanceResultRepository.findByDedupeKey(event.dedupeKey) } returns createTestIssuanceResult(
            dedupeKey = event.dedupeKey,
        )
        every { couponIssuanceResultRepository.save(any()) } returns mockk()
        every { eventHandledRepository.save(any()) } returns mockk()

        // Act
        service.processIssuanceRequest(event)

        // Assert
        verify(exactly = 1) {
            couponIssuanceResultRepository.save(match { it.status == IssuanceStatus.TEMPLATE_EXPIRED })
        }
    }

    @DisplayName("멱등 처리 - 이미 처리된 이벤트는 skip")
    @Test
    fun processIssuanceRequest_alreadyProcessed_skipsProcessing() {
        // Arrange
        val event = CouponIssueRequestedEvent(userId = 6L, templateId = 104L)

        every { eventHandledRepository.existsByDedupeKey(event.dedupeKey) } returns true

        // Act
        service.processIssuanceRequest(event)

        // Assert
        verify(exactly = 0) { couponTemplateRepository.findById(any()) } // 이후 로직 수행 안 됨
        verify(exactly = 0) { couponRepository.save(any()) }
        verify(exactly = 0) { couponIssuanceResultRepository.save(any()) }
    }

    @DisplayName("무제한 쿠폰 발급 - totalCount가 null")
    @Test
    fun processIssuanceRequest_unlimitedCoupon_issuesCoupon() {
        // Arrange
        val event = CouponIssueRequestedEvent(userId = 7L, templateId = 105L)
        val unlimitedTemplate = createTestTemplate(id = 105L, totalCount = null, issuedCount = 1000)
        val issuedCoupon = CouponDto(
            id = 2L,
            userId = 7L,
            templateId = 105L,
            status = CouponStatus.ISSUED,
            requestedAt = event.requestedAt,
        )

        every { eventHandledRepository.existsByDedupeKey(event.dedupeKey) } returns false
        every { couponTemplateRepository.findById(105L) } returns unlimitedTemplate
        every { couponRepository.findByUserIdAndTemplateId(7L, 105L) } returns null
        every { couponTemplateRepository.incrementIssuedCountIfAvailable(105L) } returns 1
        every { couponRepository.save(any()) } returns issuedCoupon
        every { couponIssuanceResultRepository.findByDedupeKey(event.dedupeKey) } returns createTestIssuanceResult(
            dedupeKey = event.dedupeKey,
        )
        every { couponIssuanceResultRepository.save(any()) } returns mockk()
        every { eventHandledRepository.save(any()) } returns mockk()

        // Act
        service.processIssuanceRequest(event)

        // Assert
        verify(exactly = 1) { couponRepository.save(any()) }
        verify(exactly = 1) {
            couponIssuanceResultRepository.save(match { it.status == IssuanceStatus.ISSUED })
        }
    }

    @DisplayName("결과 저장 - IssuanceStatus와 couponId를 업데이트")
    @Test
    fun processIssuanceRequest_savesResultWithCouponId() {
        // Arrange
        val event = CouponIssueRequestedEvent(userId = 8L, templateId = 106L)
        val template = createTestTemplate(id = 106L, totalCount = 100, issuedCount = 50)
        val issuedCoupon = CouponDto(
            id = 123L,
            userId = 8L,
            templateId = 106L,
            status = CouponStatus.ISSUED,
            requestedAt = event.requestedAt,
        )

        every { eventHandledRepository.existsByDedupeKey(event.dedupeKey) } returns false
        every { couponTemplateRepository.findById(106L) } returns template
        every { couponRepository.findByUserIdAndTemplateId(8L, 106L) } returns null
        every { couponTemplateRepository.incrementIssuedCountIfAvailable(106L) } returns 1
        every { couponRepository.save(any()) } returns issuedCoupon
        every { couponIssuanceResultRepository.findByDedupeKey(event.dedupeKey) } returns createTestIssuanceResult(
            dedupeKey = event.dedupeKey,
        )
        every { couponIssuanceResultRepository.save(any()) } returns mockk()
        every { eventHandledRepository.save(any()) } returns mockk()

        // Act
        service.processIssuanceRequest(event)

        // Assert
        verify(exactly = 1) {
            couponIssuanceResultRepository.save(
                match {
                it.status == IssuanceStatus.ISSUED && it.couponId == 123L
            },
            )
        }
    }

    @DisplayName("처리 완료 기록 - event_handled에 저장")
    @Test
    fun processIssuanceRequest_recordsEventHandled() {
        // Arrange
        val event = CouponIssueRequestedEvent(userId = 9L, templateId = 107L)
        val template = createTestTemplate(id = 107L, totalCount = 100, issuedCount = 50)
        val issuedCoupon = CouponDto(
            id = 3L,
            userId = 9L,
            templateId = 107L,
            status = CouponStatus.ISSUED,
            requestedAt = event.requestedAt,
        )

        every { eventHandledRepository.existsByDedupeKey(event.dedupeKey) } returns false
        every { couponTemplateRepository.findById(107L) } returns template
        every { couponRepository.findByUserIdAndTemplateId(9L, 107L) } returns null
        every { couponTemplateRepository.incrementIssuedCountIfAvailable(107L) } returns 1
        every { couponRepository.save(any()) } returns issuedCoupon
        every { couponIssuanceResultRepository.findByDedupeKey(event.dedupeKey) } returns createTestIssuanceResult(
            dedupeKey = event.dedupeKey,
        )
        every { couponIssuanceResultRepository.save(any()) } returns mockk()
        every { eventHandledRepository.save(any()) } returns mockk()

        // Act
        service.processIssuanceRequest(event)

        // Assert
        verify(exactly = 1) {
            eventHandledRepository.save(match { it.dedupeKey == event.dedupeKey })
        }
    }

    // Helper methods
    private fun createTestTemplate(
        id: Long,
        totalCount: Int?,
        issuedCount: Int,
        expiredAt: ZonedDateTime = ZonedDateTime.now().plusDays(30),
    ): CouponTemplateDto {
        return CouponTemplateDto(
            id = id,
            name = "Test Coupon",
            type = CouponType.FIXED,
            value = BigDecimal("1000"),
            minOrderAmount = BigDecimal("5000"),
            expiredAt = expiredAt,
            totalCount = totalCount,
            issuedCount = issuedCount,
        )
    }

    private fun createTestIssuanceResult(
        dedupeKey: String,
    ): CouponIssuanceResultDto {
        return CouponIssuanceResultDto(
            dedupeKey = dedupeKey,
            userId = 1L,
            templateId = 100L,
            status = IssuanceStatus.PENDING,
        )
    }
}

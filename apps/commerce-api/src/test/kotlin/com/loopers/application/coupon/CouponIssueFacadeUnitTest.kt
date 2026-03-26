package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponIssueRequest
import com.loopers.domain.coupon.CouponIssueRequestRepository
import com.loopers.domain.coupon.CouponIssueStatus
import com.loopers.domain.coupon.CouponTemplate
import com.loopers.domain.coupon.CouponTemplateRepository
import com.loopers.domain.coupon.CouponType
import com.loopers.domain.coupon.UserCouponRepository
import com.loopers.infrastructure.outbox.OutboxEventService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class CouponIssueFacadeUnitTest {

    private val mockIssueRequestRepo = mockk<CouponIssueRequestRepository>(relaxed = true)
    private val mockTemplateRepo = mockk<CouponTemplateRepository>(relaxed = true)
    private val mockUserCouponRepo = mockk<UserCouponRepository>(relaxed = true)
    private val mockOutboxEventService = mockk<OutboxEventService>(relaxed = true)

    private val facade = CouponIssueFacade(
        mockIssueRequestRepo, mockTemplateRepo, mockUserCouponRepo, mockOutboxEventService,
    )

    // ─── requestIssue ───

    @Test
    fun `requestIssue() should save request and outbox`() {
        val template = createTemplate(id = 1L)
        every { mockIssueRequestRepo.findByUserIdAndCouponTemplateId(1L, 1L) } returns null
        every { mockTemplateRepo.findById(1L) } returns template
        every { mockIssueRequestRepo.save(any()) } answers {
            val req = firstArg<CouponIssueRequest>()
            CouponIssueRequest(
                id = 100L, userId = req.userId, couponTemplateId = req.couponTemplateId,
            )
        }

        val result = facade.requestIssue(userId = 1L, couponTemplateId = 1L)

        assertThat(result.status).isEqualTo(CouponIssueStatus.REQUESTED)
        verify { mockOutboxEventService.save(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `requestIssue() should return existing request if already requested (idempotency)`() {
        val existing = CouponIssueRequest(
            id = 100L, userId = 1L, couponTemplateId = 1L, status = CouponIssueStatus.ISSUED,
        )
        every { mockIssueRequestRepo.findByUserIdAndCouponTemplateId(1L, 1L) } returns existing

        val result = facade.requestIssue(userId = 1L, couponTemplateId = 1L)

        assertThat(result.status).isEqualTo(CouponIssueStatus.ISSUED)
        verify(exactly = 0) { mockOutboxEventService.save(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `requestIssue() should throw NOT_FOUND when template does not exist`() {
        every { mockIssueRequestRepo.findByUserIdAndCouponTemplateId(1L, 99L) } returns null
        every { mockTemplateRepo.findById(99L) } returns null

        assertThrows<CoreException> {
            facade.requestIssue(userId = 1L, couponTemplateId = 99L)
        }.also {
            assertThat(it.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    // ─── processIssue ───

    @Test
    fun `processIssue() should issue coupon and mark request as ISSUED`() {
        val request = CouponIssueRequest(id = 100L, userId = 1L, couponTemplateId = 1L)
        val template = createTemplate(id = 1L, maxIssuance = 100, issuedCount = 0)

        every { mockIssueRequestRepo.findById(100L) } returns request
        every { mockTemplateRepo.findByIdWithLock(1L) } returns template
        every { mockUserCouponRepo.existsByUserIdAndCouponTemplateId(1L, 1L) } returns false

        facade.processIssue(requestId = 100L, userId = 1L, couponTemplateId = 1L)

        assertThat(request.status).isEqualTo(CouponIssueStatus.ISSUED)
        assertThat(template.issuedCount).isEqualTo(1)
        verify { mockUserCouponRepo.save(any()) }
        verify { mockTemplateRepo.save(template) }
    }

    @Test
    fun `processIssue() should mark FAILED when quantity exceeded`() {
        val request = CouponIssueRequest(id = 100L, userId = 1L, couponTemplateId = 1L)
        val template = createTemplate(id = 1L, maxIssuance = 10, issuedCount = 10) // full

        every { mockIssueRequestRepo.findById(100L) } returns request
        every { mockTemplateRepo.findByIdWithLock(1L) } returns template

        facade.processIssue(requestId = 100L, userId = 1L, couponTemplateId = 1L)

        assertThat(request.status).isEqualTo(CouponIssueStatus.FAILED)
        assertThat(request.failReason).contains("수량")
        verify(exactly = 0) { mockUserCouponRepo.save(any()) }
    }

    @Test
    fun `processIssue() should mark FAILED when user already has coupon`() {
        val request = CouponIssueRequest(id = 100L, userId = 1L, couponTemplateId = 1L)
        val template = createTemplate(id = 1L, maxIssuance = 100, issuedCount = 0)

        every { mockIssueRequestRepo.findById(100L) } returns request
        every { mockTemplateRepo.findByIdWithLock(1L) } returns template
        every { mockUserCouponRepo.existsByUserIdAndCouponTemplateId(1L, 1L) } returns true

        facade.processIssue(requestId = 100L, userId = 1L, couponTemplateId = 1L)

        assertThat(request.status).isEqualTo(CouponIssueStatus.FAILED)
        assertThat(request.failReason).contains("이미")
        verify(exactly = 0) { mockUserCouponRepo.save(any()) }
    }

    @Test
    fun `processIssue() should skip if request already processed (idempotency)`() {
        val request = CouponIssueRequest(
            id = 100L, userId = 1L, couponTemplateId = 1L, status = CouponIssueStatus.ISSUED,
        )

        every { mockIssueRequestRepo.findById(100L) } returns request

        facade.processIssue(requestId = 100L, userId = 1L, couponTemplateId = 1L)

        verify(exactly = 0) { mockTemplateRepo.findByIdWithLock(any()) }
        verify(exactly = 0) { mockUserCouponRepo.save(any()) }
    }

    // ─── Helpers ───

    private fun createTemplate(
        id: Long = 1L,
        maxIssuance: Int? = 100,
        issuedCount: Int = 0,
    ): CouponTemplate = CouponTemplate(
        id = id,
        name = "Test Coupon",
        type = CouponType.FIXED,
        discountValue = 1000,
        minOrderAmount = 0,
        maxIssuance = maxIssuance,
        issuedCount = issuedCount,
        expiresAt = LocalDate.now().plusDays(30),
    )
}

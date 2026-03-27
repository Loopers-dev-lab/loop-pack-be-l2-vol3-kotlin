package com.loopers.application.fcfscoupon

import com.loopers.application.outbox.FakeOutboxEventRepository
import com.loopers.domain.coupon.CouponType
import com.loopers.domain.error.CoreException
import com.loopers.infrastructure.outbox.OutboxEventPublisherImpl
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

@DisplayName("FcfsCouponFacade 테스트")
class FcfsCouponFacadeTest {

    private lateinit var templateRepository: FakeFcfsCouponTemplateRepository
    private lateinit var requestRepository: FakeFcfsCouponIssueRequestRepository
    private lateinit var outboxRepository: FakeOutboxEventRepository
    private lateinit var service: FcfsCouponService
    private lateinit var facade: FcfsCouponFacade

    @BeforeEach
    fun setUp() {
        templateRepository = FakeFcfsCouponTemplateRepository()
        requestRepository = FakeFcfsCouponIssueRequestRepository()
        outboxRepository = FakeOutboxEventRepository()
        service = FcfsCouponService(templateRepository, requestRepository)
        facade = FcfsCouponFacade(service, OutboxEventPublisherImpl(outboxRepository, ObjectMapper()))
    }

    private fun createActiveTemplate(totalQuantity: Int = 100): FcfsCouponTemplateInfo {
        return AdminFcfsCouponFacade(service).createTemplate(
            FcfsCouponCommand.CreateTemplate(
                name = "선착순쿠폰",
                description = "테스트",
                discountType = CouponType.FIXED,
                discountValue = 1000,
                minOrderAmount = null,
                maxDiscountAmount = null,
                totalQuantity = totalQuantity,
                startedAt = ZonedDateTime.now().minusDays(1),
                endedAt = ZonedDateTime.now().plusDays(1),
            ),
        )
    }

    @Nested
    @DisplayName("requestIssue")
    inner class RequestIssue {

        @Test
        @DisplayName("정상 발급 요청 시 PENDING 상태와 outbox 이벤트가 생성된다")
        fun `정상 발급 요청`() {
            // arrange
            val template = createActiveTemplate()

            // act
            val result = facade.requestIssue(memberId = 1L, templateId = template.id)

            // assert
            assertThat(result.status).isEqualTo("PENDING")
            assertThat(result.templateId).isEqualTo(template.id)

            val outboxEvents = outboxRepository.findAll()
            assertThat(outboxEvents).hasSize(1)
            assertThat(outboxEvents[0].topic).isEqualTo("coupon.issue.request")
            assertThat(outboxEvents[0].partitionKey).isEqualTo(template.id.toString())
        }

        @Test
        @DisplayName("발급 기간이 아닌 경우 예외가 발생한다")
        fun `발급 기간이 아닌 경우`() {
            // arrange
            val template = AdminFcfsCouponFacade(service).createTemplate(
                FcfsCouponCommand.CreateTemplate(
                    name = "만료쿠폰",
                    description = null,
                    discountType = CouponType.FIXED,
                    discountValue = 1000,
                    minOrderAmount = null,
                    maxDiscountAmount = null,
                    totalQuantity = 100,
                    startedAt = ZonedDateTime.now().minusDays(5),
                    endedAt = ZonedDateTime.now().minusDays(1),
                ),
            )

            // act & assert
            assertThatThrownBy { facade.requestIssue(memberId = 1L, templateId = template.id) }
                .isInstanceOf(CoreException::class.java)
        }
    }

    @Nested
    @DisplayName("getIssueRequestStatus")
    inner class GetIssueRequestStatus {

        @Test
        @DisplayName("본인의 발급 요청 상태를 조회할 수 있다")
        fun `본인 요청 조회`() {
            // arrange
            val template = createActiveTemplate()
            val request = facade.requestIssue(memberId = 1L, templateId = template.id)

            // act
            val result = facade.getIssueRequestStatus(request.id, memberId = 1L)

            // assert
            assertThat(result.status).isEqualTo("PENDING")
        }

        @Test
        @DisplayName("다른 회원의 발급 요청 조회 시 예외가 발생한다")
        fun `다른 회원 요청 조회 시 예외`() {
            // arrange
            val template = createActiveTemplate()
            val request = facade.requestIssue(memberId = 1L, templateId = template.id)

            // act & assert
            assertThatThrownBy { facade.getIssueRequestStatus(request.id, memberId = 999L) }
                .isInstanceOf(CoreException::class.java)
        }
    }
}

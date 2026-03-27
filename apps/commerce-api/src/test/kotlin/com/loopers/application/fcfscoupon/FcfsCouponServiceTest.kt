package com.loopers.application.fcfscoupon

import com.loopers.domain.coupon.CouponType
import com.loopers.domain.error.CoreException
import com.loopers.domain.fcfscoupon.FcfsCouponIssueStatus
import com.loopers.domain.fcfscoupon.FcfsCouponTemplateStatus
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

@DisplayName("FcfsCouponService 테스트")
class FcfsCouponServiceTest {

    private lateinit var templateRepository: FakeFcfsCouponTemplateRepository
    private lateinit var requestRepository: FakeFcfsCouponIssueRequestRepository
    private lateinit var service: FcfsCouponService

    @BeforeEach
    fun setUp() {
        templateRepository = FakeFcfsCouponTemplateRepository()
        requestRepository = FakeFcfsCouponIssueRequestRepository()
        service = FcfsCouponService(templateRepository, requestRepository)
    }

    private fun createTemplateCommand(
        totalQuantity: Int = 100,
        startedAt: ZonedDateTime = ZonedDateTime.now().minusDays(1),
        endedAt: ZonedDateTime = ZonedDateTime.now().plusDays(1),
    ) = FcfsCouponCommand.CreateTemplate(
        name = "선착순쿠폰",
        description = "테스트 쿠폰",
        discountType = CouponType.FIXED,
        discountValue = 1000,
        minOrderAmount = 10000,
        maxDiscountAmount = null,
        totalQuantity = totalQuantity,
        startedAt = startedAt,
        endedAt = endedAt,
    )

    @Nested
    @DisplayName("createTemplate")
    inner class CreateTemplate {

        @Test
        @DisplayName("유효한 정보로 템플릿을 생성한다")
        fun `템플릿을 생성한다`() {
            // act
            val template = service.createTemplate(createTemplateCommand())

            // assert
            assertThat(template.id).isGreaterThan(0)
            assertThat(template.name).isEqualTo("선착순쿠폰")
            assertThat(template.discountType).isEqualTo(CouponType.FIXED)
            assertThat(template.discountValue).isEqualTo(1000)
            assertThat(template.totalQuantity).isEqualTo(100)
            assertThat(template.issuedQuantity).isEqualTo(0)
            assertThat(template.status).isEqualTo(FcfsCouponTemplateStatus.ACTIVE)
        }
    }

    @Nested
    @DisplayName("getTemplate")
    inner class GetTemplate {

        @Test
        @DisplayName("존재하는 ID로 조회하면 템플릿을 반환한다")
        fun `존재하는 ID로 조회`() {
            // arrange
            val created = service.createTemplate(createTemplateCommand())

            // act
            val found = service.getTemplate(created.id)

            // assert
            assertThat(found.id).isEqualTo(created.id)
            assertThat(found.name).isEqualTo("선착순쿠폰")
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회하면 예외가 발생한다")
        fun `존재하지 않는 ID로 조회 시 예외`() {
            // act & assert
            assertThatThrownBy { service.getTemplate(999L) }
                .isInstanceOf(CoreException::class.java)
        }
    }

    @Nested
    @DisplayName("updateTemplate")
    inner class UpdateTemplate {

        @Test
        @DisplayName("템플릿 정보를 수정한다")
        fun `템플릿을 수정한다`() {
            // arrange
            val created = service.createTemplate(createTemplateCommand())

            // act
            val updated = service.updateTemplate(
                created.id,
                FcfsCouponCommand.UpdateTemplate(
                    name = "수정된쿠폰",
                    description = "수정됨",
                    discountType = CouponType.RATE,
                    discountValue = 10,
                    minOrderAmount = 5000,
                    maxDiscountAmount = 2000,
                    totalQuantity = 50,
                    startedAt = created.startedAt,
                    endedAt = created.endedAt,
                ),
            )

            // assert
            assertThat(updated.name).isEqualTo("수정된쿠폰")
            assertThat(updated.discountType).isEqualTo(CouponType.RATE)
            assertThat(updated.discountValue).isEqualTo(10)
            assertThat(updated.totalQuantity).isEqualTo(50)
        }
    }

    @Nested
    @DisplayName("deleteTemplate")
    inner class DeleteTemplate {

        @Test
        @DisplayName("템플릿을 삭제하면 DELETED 상태가 된다")
        fun `템플릿을 삭제한다`() {
            // arrange
            val created = service.createTemplate(createTemplateCommand())

            // act
            service.deleteTemplate(created.id)

            // assert
            val deleted = service.getTemplate(created.id)
            assertThat(deleted.status).isEqualTo(FcfsCouponTemplateStatus.DELETED)
        }
    }

    @Nested
    @DisplayName("createIssueRequest")
    inner class CreateIssueRequest {

        @Test
        @DisplayName("발급 요청을 PENDING 상태로 생성한다")
        fun `발급 요청을 생성한다`() {
            // act
            val request = service.createIssueRequest(templateId = 1L, memberId = 100L)

            // assert
            assertThat(request.id).isGreaterThan(0)
            assertThat(request.templateId).isEqualTo(1L)
            assertThat(request.memberId).isEqualTo(100L)
            assertThat(request.status).isEqualTo(FcfsCouponIssueStatus.PENDING)
        }
    }

    @Nested
    @DisplayName("getIssueRequest")
    inner class GetIssueRequest {

        @Test
        @DisplayName("존재하는 요청을 조회한다")
        fun `존재하는 요청 조회`() {
            // arrange
            val created = service.createIssueRequest(templateId = 1L, memberId = 100L)

            // act
            val found = service.getIssueRequest(created.id)

            // assert
            assertThat(found.id).isEqualTo(created.id)
            assertThat(found.status).isEqualTo(FcfsCouponIssueStatus.PENDING)
        }

        @Test
        @DisplayName("존재하지 않는 요청 조회 시 예외가 발생한다")
        fun `존재하지 않는 요청 조회 시 예외`() {
            // act & assert
            assertThatThrownBy { service.getIssueRequest(999L) }
                .isInstanceOf(CoreException::class.java)
        }
    }
}

package com.loopers.application.api.coupon

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponService
import com.loopers.domain.coupon.CouponIssuanceResult
import com.loopers.domain.coupon.CouponIssuanceResultRepository
import com.loopers.domain.coupon.IssuanceStatus
import com.loopers.domain.coupon.dto.CouponInfo
import com.loopers.domain.coupon.dto.CouponIssueRequestInfo
import com.loopers.domain.coupon.dto.CouponIssuanceStatusInfo
import com.loopers.domain.coupon.event.CouponIssueRequestedEvent
import com.loopers.domain.outbox.OutboxPublisher
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.PageImpl
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CouponFacade(
    private val couponService: CouponService,
    private val outboxPublisher: OutboxPublisher,
    private val couponIssuanceResultRepository: CouponIssuanceResultRepository,
    private val strategySelector: CouponIssuanceStrategySelector,
) {

    @Transactional
    fun requestIssuance(userId: Long, templateId: Long): CouponIssueRequestInfo {
        couponService.validateIssuanceRequest(userId, templateId)
        val template = couponService.getTemplateInfo(templateId)
        val strategy = strategySelector.select(template)

        val event = CouponIssueRequestedEvent(userId = userId, templateId = templateId)

        // 쿠폰 발급 전략에 따라 토픽 및 파티션 키 결정
        // - 선착순 쿠폰 (totalCount ≠ null) → limited-events 토픽, "limited:templateId" 키
        // - 일반 쿠폰 (totalCount = null) → normal-events 토픽, "normal:templateId" 키
        outboxPublisher.publish(
            event,
            templateId,
            topic = strategy.getTopic(),
            partitionKey = strategy.getPartitionKey(templateId),
        )

        // 발급 요청 결과 저장 (PENDING 상태)
        couponIssuanceResultRepository.save(
            CouponIssuanceResult(
                dedupeKey = event.dedupeKey,
                userId = userId,
                templateId = templateId,
                status = IssuanceStatus.PENDING,
            ),
        )

        return CouponIssueRequestInfo(templateId = templateId, dedupeKey = event.dedupeKey)
    }

    @Transactional
    fun issueCoupon(userId: Long, templateId: Long): CouponInfo {
        val coupon = couponService.issueCoupon(userId, templateId)
        val template = couponService.getTemplateInfo(coupon.templateId)
        return CouponInfo.from(
            coupon = coupon,
            templateName = template.name,
            type = template.type,
            value = template.value,
            minOrderAmount = template.minOrderAmount,
            expiredAt = template.expiredAt,
        )
    }

    fun getMyCoupons(userId: Long, pageable: Pageable): Page<CouponInfo> {
        val couponPage = couponService.getMyCoupons(userId, pageable)
        return convertToCouponInfoPage(couponPage)
    }

    fun getIssuanceStatus(dedupeKey: String, userId: Long): CouponIssuanceStatusInfo {
        val result = couponIssuanceResultRepository.findByDedupeKey(dedupeKey)
            ?: throw CoreException(ErrorType.NOT_FOUND, "발급 요청을 찾을 수 없습니다.")

        // 요청자와 결과의 사용자가 일치하는지 확인
        if (result.userId != userId) {
            throw CoreException(ErrorType.FORBIDDEN, "다른 사용자의 발급 요청을 조회할 수 없습니다.")
        }

        return CouponIssuanceStatusInfo.from(result)
    }

    private fun convertToCouponInfoPage(couponPage: Page<Coupon>): Page<CouponInfo> {
        val couponInfos = couponPage.content.map { coupon ->
            val template = couponService.getTemplateInfo(coupon.templateId)
            CouponInfo.from(
                coupon = coupon,
                templateName = template.name,
                type = template.type,
                value = template.value,
                minOrderAmount = template.minOrderAmount,
                expiredAt = template.expiredAt,
            )
        }
        return PageImpl(couponInfos, couponPage.pageable, couponPage.totalElements)
    }
}

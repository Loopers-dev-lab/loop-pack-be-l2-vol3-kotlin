package com.loopers.application.service

import com.loopers.domain.coupon.CouponDto
import com.loopers.domain.coupon.CouponIssuanceResultRepository
import com.loopers.domain.coupon.CouponRepository
import com.loopers.domain.coupon.CouponStatus
import com.loopers.domain.coupon.CouponTemplateRepository
import com.loopers.domain.coupon.IssuanceStatus
import com.loopers.domain.event.CouponIssueRequestedEvent
import com.loopers.domain.eventhandled.EventHandledDto
import com.loopers.domain.eventhandled.EventHandledRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Service
@Transactional
class CouponIssueService(
    private val couponRepository: CouponRepository,
    private val couponTemplateRepository: CouponTemplateRepository,
    private val eventHandledRepository: EventHandledRepository,
    private val couponIssuanceResultRepository: CouponIssuanceResultRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun processIssuanceRequest(event: CouponIssueRequestedEvent) {
        // 1. 멱등성 체크 - 이미 처리된 이벤트면 skip
        if (eventHandledRepository.existsByDedupeKey(event.dedupeKey)) {
            return
        }

        // 2. CouponTemplate 유효성 확인
        val template = couponTemplateRepository.findById(event.templateId)
        if (template == null) {
            logger.warn("CouponTemplate not found: templateId={}", event.templateId)
            updateIssuanceResult(event.dedupeKey, IssuanceStatus.TEMPLATE_NOT_FOUND)
            eventHandledRepository.save(EventHandledDto(dedupeKey = event.dedupeKey))
            return
        }

        // 3. 만료 여부 확인
        if (template.isExpired()) {
            logger.warn("CouponTemplate expired: templateId={}", event.templateId)
            updateIssuanceResult(event.dedupeKey, IssuanceStatus.TEMPLATE_EXPIRED)
            eventHandledRepository.save(EventHandledDto(dedupeKey = event.dedupeKey))
            return
        }

        // 4. 중복 발급 확인
        // ✅ Kafka 파티션 키 = templateId → 같은 템플릿의 모든 요청은 같은 파티션으로
        //    Consumer가 순차 처리 (단일 스레드) → race condition 없음 (DB lock 불필요)
        val existingCoupon = couponRepository.findByUserIdAndTemplateId(event.userId, event.templateId)
        if (existingCoupon != null) {
            logger.info("Duplicate coupon issue: userId={}, templateId={}", event.userId, event.templateId)
            updateIssuanceResult(event.dedupeKey, IssuanceStatus.DUPLICATE)
            eventHandledRepository.save(EventHandledDto(dedupeKey = event.dedupeKey))
            return
        }

        // 5. 원자적 업데이트로 선착순 처리 및 발행 수 증가
        // DB 레벨에서: totalCount가 없거나 issuedCount < totalCount인 경우만 증가
        val updatedCount = couponTemplateRepository.incrementIssuedCountIfAvailable(event.templateId)
        if (updatedCount == 0) {
            logger.info("CouponTemplate sold out: templateId={}", event.templateId)
            updateIssuanceResult(event.dedupeKey, IssuanceStatus.SOLD_OUT)
            eventHandledRepository.save(EventHandledDto(dedupeKey = event.dedupeKey))
            return
        }

        // 6. 쿠폰 발급
        val newCoupon = CouponDto(
            id = 0,
            userId = event.userId,
            templateId = template.id,
            status = CouponStatus.ISSUED,
            requestedAt = event.requestedAt,
        )
        val issuedCoupon = couponRepository.save(newCoupon)
        logger.info(
            "Coupon issued: userId={}, templateId={}, couponId={}, requestedAt={}",
            event.userId,
            event.templateId,
            issuedCoupon.id,
            event.requestedAt,
        )

        // 7. 발급 성공 - 결과 업데이트
        updateIssuanceResult(event.dedupeKey, IssuanceStatus.ISSUED, issuedCoupon.id)

        // 8. 처리 완료 기록
        eventHandledRepository.save(EventHandledDto(dedupeKey = event.dedupeKey))
    }

    private fun updateIssuanceResult(
        dedupeKey: String,
        status: IssuanceStatus,
        couponId: Long? = null,
    ) {
        val result = couponIssuanceResultRepository.findByDedupeKey(dedupeKey)
        if (result != null) {
            val updatedResult = result.copy(
                status = status,
                couponId = couponId,
                updatedAt = ZonedDateTime.now(),
            )
            couponIssuanceResultRepository.save(updatedResult)
        } else {
            logger.warn("CouponIssuanceResult not found: dedupeKey={}", dedupeKey)
        }
    }
}

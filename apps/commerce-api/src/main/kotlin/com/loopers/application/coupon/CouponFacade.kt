package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponIssueMessage
import com.loopers.domain.coupon.CouponService
import com.loopers.domain.coupon.CouponType
import com.loopers.domain.event.EventTopics
import com.loopers.domain.user.UserService
import com.loopers.infrastructure.coupon.CouponCounterRedisRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.time.ZonedDateTime
import java.util.UUID

@Component
class CouponFacade(
    private val couponService: CouponService,
    private val userService: UserService,
    private val kafkaTemplate: KafkaTemplate<Any, Any>,
    private val couponCounterRedisRepository: CouponCounterRedisRepository,
) {
    fun issueCoupon(loginId: String, password: String, couponTemplateId: Long): IssuedCouponInfo {
        val user = getAuthenticatedUser(loginId, password)
        val template = couponService.getCouponTemplate(couponTemplateId)

        if (template.hasIssuanceLimit()) {
            throw CoreException(ErrorType.BAD_REQUEST, "선착순 쿠폰은 비동기 발급 API를 이용해 주세요.")
        }

        val issuedCoupon = couponService.issueCoupon(user.id, couponTemplateId)
        return IssuedCouponInfo.from(issuedCoupon, template)
    }

    fun requestCouponIssue(loginId: String, password: String, couponTemplateId: Long): CouponIssueRequestInfo {
        val user = getAuthenticatedUser(loginId, password)
        val template = couponService.getCouponTemplate(couponTemplateId)

        if (template.isExpired()) {
            throw CoreException(ErrorType.BAD_REQUEST, "만료된 쿠폰입니다.")
        }
        if (couponService.hasCouponIssueRequest(user.id, couponTemplateId)) {
            throw CoreException(ErrorType.CONFLICT, "이미 발급 요청이 존재합니다.")
        }

        val requestId = UUID.randomUUID().toString()
        couponService.saveCouponIssueRequest(user.id, couponTemplateId, requestId)

        kafkaTemplate.send(
            EventTopics.COUPON_ISSUE_REQUESTS,
            couponTemplateId.toString(),
            CouponIssueMessage(requestId = requestId, userId = user.id, couponTemplateId = couponTemplateId),
        )

        return CouponIssueRequestInfo(requestId = requestId, status = "PENDING")
    }

    fun getCouponIssueStatus(requestId: String): CouponIssueRequestInfo {
        val request = couponService.getCouponIssueRequest(requestId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "발급 요청을 찾을 수 없습니다.")
        return CouponIssueRequestInfo(requestId = request.requestId, status = request.status.name, reason = request.reason)
    }

    fun getUserCoupons(loginId: String, password: String): List<IssuedCouponInfo> {
        val user = getAuthenticatedUser(loginId, password)
        val issuedCoupons = couponService.getUserIssuedCoupons(user.id)
        return issuedCoupons.map { issuedCoupon ->
            val template = couponService.getCouponTemplate(issuedCoupon.couponTemplateId)
            IssuedCouponInfo.from(issuedCoupon, template)
        }
    }

    fun getCouponTemplates(pageable: Pageable): Page<CouponTemplateInfo> {
        return couponService.getCouponTemplates(pageable)
            .map { CouponTemplateInfo.from(it) }
    }

    fun getCouponTemplate(id: Long): CouponTemplateInfo {
        return couponService.getCouponTemplate(id)
            .let { CouponTemplateInfo.from(it) }
    }

    fun createCouponTemplate(
        name: String,
        type: CouponType,
        value: Long,
        minOrderAmount: Long?,
        expiredAt: ZonedDateTime,
    ): CouponTemplateInfo {
        return couponService.createCouponTemplate(name, type, value, minOrderAmount, expiredAt)
            .let { CouponTemplateInfo.from(it) }
    }

    fun updateCouponTemplate(
        id: Long,
        name: String,
        value: Long,
        minOrderAmount: Long?,
        expiredAt: ZonedDateTime,
    ): CouponTemplateInfo {
        return couponService.updateCouponTemplate(id, name, value, minOrderAmount, expiredAt)
            .let { CouponTemplateInfo.from(it) }
    }

    fun deleteCouponTemplate(id: Long) {
        couponService.deleteCouponTemplate(id)
    }

    fun getIssuedCoupons(couponTemplateId: Long, pageable: Pageable): Page<IssuedCouponInfo> {
        val template = couponService.getCouponTemplate(couponTemplateId)
        return couponService.getIssuedCouponsByCouponTemplateId(couponTemplateId, pageable)
            .map { IssuedCouponInfo.from(it, template) }
    }

    private fun getAuthenticatedUser(loginId: String, password: String) =
        userService.getUserByLoginIdAndPassword(loginId, password)
            ?: throw CoreException(ErrorType.NOT_FOUND, "User not found")
}

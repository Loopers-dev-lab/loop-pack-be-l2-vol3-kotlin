package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CouponService(
    private val couponTemplateRepository: CouponTemplateRepository,
    private val couponRepository: CouponRepository,
) {

    // ===== CouponTemplate 관련 =====

    fun getTemplateInfo(templateId: Long): CouponTemplate {
        return couponTemplateRepository.findById(templateId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "쿠폰 템플릿이 존재하지 않습니다.")
    }

    fun getActiveTemplates(pageable: Pageable): Page<CouponTemplate> {
        return couponTemplateRepository.findActiveTemplates(pageable)
    }

    @Transactional
    fun createTemplate(
        name: String,
        type: CouponType,
        value: java.math.BigDecimal,
        minOrderAmount: java.math.BigDecimal,
        expiredAt: java.time.ZonedDateTime,
    ): CouponTemplate {
        val template = CouponTemplate.create(
            name = name,
            type = type,
            value = value,
            minOrderAmount = minOrderAmount,
            expiredAt = expiredAt,
        )
        return couponTemplateRepository.save(template)
    }

    @Transactional
    fun updateTemplate(
        templateId: Long,
        name: String,
        value: java.math.BigDecimal,
        minOrderAmount: java.math.BigDecimal,
        expiredAt: java.time.ZonedDateTime,
    ): CouponTemplate {
        val template = getTemplateInfo(templateId)
        template.updateInfo(name, value, minOrderAmount, expiredAt)
        return couponTemplateRepository.save(template)
    }

    @Transactional
    fun deleteTemplate(templateId: Long) {
        val template = getTemplateInfo(templateId)
        template.delete()
        couponTemplateRepository.save(template)
    }

    // ===== Coupon 발급 관련 =====

    @Transactional
    fun issueCoupon(userId: Long, templateId: Long): Coupon {
        // 템플릿 유효성 확인
        val template = getTemplateInfo(templateId)

        // 중복 발급 확인 (사용자 + 템플릿 조합으로 1개만 가능)
        val existingCoupon = couponRepository.findByUserIdAndTemplateId(userId, templateId)
        if (existingCoupon != null) {
            throw CoreException(ErrorType.BAD_REQUEST, "이미 발급받은 쿠폰입니다.")
        }

        // 새 쿠폰 발급
        val newCoupon = Coupon.issue(userId, template)
        return couponRepository.save(newCoupon)
    }

    // ===== Coupon 조회 관련 =====

    fun getMyCoupons(userId: Long, pageable: Pageable): Page<Coupon> {
        return couponRepository.findByUserId(userId, pageable)
    }

    fun getMyIssuedCoupons(userId: Long, pageable: Pageable): Page<Coupon> {
        return couponRepository.findByUserIdAndStatus(userId, CouponStatus.ISSUED, pageable)
    }

    fun getMyUsedCoupons(userId: Long, pageable: Pageable): Page<Coupon> {
        return couponRepository.findByUserIdAndStatus(userId, CouponStatus.USED, pageable)
    }

    fun getIssuedCoupons(templateId: Long, pageable: Pageable): Page<Coupon> {
        return couponRepository.findByTemplateId(templateId, pageable)
    }

    fun getCoupon(couponId: Long): Coupon {
        return couponRepository.findById(couponId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "쿠폰이 존재하지 않습니다.")
    }

    // ===== Coupon 사용 관련 =====

    @Transactional
    fun useCoupon(couponId: Long, orderAmount: java.math.BigDecimal) {
        val coupon = getCoupon(couponId)
        val template = getTemplateInfo(coupon.templateId)

        // 유효성 검증
        if (!coupon.isValid()) {
            throw CoreException(ErrorType.BAD_REQUEST, "사용할 수 없는 쿠폰입니다.")
        }

        // 주문 금액 검증
        if (!template.isApplicable(orderAmount)) {
            throw CoreException(
                ErrorType.BAD_REQUEST,
                "주문 금액이 최소 주문액(${template.minOrderAmount})에 미달합니다.",
            )
        }

        // 쿠폰 사용
        coupon.use()
    }

    // ===== 쿠폰 할인 계산 관련 =====

    fun calculateDiscount(couponId: Long, orderAmount: java.math.BigDecimal): java.math.BigDecimal {
        val coupon = getCoupon(couponId)
        val template = getTemplateInfo(coupon.templateId)

        if (!coupon.canApplyToOrder(orderAmount)) {
            throw CoreException(ErrorType.BAD_REQUEST, "적용할 수 없는 쿠폰입니다.")
        }

        if (!template.isApplicable(orderAmount)) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문 금액이 적격이 아닙니다.")
        }

        return template.type.calculateDiscount(orderAmount, template.value)
    }
}

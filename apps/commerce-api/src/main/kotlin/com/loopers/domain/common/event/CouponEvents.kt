package com.loopers.domain.common.event

data class CouponIssuedEvent(
    val issuedCouponId: Long,
    val couponTemplateId: Long,
    val memberId: Long,
)

data class CouponUsedEvent(
    val issuedCouponId: Long,
    val memberId: Long,
)

data class CouponRestoredEvent(
    val issuedCouponId: Long,
)

data class CouponTemplateDeletedEvent(
    val couponTemplateId: Long,
)

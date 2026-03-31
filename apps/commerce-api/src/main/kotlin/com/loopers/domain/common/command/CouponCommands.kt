package com.loopers.domain.common.command

data class UseCouponCommand(
    val issuedCouponId: Long,
    val memberId: Long,
)

data class RestoreCouponCommand(
    val issuedCouponId: Long,
)

package com.loopers.domain.coupon

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "coupon_issuance_result")
class CouponIssuanceResult(
    @Id
    val dedupeKey: String,

    val userId: Long,

    val templateId: Long,

    @Enumerated(EnumType.STRING)
    var status: IssuanceStatus = IssuanceStatus.PENDING,

    @Column(name = "coupon_id")
    var couponId: Long? = null,

    val createdAt: ZonedDateTime = ZonedDateTime.now(),

    var updatedAt: ZonedDateTime? = null,
)

enum class IssuanceStatus {
    PENDING, // 처리 대기 중
    ISSUED, // 발급 완료
    SOLD_OUT, // 품절
    DUPLICATE, // 중복 발급
    TEMPLATE_EXPIRED, // 템플릿 만료
    TEMPLATE_NOT_FOUND, // 템플릿 없음
}

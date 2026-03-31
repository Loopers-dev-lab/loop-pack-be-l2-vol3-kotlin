package com.loopers.domain.coupon

import java.math.BigDecimal
import java.time.ZonedDateTime

class Coupon private constructor(
    val id: Long?,
    val name: String,
    val type: Type,
    val discountValue: Long,
    val minOrderAmount: BigDecimal?,
    val expiredAt: ZonedDateTime,
    val issueLimit: Long?,
    val issuedCount: Long,
    val deletedAt: ZonedDateTime?,
    val createdAt: ZonedDateTime?,
) {
    fun isExpired(): Boolean = !ZonedDateTime.now().isBefore(expiredAt)

    fun isDeleted(): Boolean = deletedAt != null

    fun isSoldOut(): Boolean = issueLimit != null && issuedCount >= issueLimit

    fun issue(): Coupon {
        if (isDeleted()) {
            throw IllegalStateException("coupon is deleted")
        }
        if (isExpired()) {
            throw IllegalStateException("coupon is expired")
        }
        if (isSoldOut()) {
            throw IllegalStateException("coupon is sold out")
        }
        return Coupon(
            id = id,
            name = name,
            type = type,
            discountValue = discountValue,
            minOrderAmount = minOrderAmount,
            expiredAt = expiredAt,
            issueLimit = issueLimit,
            issuedCount = issuedCount + 1,
            deletedAt = deletedAt,
            createdAt = createdAt,
        )
    }

    fun update(
        name: String,
        discountValue: Long,
        minOrderAmount: BigDecimal?,
        expiredAt: ZonedDateTime,
        issueLimit: Long?,
    ): Coupon {
        if (expiredAt.isBefore(ZonedDateTime.now())) {
            throw IllegalStateException("coupon expiration is invalid")
        }
        return Coupon(
            id = id,
            name = name,
            type = type,
            discountValue = discountValue,
            minOrderAmount = minOrderAmount,
            expiredAt = expiredAt,
            issueLimit = issueLimit,
            issuedCount = issuedCount,
            deletedAt = deletedAt,
            createdAt = createdAt,
        )
    }

    fun delete(): Coupon = Coupon(
        id = id,
        name = name,
        type = type,
        discountValue = discountValue,
        minOrderAmount = minOrderAmount,
        expiredAt = expiredAt,
        issueLimit = issueLimit,
        issuedCount = issuedCount,
        deletedAt = ZonedDateTime.now(),
        createdAt = createdAt,
    )

    enum class Type {
        FIXED,
        RATE,
    }

    companion object {
        fun register(
            name: String,
            type: Type,
            discountValue: Long,
            minOrderAmount: BigDecimal?,
            expiredAt: ZonedDateTime,
            issueLimit: Long? = null,
        ): Coupon {
            if (expiredAt.isBefore(ZonedDateTime.now())) {
                throw IllegalStateException("coupon expiration is invalid")
            }
            return Coupon(
                id = null,
                name = name,
                type = type,
                discountValue = discountValue,
                minOrderAmount = minOrderAmount,
                expiredAt = expiredAt,
                issueLimit = issueLimit,
                issuedCount = 0L,
                deletedAt = null,
                createdAt = null,
            )
        }

        fun retrieve(
            id: Long,
            name: String,
            type: Type,
            discountValue: Long,
            minOrderAmount: BigDecimal?,
            expiredAt: ZonedDateTime,
            issueLimit: Long?,
            issuedCount: Long,
            deletedAt: ZonedDateTime?,
            createdAt: ZonedDateTime,
        ): Coupon = Coupon(
            id = id,
            name = name,
            type = type,
            discountValue = discountValue,
            minOrderAmount = minOrderAmount,
            expiredAt = expiredAt,
            issueLimit = issueLimit,
            issuedCount = issuedCount,
            deletedAt = deletedAt,
            createdAt = createdAt,
        )
    }
}

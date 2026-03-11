package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponChanger
import com.loopers.domain.coupon.CouponReader
import com.loopers.domain.coupon.CouponRegister
import com.loopers.domain.coupon.CouponRemover
import com.loopers.domain.coupon.CouponType
import com.loopers.domain.coupon.IssuedCouponReader
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Component
class CouponAdminUseCase(
    private val couponRegister: CouponRegister,
    private val couponReader: CouponReader,
    private val couponChanger: CouponChanger,
    private val couponRemover: CouponRemover,
    private val issuedCouponReader: IssuedCouponReader,
) {

    @Transactional
    fun register(command: RegisterCommand): CouponInfo.Detail {
        val coupon = couponRegister.register(
            name = command.name,
            type = command.type,
            discountValue = command.discountValue,
            minOrderAmount = command.minOrderAmount,
            expiredAt = command.expiredAt,
        )
        return CouponInfo.Detail.from(coupon)
    }

    @Transactional(readOnly = true)
    fun getById(id: Long): CouponInfo.Detail {
        val coupon = couponReader.getById(id)
        return CouponInfo.Detail.from(coupon)
    }

    @Transactional(readOnly = true)
    fun getAll(pageable: Pageable): Page<CouponInfo.Main> {
        return couponReader.getAll(pageable).map { CouponInfo.Main.from(it) }
    }

    @Transactional
    fun update(id: Long, command: UpdateCommand): CouponInfo.Detail {
        val coupon = couponChanger.changeInfo(
            id = id,
            name = command.name,
            type = command.type,
            discountValue = command.discountValue,
            minOrderAmount = command.minOrderAmount,
            expiredAt = command.expiredAt,
        )
        return CouponInfo.Detail.from(coupon)
    }

    @Transactional
    fun delete(id: Long) {
        couponRemover.remove(id)
    }

    @Transactional(readOnly = true)
    fun getIssuedCoupons(couponId: Long, pageable: Pageable): Page<CouponInfo.IssuedMain> {
        return issuedCouponReader.getAllByCouponId(couponId, pageable)
            .map { CouponInfo.IssuedMain.from(it) }
    }

    data class RegisterCommand(
        val name: String,
        val type: CouponType,
        val discountValue: Long,
        val minOrderAmount: Long?,
        val expiredAt: ZonedDateTime,
    )

    data class UpdateCommand(
        val name: String,
        val type: CouponType,
        val discountValue: Long,
        val minOrderAmount: Long?,
        val expiredAt: ZonedDateTime,
    )
}

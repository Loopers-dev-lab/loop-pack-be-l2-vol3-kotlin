package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CouponService(
    private val couponRepository: CouponRepository,
    private val issuedCouponRepository: IssuedCouponRepository,
) {
    @Transactional
    fun register(command: RegisterCouponCommand): CouponInfo {
        val coupon = CouponModel(
            name = command.name,
            discountType = command.discountType,
            discountValue = command.discountValue,
            totalQuantity = command.totalQuantity,
            expiredAt = command.expiredAt,
        )
        val saved = couponRepository.save(coupon)
        return CouponInfo.from(saved)
    }

    @Transactional
    fun modify(id: Long, command: ModifyCouponCommand): CouponInfo {
        val coupon = couponRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다.")
        coupon.update(
            newName = command.name,
            newDiscountType = command.discountType,
            newDiscountValue = command.discountValue,
            newTotalQuantity = command.totalQuantity,
            newExpiredAt = command.expiredAt,
        )
        val saved = couponRepository.save(coupon)
        return CouponInfo.from(saved)
    }

    @Transactional
    fun delete(id: Long) {
        val coupon = couponRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다.")
        coupon.delete()
        couponRepository.save(coupon)
    }

    @Transactional(readOnly = true)
    fun getCoupon(id: Long): CouponInfo {
        val coupon = couponRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다.")
        return CouponInfo.from(coupon)
    }

    @Transactional(readOnly = true)
    fun getCoupons(pageable: Pageable): Slice<CouponInfo> {
        return couponRepository.findAll(pageable).map { CouponInfo.from(it) }
    }

    @Transactional(readOnly = true)
    fun getIssuedCoupons(couponId: Long, pageable: Pageable): Slice<IssuedCouponInfo> {
        return issuedCouponRepository.findAllByCouponId(couponId, pageable).map { IssuedCouponInfo.from(it) }
    }

    @Transactional
    fun issueCoupon(command: IssueCouponCommand): IssuedCouponInfo {
        val coupon = couponRepository.findByIdWithLock(command.couponId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다.")

        val alreadyIssued = issuedCouponRepository.findByCouponIdAndUserId(command.couponId, command.userId)
        if (alreadyIssued != null) {
            throw CoreException(ErrorType.CONFLICT, "이미 발급받은 쿠폰입니다.")
        }

        coupon.issue()
        couponRepository.save(coupon)

        val issuedCoupon = IssuedCouponModel(
            couponId = command.couponId,
            userId = command.userId,
            discountType = coupon.discountType,
            discountValue = coupon.discountValue,
            expiredAt = coupon.expiredAt,
        )
        val saved = issuedCouponRepository.save(issuedCoupon)
        return IssuedCouponInfo.from(saved)
    }

    @Transactional(readOnly = true)
    fun getUserCoupons(userId: Long, pageable: Pageable): Slice<IssuedCouponInfo> {
        return issuedCouponRepository.findAllByUserId(userId, pageable).map { IssuedCouponInfo.from(it) }
    }

    @Transactional
    fun restoreUsedCoupon(issuedCouponId: Long) {
        val issuedCoupon = issuedCouponRepository.findById(issuedCouponId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "발급된 쿠폰을 찾을 수 없습니다.")
        issuedCoupon.restoreUsage()
        issuedCouponRepository.save(issuedCoupon)
    }

    @Transactional
    fun validateAndUseForOrder(issuedCouponId: Long, userId: Long): CouponDiscountInfo {
        val issuedCoupon = issuedCouponRepository.findById(issuedCouponId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "발급된 쿠폰을 찾을 수 없습니다.")

        issuedCoupon.validate(userId)
        issuedCoupon.use()
        issuedCouponRepository.save(issuedCoupon)

        return CouponDiscountInfo(
            discountType = issuedCoupon.discountType,
            discountValue = issuedCoupon.discountValue,
        )
    }
}

package com.loopers.application.order

import com.loopers.application.UseCase
import com.loopers.domain.coupon.CouponService
import com.loopers.domain.order.CancelOrderCommand
import com.loopers.domain.order.OrderService
import com.loopers.domain.user.UserService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserCancelOrderUseCase(
    private val orderService: OrderService,
    private val couponService: CouponService,
    private val userService: UserService,
) : UseCase<CancelOrderCriteria, CancelOrderResult> {

    @Transactional
    override fun execute(criteria: CancelOrderCriteria): CancelOrderResult {
        val user = userService.getUser(criteria.loginId)
        val command = CancelOrderCommand(orderId = criteria.orderId, userId = user.id)
        val info = orderService.cancelOrder(command)

        info.issuedCouponId?.let { couponService.restoreUsedCoupon(it) }

        return CancelOrderResult.from(info)
    }
}

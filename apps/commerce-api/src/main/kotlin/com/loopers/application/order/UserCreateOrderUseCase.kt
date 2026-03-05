package com.loopers.application.order

import com.loopers.application.UseCase
import com.loopers.domain.coupon.CouponService
import com.loopers.domain.order.CreateOrderCommand
import com.loopers.domain.order.CreateOrderItemCommand
import com.loopers.domain.order.OrderService
import com.loopers.domain.user.UserService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserCreateOrderUseCase(
    private val orderService: OrderService,
    private val couponService: CouponService,
    private val userService: UserService,
) : UseCase<CreateOrderCriteria, CreateOrderResult> {

    @Transactional
    override fun execute(criteria: CreateOrderCriteria): CreateOrderResult {
        val couponDiscount = criteria.couponId?.let { issuedCouponId ->
            val user = userService.getUser(criteria.loginId)
            couponService.validateAndUseForOrder(issuedCouponId, user.id)
        }

        val command = CreateOrderCommand(
            loginId = criteria.loginId,
            items = criteria.items.map {
                CreateOrderItemCommand(productId = it.productId, quantity = it.quantity)
            },
            couponDiscount = couponDiscount,
            issuedCouponId = criteria.couponId,
        )
        val info = orderService.createOrder(command)
        return CreateOrderResult.from(info)
    }
}

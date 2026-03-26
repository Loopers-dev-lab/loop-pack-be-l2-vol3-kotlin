package com.loopers.application.order

import com.loopers.application.UseCase
import com.loopers.domain.common.event.OrderCreatedEvent
import com.loopers.domain.coupon.CouponService
import com.loopers.domain.order.CreateOrderCommand
import com.loopers.domain.order.CreateOrderItemCommand
import com.loopers.domain.order.OrderService
import com.loopers.domain.user.UserService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserCreateOrderUseCase(
    private val orderService: OrderService,
    private val couponService: CouponService,
    private val userService: UserService,
    private val eventPublisher: ApplicationEventPublisher,
) : UseCase<CreateOrderCriteria, CreateOrderResult> {

    @Transactional
    override fun execute(criteria: CreateOrderCriteria): CreateOrderResult {
        val user = userService.getUser(criteria.loginId)
        val couponDiscount = criteria.couponId?.let { issuedCouponId ->
            couponService.validateAndUseForOrder(issuedCouponId, user.id)
        }

        val command = CreateOrderCommand(
            userId = user.id,
            userName = criteria.loginId,
            items = criteria.items.map {
                CreateOrderItemCommand(productId = it.productId, quantity = it.quantity)
            },
            couponDiscount = couponDiscount,
            issuedCouponId = criteria.couponId,
        )
        val info = orderService.createOrder(command)

        eventPublisher.publishEvent(
            OrderCreatedEvent(
                orderId = info.id,
                userId = user.id,
                loginId = criteria.loginId,
                totalPrice = info.totalPrice,
            ),
        )

        return CreateOrderResult.from(info)
    }
}

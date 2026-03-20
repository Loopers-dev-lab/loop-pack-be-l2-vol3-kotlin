package com.loopers.application.payment

import com.loopers.application.UseCase
import com.loopers.domain.order.OrderService
import com.loopers.domain.order.OrderStatus
import com.loopers.domain.payment.CreatePaymentCommand
import com.loopers.domain.payment.PaymentService
import com.loopers.domain.payment.PgClient
import com.loopers.domain.payment.PgPaymentRequest
import com.loopers.domain.user.UserService
import com.loopers.infrastructure.payment.PgProperties
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.RoundingMode

@Component
class UserRequestPaymentUseCase(
    private val userService: UserService,
    private val orderService: OrderService,
    private val paymentService: PaymentService,
    private val pgClient: PgClient,
    private val pgProperties: PgProperties,
) : UseCase<RequestPaymentCriteria, RequestPaymentResult> {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun execute(criteria: RequestPaymentCriteria): RequestPaymentResult {
        val user = userService.getUser(criteria.loginId)
        val order = orderService.getOrder(criteria.orderId, user.id)

        if (order.status != OrderStatus.ORDERED) {
            throw CoreException(ErrorType.BAD_REQUEST, "결제 가능한 주문 상태가 아닙니다.")
        }

        val paymentInfo = paymentService.createPayment(
            CreatePaymentCommand(
                orderId = order.id,
                userId = user.id,
                amount = order.totalPrice,
                cardType = criteria.cardType,
                cardNo = criteria.cardNo,
            ),
        )

        try {
            val pgResponse = pgClient.requestPayment(
                PgPaymentRequest(
                    orderId = order.id.toString(),
                    userId = user.id,
                    amount = order.totalPrice.setScale(0, RoundingMode.HALF_UP).toLong(),
                    callbackUrl = pgProperties.callbackUrl,
                    cardType = criteria.cardType,
                    cardNo = criteria.cardNo,
                ),
            )
            val updated = paymentService.updateTransactionKey(paymentInfo.id, pgResponse.transactionKey)
            return RequestPaymentResult.from(updated)
        } catch (e: CoreException) {
            log.warn("PG 결제 요청 실패: paymentId={}, error={}", paymentInfo.id, e.message)
            return RequestPaymentResult.from(paymentInfo)
        }
    }
}

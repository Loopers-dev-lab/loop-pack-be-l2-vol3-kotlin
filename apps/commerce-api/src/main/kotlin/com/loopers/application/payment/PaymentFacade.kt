package com.loopers.application.payment

import com.loopers.domain.order.OrderService
import com.loopers.domain.order.OrderStatus
import com.loopers.domain.payment.CardType
import com.loopers.domain.payment.Payment
import com.loopers.domain.payment.PaymentService
import com.loopers.domain.payment.PaymentStatus
import com.loopers.domain.payment.PgClient
import com.loopers.domain.payment.PgPaymentRequest
import com.loopers.domain.user.UserService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PaymentFacade(
    private val paymentService: PaymentService,
    private val orderService: OrderService,
    private val userService: UserService,
    private val pgClient: PgClient,
    @Value("\${pg.callback-url}") private val callbackUrl: String,
) {
    fun requestPayment(
        loginId: String,
        password: String,
        orderId: Long,
        cardType: CardType,
        cardNo: String,
    ): PaymentInfo {
        val user = getAuthenticatedUser(loginId, password)
        val order = orderService.getOrder(orderId)

        if (order.userId != user.id) {
            throw CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다.")
        }
        if (order.status != OrderStatus.PENDING) {
            throw CoreException(ErrorType.BAD_REQUEST, "결제 가능한 주문이 아닙니다.")
        }
        if (paymentService.hasSuccessfulPayment(orderId)) {
            throw CoreException(ErrorType.CONFLICT, "이미 결제된 주문입니다.")
        }

        val maskedCardNo = Payment.maskCardNo(cardNo)
        val payment = paymentService.createPayment(
            Payment(
                orderId = orderId,
                userId = user.id,
                cardType = cardType,
                cardNo = maskedCardNo,
                amount = order.totalPrice,
            ),
        )

        try {
            val pgResponse = pgClient.requestPayment(
                PgPaymentRequest(
                    orderId = orderId.toString().padStart(6, '0'),
                    cardType = cardType.name,
                    cardNo = cardNo,
                    amount = order.totalPrice,
                    callbackUrl = callbackUrl,
                    userId = user.id,
                ),
            )
            paymentService.updateAfterPgResponse(payment.id, pgResponse.transactionKey, pgResponse.status, pgResponse.reason)
        } catch (e: CoreException) {
            paymentService.updateAfterPgResponse(payment.id, null, "FAILED", e.message)
        }

        return PaymentInfo.from(paymentService.getPayment(payment.id))
    }

    @Transactional
    fun handleCallback(
        transactionKey: String,
        orderId: String,
        status: String,
        reason: String?,
    ) {
        val payment = paymentService.getPaymentByTransactionKey(transactionKey)

        if (payment.status != PaymentStatus.PENDING) {
            return
        }

        val paymentStatus = PaymentStatus.valueOf(status)
        payment.complete(paymentStatus, reason)

        if (paymentStatus == PaymentStatus.SUCCESS) {
            val order = orderService.getOrder(payment.orderId)
            order.pay()
        }
    }

    fun getPayment(loginId: String, password: String, paymentId: Long): PaymentInfo {
        val user = getAuthenticatedUser(loginId, password)
        val payment = paymentService.getPayment(paymentId)
        if (payment.userId != user.id) {
            throw CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다.")
        }
        return PaymentInfo.from(payment)
    }

    fun getPaymentsByOrderId(loginId: String, password: String, orderId: Long): List<PaymentInfo> {
        val user = getAuthenticatedUser(loginId, password)
        val order = orderService.getOrder(orderId)
        if (order.userId != user.id) {
            throw CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다.")
        }
        return paymentService.getPaymentsByOrderId(orderId)
            .map { PaymentInfo.from(it) }
    }

    private fun getAuthenticatedUser(loginId: String, password: String) =
        userService.getUserByLoginIdAndPassword(loginId, password)
            ?: throw CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다.")
}

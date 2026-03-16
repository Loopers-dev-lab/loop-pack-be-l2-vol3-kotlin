package com.loopers.application.payment

import com.loopers.domain.common.vo.OrderId
import com.loopers.domain.order.repository.OrderRepository
import com.loopers.domain.payment.PgClient
import com.loopers.domain.payment.PgPaymentRequest
import com.loopers.domain.payment.PgResultStatus
import com.loopers.domain.payment.model.CardType
import com.loopers.domain.payment.model.Payment
import com.loopers.domain.payment.model.PaymentStatus
import com.loopers.domain.payment.repository.PaymentRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RequestPaymentUseCase(
    private val orderRepository: OrderRepository,
    private val paymentRepository: PaymentRepository,
    private val pgClient: PgClient,
) {
    @Transactional
    fun execute(command: PaymentCommand.RequestPayment): PaymentInfo {
        // 1. 기존 진행 중 결제 확인
        paymentRepository.findByOrderId(command.orderId)?.let { existingPayment ->
            if (existingPayment.status == PaymentStatus.REQUESTED || existingPayment.status == PaymentStatus.TIMEOUT) {
                throw CoreException(ErrorType.CONFLICT, "이미 결제가 진행 중인 주문입니다.")
            }
        }

        // 2. Order 조회 + CREATED 상태 확인 (markPendingPayment 내부에서 검증)
        val order = orderRepository.findById(OrderId(command.orderId))
            ?: throw CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다.")

        // 3. Order → PENDING_PAYMENT (CREATED 아니면 BAD_REQUEST 발생)
        order.markPendingPayment()
        orderRepository.save(order)

        val cardType = CardType.valueOf(command.cardType)
        val amount = order.totalPrice.value.toLong()

        // 4. PG 결제 요청
        val pgResult = pgClient.requestPayment(
            PgPaymentRequest(
                orderId = command.orderId,
                cardType = cardType,
                cardNo = command.cardNo,
                amount = amount,
                callbackUrl = command.callbackUrl,
            ),
        )

        // 5. 결과에 따라 Payment 생성 + 상태 반영
        val payment = Payment.create(
            orderId = command.orderId,
            cardType = cardType,
            cardNo = command.cardNo,
            amount = amount,
        )

        when (pgResult.status) {
            PgResultStatus.SUCCESS -> {
                // REQUESTED 상태 유지 (콜백으로 SUCCESS 전환 예정)
            }
            PgResultStatus.FAILED -> {
                payment.markFailed(pgResult.reason ?: "PG 결제 실패")
                order.markFailed()
                orderRepository.save(order)
            }
            PgResultStatus.TIMEOUT -> {
                payment.markTimeout()
                // 스케줄러가 나중에 처리 (TODO: 다음 주 이벤트 기반 전환)
            }
        }

        val savedPayment = paymentRepository.save(payment)
        return PaymentInfo.from(savedPayment)
    }
}

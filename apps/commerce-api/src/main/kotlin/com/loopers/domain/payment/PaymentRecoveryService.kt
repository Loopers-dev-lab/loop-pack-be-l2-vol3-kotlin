package com.loopers.domain.payment

import com.loopers.domain.order.OrderService
import com.loopers.support.error.CoreException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 타임아웃 등으로 실패한 결제를 복구하는 서비스
 *
 * PENDING 상태의 Receipt에 대해 PG에서 실제 결제 여부를 조회하여 상태를 업데이트합니다.
 *
 * 동작:
 * 1. PENDING 상태인 Receipt 중 생성 후 1분 이상 경과한 것을 조회
 * 2. PG에 checkPaymentStatus() 호출
 * 3. 결과에 따라 Receipt 상태 업데이트:
 *    - PG COMPLETED → Receipt COMPLETED (+ Order PAID)
 *    - PG FAILED → Receipt FAILED
 *    - PG PENDING → PENDING 유지 (다음 주기 재시도)
 */
@Service
@Transactional
class PaymentRecoveryService(
    private val receiptService: ReceiptService,
    private val orderService: OrderService,
    private val paymentClient: PaymentClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        // PG 요청 후 1분 이상 경과해야 복구 시도 (PG 처리 중 상태 변경 방지)
        private const val RECOVERY_DELAY_MINUTES = 1L
    }

    /**
     * PENDING/TIMEOUT 상태의 실패한 결제를 복구합니다.
     *
     * @return 복구된 Receipt 개수
     */
    fun recoverFailedPayments(): Int {
        val pendingReceipts = receiptService.getPendingReceiptsForRecovery(RECOVERY_DELAY_MINUTES)
        log.info("Found ${pendingReceipts.size} pending receipts for recovery")

        var recoveredCount = 0
        for (receipt in pendingReceipts) {
            try {
                val recovered = attemptRecovery(receipt)
                if (recovered) {
                    recoveredCount++
                }
            } catch (e: Exception) {
                log.warn(
                    "Failed to recover payment for receipt id=${receipt.id}, transactionId=${receipt.transactionId}. " +
                        "Will retry in next cycle. Cause: ${e.message}",
                    e,
                )
            }
        }

        log.info("Payment recovery completed: $recoveredCount out of ${pendingReceipts.size} recovered")
        return recoveredCount
    }

    /**
     * 개별 Receipt의 복구를 시도합니다.
     *
     * @return 복구 성공 여부
     */
    private fun attemptRecovery(receipt: Receipt): Boolean {
        val transactionId = receipt.transactionId
            ?: return false.also {
                log.warn("Receipt id=${receipt.id} has no transactionId, skipping recovery")
            }

        return try {
            val pgStatus = paymentClient.checkPaymentStatus(receipt.orderId)
            log.debug("PG status for $transactionId: ${pgStatus.status}")

            when (pgStatus.status.uppercase()) {
                "COMPLETED" -> {
                    // 결제 성공 - Receipt와 Order 업데이트
                    receiptService.markAsCompleted(receipt)
                    orderService.markOrderAsPaid(receipt.orderId)
                    log.info("Payment recovered: transactionId=$transactionId, orderId=${receipt.orderId}")
                    true
                }

                "FAILED" -> {
                    // 결제 실패
                    receiptService.markAsFailed(receipt, pgStatus.reason)
                    log.info("Payment marked as failed: transactionId=$transactionId, reason=${pgStatus.reason}")
                    true
                }

                "CANCELLED" -> {
                    // 결제 취소
                    receiptService.markAsCancelled(receipt, pgStatus.reason)
                    log.info("Payment marked as cancelled: transactionId=$transactionId")
                    true
                }

                "PENDING", "TIMEOUT" -> {
                    // 아직 PG에서 처리 중이거나 타임아웃 - 다음 주기에 재시도
                    log.debug("Payment still pending on PG side: transactionId=$transactionId, status=${pgStatus.status}")
                    false
                }

                else -> {
                    log.warn("Unknown payment status from PG: transactionId=$transactionId, status=${pgStatus.status}")
                    false
                }
            }
        } catch (e: CoreException) {
            // PG 조회 실패 - 다음 주기에 재시도
            log.debug("Failed to check payment status on PG: ${e.message}")
            false
        }
    }
}

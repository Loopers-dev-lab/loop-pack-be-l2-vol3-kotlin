package com.loopers.domain.payment

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

/**
 * 30분 이상 PENDING 상태인 Receipt을 FAILED로 자동 처리
 * - PG 콜백이 오지 않는 경우를 대비한 타임아웃 처리
 * - 매 5분마다 실행
 */
@Component
class ReceiptTimeoutHandler(
    private val receiptService: ReceiptService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        // 콜백 대기 타임아웃: 30분
        private const val PENDING_TIMEOUT_MINUTES = 30L
    }

    @Scheduled(fixedDelay = 5 * 60 * 1000) // 5분마다 실행
    @Transactional
    fun handlePendingPaymentTimeout() {
        val timeoutThreshold = ZonedDateTime.now().minusMinutes(PENDING_TIMEOUT_MINUTES)

        try {
            val pendingReceipts = receiptService.findPendingReceiptsOlderThan(timeoutThreshold)

            if (pendingReceipts.isEmpty()) {
                return
            }

            log.info("Processing {} pending receipts for timeout", pendingReceipts.size)

            pendingReceipts.forEach { receipt ->
                try {
                    receipt.markAsFailed()
                    receiptService.save(receipt)
                    log.warn(
                        "Receipt timeout: receiptId={}, orderId={}, transactionId={}",
                        receipt.id,
                        receipt.orderId,
                        receipt.transactionId,
                    )
                } catch (e: Exception) {
                    log.error(
                        "Failed to mark receipt as failed: receiptId={}, orderId={}",
                        receipt.id,
                        receipt.orderId,
                        e,
                    )
                }
            }
        } catch (e: Exception) {
            log.error("Error processing pending payment timeout", e)
        }
    }
}

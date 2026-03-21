package com.loopers.domain.payment

class FakePgClient : PgClient {
    // 시나리오 설정용
    var requestPaymentResult: PgPaymentResult? = null
    var requestPaymentException: RuntimeException? = null
    var transactionDetail: PgTransactionDetail? = null
    var transactionDetailException: RuntimeException? = null

    // orderId별 응답 설정 (transactionDetailException보다 우선 적용)
    val transactionDetailByOrderId: MutableMap<Long, PgTransactionDetail?> = mutableMapOf()
    val transactionDetailExceptionByOrderId: MutableMap<Long, RuntimeException> = mutableMapOf()

    // 호출 기록
    val requestPaymentCalls = mutableListOf<PgPaymentRequest>()
    val getTransactionCalls = mutableListOf<Long>()

    private var transactionKeySequence = 1

    override fun requestPayment(request: PgPaymentRequest): PgPaymentResult {
        requestPaymentCalls.add(request)
        requestPaymentException?.let { throw it }
        return requestPaymentResult ?: PgPaymentResult(
            transactionKey = "TR-${transactionKeySequence++}",
            status = PgResultStatus.SUCCESS,
        )
    }

    override fun getTransactionByOrderId(orderId: Long): PgTransactionDetail? {
        getTransactionCalls.add(orderId)
        transactionDetailExceptionByOrderId[orderId]?.let { throw it }
        transactionDetailException?.let { throw it }
        return if (transactionDetailByOrderId.containsKey(orderId)) {
            transactionDetailByOrderId[orderId]
        } else {
            transactionDetail
        }
    }

    fun reset() {
        requestPaymentResult = null
        requestPaymentException = null
        transactionDetail = null
        transactionDetailException = null
        transactionDetailByOrderId.clear()
        transactionDetailExceptionByOrderId.clear()
        requestPaymentCalls.clear()
        getTransactionCalls.clear()
        transactionKeySequence = 1
    }
}

package com.loopers.domain.payment

class FakePgClient : PgClient {
    // 시나리오 설정용
    var requestPaymentResult: PgPaymentResult? = null
    var requestPaymentException: RuntimeException? = null
    var transactionDetail: PgTransactionDetail? = null
    var transactionDetailException: RuntimeException? = null

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
        transactionDetailException?.let { throw it }
        return transactionDetail
    }

    fun reset() {
        requestPaymentResult = null
        requestPaymentException = null
        transactionDetail = null
        transactionDetailException = null
        requestPaymentCalls.clear()
        getTransactionCalls.clear()
        transactionKeySequence = 1
    }
}

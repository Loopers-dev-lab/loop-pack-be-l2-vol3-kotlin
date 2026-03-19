package com.loopers.domain.payment

interface PaymentGateway {
    fun requestPayment(memberId: Long, request: Request): RequestResult

    fun getTransaction(memberId: Long, transactionKey: String): LookupResult

    fun findLatestTransactionByOrderId(memberId: Long, orderId: String): LookupResult

    data class Request(
        val orderId: String,
        val cardType: CardType,
        val cardNo: String,
        val amount: Long,
    )

    sealed interface RequestResult {
        data class Accepted(
            val transactionKey: String,
            val status: PgPaymentStatus,
            val reason: String?,
        ) : RequestResult

        data class RequestFailed(val reason: String) : RequestResult

        data class Unknown(val reason: String) : RequestResult
    }

    sealed interface LookupResult {
        data class Found(
            val transactionKey: String,
            val status: PgPaymentStatus,
            val reason: String?,
        ) : LookupResult

        data object NotFound : LookupResult

        data class Unavailable(val reason: String) : LookupResult
    }
}

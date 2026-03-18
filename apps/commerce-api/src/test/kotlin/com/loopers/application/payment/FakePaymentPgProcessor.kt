package com.loopers.application.payment

class FakePaymentPgProcessor : PaymentPgProcessor {
    data class ProcessPaymentCall(
        val paymentId: Long,
        val orderId: Long,
        val amount: Long,
        val cardType: String,
        val cardNo: String,
    )

    val processPaymentCalls = mutableListOf<ProcessPaymentCall>()

    override fun processPayment(paymentId: Long, orderId: Long, amount: Long, cardType: String, cardNo: String) {
        val maskedCardNo = maskCardNo(cardNo)
        processPaymentCalls.add(ProcessPaymentCall(paymentId, orderId, amount, cardType, maskedCardNo))
    }

    private fun maskCardNo(cardNo: String): String {
        val parts = cardNo.split("-")
        return if (parts.size == 4) "${parts[0]}-****-****-${parts[3]}" else "****"
    }
}

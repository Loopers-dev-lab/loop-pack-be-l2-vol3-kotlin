package com.loopers.infrastructure.payment

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(
    name = "pgClient",
    url = "\${pg.base-url}",
)
interface PgFeignClient {
    @PostMapping("/api/v1/payments")
    fun requestPayment(
        @RequestHeader("X-USER-ID") userId: String,
        @RequestBody request: PgFeignPaymentRequest,
    ): PgFeignApiResponse<PgFeignTransactionResponse>

    @GetMapping("/api/v1/payments")
    fun getTransactionsByOrderId(
        @RequestHeader("X-USER-ID") userId: String,
        @RequestParam("orderId") orderId: String,
    ): PgFeignApiResponse<PgFeignOrderResponse>
}

data class PgFeignApiResponse<T>(
    val meta: PgFeignMeta,
    val data: T?,
)

data class PgFeignMeta(
    val result: String,
    val errorCode: String?,
    val message: String?,
)

data class PgFeignPaymentRequest(
    val orderId: String,
    val cardType: String,
    val cardNo: String,
    val amount: Long,
    val callbackUrl: String,
    val idempotencyKey: String? = null,
)

data class PgFeignTransactionResponse(
    val transactionKey: String,
    val status: String,
    val reason: String?,
)

data class PgFeignOrderResponse(
    val orderId: String,
    val transactions: List<PgFeignTransactionResponse>,
)

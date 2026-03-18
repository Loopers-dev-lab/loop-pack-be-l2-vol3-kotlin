package com.loopers.infrastructure.pg

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(
    name = "pgClient",
    url = "\${pg.base-url}",
    configuration = [PgClientConfig::class],
)
interface PgClient {

    @PostMapping("/api/v1/payments")
    fun requestPayment(
        @RequestHeader("X-USER-ID") userId: String,
        @RequestBody request: PgPaymentRequest,
    ): PgApiResponse<PgPaymentResponse>

    @GetMapping("/api/v1/payments/{transactionKey}")
    fun getPaymentStatus(
        @RequestHeader("X-USER-ID") userId: String,
        @PathVariable transactionKey: String,
    ): PgApiResponse<PgTransactionDetailResponse>

    @GetMapping("/api/v1/payments")
    fun getPaymentsByOrderId(
        @RequestHeader("X-USER-ID") userId: String,
        @RequestParam orderId: String,
    ): PgApiResponse<PgOrderTransactionsResponse>
}

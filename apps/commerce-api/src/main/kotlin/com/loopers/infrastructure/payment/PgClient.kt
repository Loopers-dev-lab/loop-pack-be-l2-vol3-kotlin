package com.loopers.infrastructure.payment

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(
    name = "pg-simulator",
    url = "\${pg.base-url}",
)
interface PgClient {

    @PostMapping("/api/v1/payments")
    fun requestPayment(
        @RequestHeader("X-USER-ID") userId: String,
        @RequestBody request: PgPaymentRequest,
    ): PgPaymentResponse

    @GetMapping("/api/v1/payments/{transactionKey}")
    fun getTransaction(
        @RequestHeader("X-USER-ID") userId: String,
        @PathVariable transactionKey: String,
    ): PgTransactionDetailResponse

    @GetMapping("/api/v1/payments")
    fun getTransactionsByOrderId(
        @RequestHeader("X-USER-ID") userId: String,
        @RequestParam("orderId") orderId: String,
    ): PgOrderResponse
}

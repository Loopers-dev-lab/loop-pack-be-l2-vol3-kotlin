package com.loopers.infrastructure.payment

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader

@FeignClient(name = "pg-simulator", url = "\${pg.base-url}")
interface PgFeignClient {
    @PostMapping("/api/v1/payments")
    fun requestPayment(
        @RequestHeader("X-USER-ID") userId: String,
        @RequestBody request: PgPaymentRequestDto,
    ): PgApiResponse<PgPaymentResponseDto>

    @GetMapping("/api/v1/payments/{transactionKey}")
    fun getPayment(
        @RequestHeader("X-USER-ID") userId: String,
        @PathVariable transactionKey: String,
    ): PgApiResponse<PgPaymentDetailDto>
}

data class PgPaymentRequestDto(
    val orderId: String,
    val cardType: String,
    val cardNo: String,
    val amount: Long,
    val callbackUrl: String,
)

data class PgApiResponse<T>(
    val data: T?,
    val success: Boolean,
)

data class PgPaymentResponseDto(
    val transactionKey: String,
    val status: String,
    val reason: String?,
)

data class PgPaymentDetailDto(
    val transactionKey: String,
    val orderId: String,
    val cardType: String,
    val cardNo: String,
    val amount: Long,
    val status: String,
    val reason: String?,
)

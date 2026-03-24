package com.loopers.interfaces.api.user.payment

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity

@Tag(name = "[User] Payment V1 API", description = "결제 API 입니다.")
interface UserPaymentV1ApiSpec {

    @Operation(summary = "결제 요청", description = "결제를 요청합니다.")
    fun create(
        loginId: String,
        password: String,
        idempotencyKey: String,
        request: UserPaymentV1Request.Create,
    ): ResponseEntity<ApiResponse<UserPaymentV1Response.Created>>

    @Operation(summary = "결제 상세 조회", description = "결제 상세 정보를 조회합니다. PENDING 상태이면 PG 상태를 재조회(read-repair)합니다.")
    fun detail(
        loginId: String,
        password: String,
        paymentId: Long,
    ): ResponseEntity<ApiResponse<UserPaymentV1Response.Detail>>

    @Operation(summary = "결제 수동 복구", description = "timeout fallback 상태의 결제를 PG orderId 조회로 수동 복구합니다.")
    fun reconcile(
        loginId: String,
        password: String,
        paymentId: Long,
    ): ResponseEntity<ApiResponse<UserPaymentV1Response.Reconciled>>
}

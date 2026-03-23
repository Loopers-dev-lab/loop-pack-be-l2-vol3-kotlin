package com.loopers.interfaces.api.payment

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse

@Tag(name = "Payment Admin V1 API", description = "결제 관리 API (관리자)")
interface PaymentAdminV1ApiSpec {

    @Operation(summary = "결제 상태 수동 동기화", description = "PG 시스템에 결제 상태를 확인하여 내부 상태를 동기화합니다.")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "동기화 성공"),
            SwaggerResponse(responseCode = "400", description = "트랜잭션 키 없음"),
            SwaggerResponse(responseCode = "404", description = "존재하지 않는 결제"),
        ],
    )
    fun syncPayment(
        paymentId: Long,
    ): ApiResponse<PaymentAdminV1Dto.PaymentAdminResponse>
}

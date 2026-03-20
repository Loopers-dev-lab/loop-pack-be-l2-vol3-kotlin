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
}

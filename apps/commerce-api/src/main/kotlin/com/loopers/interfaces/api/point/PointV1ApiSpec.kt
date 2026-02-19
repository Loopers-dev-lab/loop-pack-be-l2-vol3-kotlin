package com.loopers.interfaces.api.point

import com.loopers.interfaces.support.ApiResponse
import com.loopers.interfaces.support.auth.AuthUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Point V1 API", description = "포인트 API")
interface PointV1ApiSpec {

    @Operation(summary = "포인트 충전", description = "포인트를 충전합니다.")
    fun chargePoints(
        @Parameter(hidden = true) @AuthUser userId: Long,
        request: PointV1Dto.ChargeRequest,
    ): ApiResponse<PointV1Dto.BalanceResponse>

    @Operation(summary = "포인트 잔액 조회", description = "포인트 잔액을 조회합니다.")
    fun getBalance(
        @Parameter(hidden = true) @AuthUser userId: Long,
    ): ApiResponse<PointV1Dto.BalanceResponse>
}

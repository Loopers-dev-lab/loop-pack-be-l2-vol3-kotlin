package com.loopers.interfaces.api.point.spec

import com.loopers.interfaces.api.point.dto.PointV1Dto
import com.loopers.interfaces.support.ApiResponse
import com.loopers.interfaces.support.auth.AuthUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

@Tag(name = "Point V1 API", description = "포인트 API")
interface PointV1ApiSpec {

    @Operation(summary = "포인트 충전", description = "포인트를 충전합니다.")
    fun chargePoints(
        @Parameter(hidden = true) @AuthUser userId: Long,
        @Parameter(description = "충전 금액")
        @Min(value = 1, message = "충전 금액은 1 이상이어야 합니다.")
        @Max(value = 10_000_000, message = "1회 충전 한도는 10,000,000포인트입니다.")
        amount: Long,
    ): ApiResponse<PointV1Dto.BalanceResponse>

    @Operation(summary = "포인트 잔액 조회", description = "포인트 잔액을 조회합니다.")
    fun getBalance(
        @Parameter(hidden = true) @AuthUser userId: Long,
    ): ApiResponse<PointV1Dto.BalanceResponse>
}

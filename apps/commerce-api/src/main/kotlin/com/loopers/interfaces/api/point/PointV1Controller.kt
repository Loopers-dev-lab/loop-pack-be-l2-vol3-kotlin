package com.loopers.interfaces.api.point

import com.loopers.application.point.ChargePointUseCase
import com.loopers.application.point.GetUserPointUseCase
import com.loopers.interfaces.api.point.dto.PointV1Dto
import com.loopers.interfaces.api.point.spec.PointV1ApiSpec
import com.loopers.interfaces.support.ApiResponse
import com.loopers.interfaces.support.auth.AuthUser
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/users/points")
class PointV1Controller(
    private val chargePointUseCase: ChargePointUseCase,
    private val getUserPointUseCase: GetUserPointUseCase,
) : PointV1ApiSpec {

    @PostMapping("/charge")
    override fun chargePoints(
        @AuthUser userId: Long,
        @RequestParam amount: Long,
    ): ApiResponse<PointV1Dto.BalanceResponse> {
        return chargePointUseCase.execute(userId, amount)
            .let { PointV1Dto.BalanceResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping
    override fun getBalance(
        @AuthUser userId: Long,
    ): ApiResponse<PointV1Dto.BalanceResponse> {
        return getUserPointUseCase.execute(userId)
            .let { PointV1Dto.BalanceResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}

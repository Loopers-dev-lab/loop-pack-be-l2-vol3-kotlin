package com.loopers.interfaces.api.point

import com.loopers.domain.point.PointChargingService
import com.loopers.domain.point.UserPointService
import com.loopers.interfaces.support.ApiResponse
import com.loopers.interfaces.support.auth.AuthUser
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users/points")
class PointV1Controller(
    private val pointChargingService: PointChargingService,
    private val userPointService: UserPointService,
) : PointV1ApiSpec {

    @PostMapping("/charge")
    override fun chargePoints(
        @AuthUser userId: Long,
        @RequestBody @Valid request: PointV1Dto.ChargeRequest,
    ): ApiResponse<PointV1Dto.BalanceResponse> {
        return pointChargingService.charge(userId, request.amount)
            .let { PointV1Dto.BalanceResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping
    override fun getBalance(
        @AuthUser userId: Long,
    ): ApiResponse<PointV1Dto.BalanceResponse> {
        return userPointService.getBalance(userId)
            .let { PointV1Dto.BalanceResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}

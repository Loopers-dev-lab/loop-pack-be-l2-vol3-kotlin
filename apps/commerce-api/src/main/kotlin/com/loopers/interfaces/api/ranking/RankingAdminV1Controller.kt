package com.loopers.interfaces.api.ranking

import com.loopers.domain.ranking.RankingWeightsService
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api-admin/v1/rankings/weights")
class RankingAdminV1Controller(
    private val rankingWeightsService: RankingWeightsService,
) : RankingAdminV1ApiSpec {

    @GetMapping
    override fun getWeights(): ApiResponse<RankingAdminV1Dto.WeightsResponse> {
        return rankingWeightsService.getCurrent()
            .let { RankingAdminV1Dto.WeightsResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PutMapping
    override fun updateWeights(
        @RequestBody request: RankingAdminV1Dto.UpdateWeightsRequest,
    ): ApiResponse<RankingAdminV1Dto.WeightsResponse> {
        val weights = try {
            request.toWeights()
        } catch (e: IllegalArgumentException) {
            throw CoreException(ErrorType.BAD_REQUEST, e.message)
        }
        rankingWeightsService.update(weights)
        return RankingAdminV1Dto.WeightsResponse.from(weights)
            .let { ApiResponse.success(it) }
    }
}

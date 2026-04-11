package com.loopers.interfaces.api.ranking

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse

@Tag(name = "Ranking Admin V1 API", description = "랭킹 운영 API (관리자)")
interface RankingAdminV1ApiSpec {

    @Operation(summary = "랭킹 가중치 조회", description = "현재 적용 중인 랭킹 점수 계산 가중치를 조회합니다.")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "조회 성공"),
        ],
    )
    fun getWeights(): ApiResponse<RankingAdminV1Dto.WeightsResponse>

    @Operation(
        summary = "랭킹 가중치 변경",
        description = "랭킹 점수 계산에 사용되는 가중치를 변경합니다. 배포 없이 즉시 반영됩니다.",
    )
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "변경 성공"),
            SwaggerResponse(responseCode = "400", description = "잘못된 가중치 값"),
        ],
    )
    fun updateWeights(
        request: RankingAdminV1Dto.UpdateWeightsRequest,
    ): ApiResponse<RankingAdminV1Dto.WeightsResponse>
}

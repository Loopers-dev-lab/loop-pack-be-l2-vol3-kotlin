package com.loopers.interfaces.api.like

import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.auth.LoginUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse

@Tag(name = "Like V1 API", description = "좋아요 API (대고객)")
interface LikeV1ApiSpec {

    @Operation(summary = "좋아요 등록", description = "상품에 좋아요를 등록합니다.")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "등록 성공"),
            SwaggerResponse(responseCode = "404", description = "존재하지 않는 상품"),
            SwaggerResponse(responseCode = "409", description = "이미 좋아요한 상품"),
        ],
    )
    fun likeProduct(loginUser: LoginUser, productId: Long): ApiResponse<Unit>

    @Operation(summary = "좋아요 취소", description = "상품에 좋아요를 취소합니다.")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "취소 성공"),
            SwaggerResponse(responseCode = "404", description = "좋아요하지 않은 상품"),
        ],
    )
    fun unlikeProduct(loginUser: LoginUser, productId: Long): ApiResponse<Unit>

    @Operation(summary = "좋아요 목록 조회", description = "내 좋아요 상품 목록을 조회합니다.")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "조회 성공"),
        ],
    )
    fun getLikedProducts(loginUser: LoginUser): ApiResponse<List<LikeV1Dto.LikedProductResponse>>
}

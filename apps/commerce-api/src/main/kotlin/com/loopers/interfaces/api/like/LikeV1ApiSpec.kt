package com.loopers.interfaces.api.like

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "Like V1 API", description = "좋아요 관련 API")
interface LikeV1ApiSpec {

    @Operation(summary = "좋아요 등록", description = "상품에 좋아요를 등록합니다. 이미 좋아요한 상품이면 기존 좋아요를 유지합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "좋아요 등록 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
            SwaggerApiResponse(responseCode = "404", description = "상품 없음"),
        ],
    )
    fun addLike(
        @Parameter(description = "로그인 ID", required = true)
        loginId: String,
        @Parameter(description = "비밀번호", required = true)
        password: String,
        @Parameter(description = "상품 ID", required = true)
        productId: Long,
    ): ApiResponse<Any>

    @Operation(summary = "좋아요 취소", description = "상품의 좋아요를 취소합니다. 좋아요가 없으면 아무 작업도 하지 않습니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "좋아요 취소 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
        ],
    )
    fun cancelLike(
        @Parameter(description = "로그인 ID", required = true)
        loginId: String,
        @Parameter(description = "비밀번호", required = true)
        password: String,
        @Parameter(description = "상품 ID", required = true)
        productId: Long,
    ): ApiResponse<Any>

    @Operation(summary = "좋아요 목록 조회", description = "본인의 좋아요 목록을 조회합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
            SwaggerApiResponse(responseCode = "403", description = "본인의 좋아요만 조회 가능"),
        ],
    )
    fun getUserLikes(
        @Parameter(description = "로그인 ID", required = true)
        loginId: String,
        @Parameter(description = "비밀번호", required = true)
        password: String,
        @Parameter(description = "유저 ID", required = true)
        userId: Long,
    ): ApiResponse<List<LikeV1Dto.LikeResponse>>
}

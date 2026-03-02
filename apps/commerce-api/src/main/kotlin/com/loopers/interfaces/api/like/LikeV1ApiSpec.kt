package com.loopers.interfaces.api.like

import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.auth.AuthenticatedUser
import com.loopers.interfaces.api.auth.RequireAuth
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.PathVariable

@Tag(name = "Like V1 API", description = "좋아요 API")
interface LikeV1ApiSpec {

    @Operation(
        summary = "상품 좋아요 등록",
        description = "상품에 좋아요를 등록합니다.",
        parameters = [
            Parameter(
                name = "X-Loopers-LoginId",
                description = "사용자 로그인 ID",
                required = true,
                schema = Schema(type = "string"),
                `in` = ParameterIn.HEADER,
            ),
            Parameter(
                name = "X-Loopers-LoginPw",
                description = "사용자 비밀번호",
                required = true,
                schema = Schema(type = "string"),
                `in` = ParameterIn.HEADER,
            ),
        ],
    )
    fun like(
        @PathVariable productId: Long,
        @Parameter(hidden = true) @RequireAuth authenticatedUser: AuthenticatedUser,
    ): ApiResponse<LikeV1Dto.LikeResponse>

    @Operation(
        summary = "상품 좋아요 취소",
        description = "상품의 좋아요를 취소합니다.",
        parameters = [
            Parameter(
                name = "X-Loopers-LoginId",
                description = "사용자 로그인 ID",
                required = true,
                schema = Schema(type = "string"),
                `in` = ParameterIn.HEADER,
            ),
            Parameter(
                name = "X-Loopers-LoginPw",
                description = "사용자 비밀번호",
                required = true,
                schema = Schema(type = "string"),
                `in` = ParameterIn.HEADER,
            ),
        ],
    )
    fun unlike(
        @PathVariable productId: Long,
        @Parameter(hidden = true) @RequireAuth authenticatedUser: AuthenticatedUser,
    ): ApiResponse<Unit>

    @Operation(
        summary = "내가 좋아요 한 상품 목록 조회",
        description = "인증된 사용자가 좋아요한 상품 목록을 조회합니다.",
        parameters = [
            Parameter(
                name = "X-Loopers-LoginId",
                description = "사용자 로그인 ID",
                required = true,
                schema = Schema(type = "string"),
                `in` = ParameterIn.HEADER,
            ),
            Parameter(
                name = "X-Loopers-LoginPw",
                description = "사용자 비밀번호",
                required = true,
                schema = Schema(type = "string"),
                `in` = ParameterIn.HEADER,
            ),
        ],
    )
    fun getLikedProducts(
        @PathVariable userId: Long,
        @Parameter(hidden = true) @RequireAuth authenticatedUser: AuthenticatedUser,
        pageable: Pageable,
    ): ApiResponse<List<LikeV1Dto.LikeResponse>>
}

package com.loopers.interfaces.api.brand

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Brand V1 API", description = "브랜드 관련 API")
interface BrandV1ApiSpec {

    @Operation(summary = "브랜드 등록", description = "새로운 브랜드를 등록합니다.")
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "등록 성공"),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "중복 브랜드명",
                content = [Content(schema = Schema(implementation = ApiResponse::class))],
            ),
        ],
    )
    fun register(
        @Parameter(hidden = true) loginId: String,
        @Parameter(hidden = true) password: String,
        request: BrandV1Dto.RegisterRequest,
    ): ApiResponse<BrandV1Dto.DetailResponse>

    @Operation(summary = "브랜드 상세 조회", description = "브랜드 ID로 상세 정보를 조회합니다.")
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "브랜드 없음",
                content = [Content(schema = Schema(implementation = ApiResponse::class))],
            ),
        ],
    )
    fun getById(id: Long): ApiResponse<BrandV1Dto.DetailResponse>

    @Operation(summary = "활성 브랜드 목록 조회", description = "활성 상태의 모든 브랜드를 조회합니다.")
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        ],
    )
    fun getAllActive(): ApiResponse<List<BrandV1Dto.MainResponse>>

    @Operation(summary = "브랜드명 변경", description = "브랜드명을 변경합니다.")
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "변경 성공"),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "중복 브랜드명",
                content = [Content(schema = Schema(implementation = ApiResponse::class))],
            ),
        ],
    )
    fun changeName(
        @Parameter(hidden = true) loginId: String,
        @Parameter(hidden = true) password: String,
        id: Long,
        request: BrandV1Dto.ChangeNameRequest,
    ): ApiResponse<BrandV1Dto.DetailResponse>

    @Operation(summary = "브랜드 삭제(비활성화)", description = "브랜드를 비활성화합니다.")
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "브랜드 없음",
                content = [Content(schema = Schema(implementation = ApiResponse::class))],
            ),
        ],
    )
    fun remove(
        @Parameter(hidden = true) loginId: String,
        @Parameter(hidden = true) password: String,
        id: Long,
    ): ApiResponse<Any>
}

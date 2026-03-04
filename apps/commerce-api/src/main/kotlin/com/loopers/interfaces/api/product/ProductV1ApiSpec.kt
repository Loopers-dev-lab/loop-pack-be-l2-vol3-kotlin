package com.loopers.interfaces.api.product

import com.loopers.domain.product.ProductSortType
import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Product V1 API", description = "상품 관련 API")
interface ProductV1ApiSpec {

    @Operation(summary = "상품 등록", description = "새로운 상품을 등록합니다.")
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "등록 성공"),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "브랜드 없음",
                content = [Content(schema = Schema(implementation = ApiResponse::class))],
            ),
        ],
    )
    fun register(
        @Parameter(hidden = true) loginId: String,
        @Parameter(hidden = true) password: String,
        request: ProductV1Dto.RegisterRequest,
    ): ApiResponse<ProductV1Dto.DetailResponse>

    @Operation(summary = "상품 상세 조회", description = "상품 ID로 상세 정보를 조회합니다.")
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "상품 없음",
                content = [Content(schema = Schema(implementation = ApiResponse::class))],
            ),
        ],
    )
    fun getById(id: Long): ApiResponse<ProductV1Dto.DetailResponse>

    @Operation(summary = "상품 목록 조회", description = "상품 목록을 정렬 기준에 따라 조회합니다.")
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        ],
    )
    fun getAll(
        sortType: ProductSortType,
        brandId: Long?,
    ): ApiResponse<List<ProductV1Dto.MainResponse>>

    @Operation(summary = "상품 정보 변경", description = "상품 정보를 변경합니다.")
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "변경 성공"),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "상품 없음",
                content = [Content(schema = Schema(implementation = ApiResponse::class))],
            ),
        ],
    )
    fun changeInfo(
        @Parameter(hidden = true) loginId: String,
        @Parameter(hidden = true) password: String,
        id: Long,
        request: ProductV1Dto.ChangeInfoRequest,
    ): ApiResponse<ProductV1Dto.DetailResponse>

    @Operation(summary = "상품 삭제(판매중지)", description = "상품을 판매 중지합니다.")
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "상품 없음",
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

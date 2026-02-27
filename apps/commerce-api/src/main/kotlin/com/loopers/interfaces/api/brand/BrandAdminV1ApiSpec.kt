package com.loopers.interfaces.api.brand

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse

@Tag(name = "Brand Admin V1 API", description = "브랜드 관리자 API")
interface BrandAdminV1ApiSpec {

    @Operation(summary = "브랜드 목록 조회", description = "전체 브랜드 목록을 페이징하여 조회합니다.")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "조회 성공"),
        ],
    )
    fun getBrands(page: Int, size: Int): ApiResponse<Page<BrandAdminV1Dto.BrandAdminResponse>>

    @Operation(summary = "브랜드 상세 조회", description = "브랜드 상세 정보를 조회합니다.")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "조회 성공"),
            SwaggerResponse(responseCode = "404", description = "존재하지 않는 브랜드"),
        ],
    )
    fun getBrand(brandId: Long): ApiResponse<BrandAdminV1Dto.BrandAdminResponse>

    @Operation(summary = "브랜드 등록", description = "새로운 브랜드를 등록합니다.")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "등록 성공"),
            SwaggerResponse(responseCode = "400", description = "잘못된 요청"),
        ],
    )
    fun createBrand(request: BrandAdminV1Dto.CreateRequest): ApiResponse<BrandAdminV1Dto.BrandAdminResponse>

    @Operation(summary = "브랜드 수정", description = "브랜드 정보를 수정합니다.")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "수정 성공"),
            SwaggerResponse(responseCode = "404", description = "존재하지 않는 브랜드"),
        ],
    )
    fun updateBrand(brandId: Long, request: BrandAdminV1Dto.UpdateRequest): ApiResponse<BrandAdminV1Dto.BrandAdminResponse>

    @Operation(summary = "브랜드 삭제", description = "브랜드를 삭제합니다. 소속 상품과 좋아요도 함께 삭제됩니다.")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "삭제 성공"),
            SwaggerResponse(responseCode = "404", description = "존재하지 않는 브랜드"),
        ],
    )
    fun deleteBrand(brandId: Long): ApiResponse<Unit>
}

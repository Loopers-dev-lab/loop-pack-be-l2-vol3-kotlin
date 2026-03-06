package com.loopers.interfaces.admin.brand

import com.loopers.domain.brand.dto.BrandInfo
import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page

@Tag(name = "Brand Admin V1 API", description = "브랜드 관리 API (Admin 전용)")
interface AdminBrandV1ApiSpec {

    @Operation(
        summary = "브랜드 정보 조회",
        description = "브랜드 정보를 조회 합니다",
        responses = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "브랜드가 존재하지 않음"),
        ],
    )
    fun getBrandInfo(
        @Parameter(
            description = "브랜드 ID",
            required = true,
        )
        brandId: Long,
    ): ApiResponse<BrandInfo>

    @Operation(
        summary = "브랜드 목록 조회",
        description = "브랜드 목록을 페이징으로 조회합니다 (Admin 전용)",
        responses = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        ],
    )
    fun getAllBrands(
        @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
        page: Int,
        @Parameter(description = "페이지 크기", example = "20")
        size: Int,
    ): ApiResponse<Page<BrandInfo>>

    @Operation(
        summary = "브랜드 생성",
        description = "새로운 브랜드를 생성합니다 (Admin 전용)",
        responses = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 성공"),
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        ],
    )
    fun createBrand(
        request: AdminBrandV1Dto.CreateBrandRequest,
    ): ApiResponse<BrandInfo>

    @Operation(
        summary = "브랜드 수정",
        description = "브랜드 정보를 수정합니다 (Admin 전용)",
        responses = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "브랜드가 존재하지 않음"),
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        ],
    )
    fun updateBrand(
        @Parameter(
            description = "브랜드 ID",
            required = true,
        )
        brandId: Long,
        request: AdminBrandV1Dto.UpdateBrandRequest,
    ): ApiResponse<Unit>

    @Operation(
        summary = "브랜드 삭제",
        description = "브랜드를 삭제합니다 (Admin 전용)",
        responses = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "브랜드가 존재하지 않음"),
        ],
    )
    fun deleteBrand(
        @Parameter(
            description = "브랜드 ID",
            required = true,
        )
        brandId: Long,
    ): ApiResponse<Unit>
}

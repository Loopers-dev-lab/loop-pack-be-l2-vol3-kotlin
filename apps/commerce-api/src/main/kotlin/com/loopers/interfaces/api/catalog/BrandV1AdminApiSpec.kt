package com.loopers.interfaces.api.catalog

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Brand V1 API", description = "브랜드 관련 어드민 API 입니다.")
interface BrandV1AdminApiSpec {
    @Operation(
        summary = "브랜드 등록",
        description = "새로운 브랜드를 등록합니다.",
    )
    @SwaggerResponse(responseCode = "201", description = "등록 성공")
    fun register(
        ldap: String,
        request: BrandV1AdminDto.RegisterRequest,
    )

    @Operation(
        summary = "브랜드 목록 조회",
        description = "페이지네이션으로 브랜드 전체 목록을 조회합니다.",
    )
    @SwaggerResponse(responseCode = "200", description = "조회 성공")
    fun getBrands(
        ldap: String,
        page: Int,
        size: Int,
    ): ApiResponse<BrandV1AdminDto.BrandSliceResponse>

    @Operation(
        summary = "브랜드 상세 조회",
        description = "브랜드의 상세 정보를 조회합니다.",
    )
    @SwaggerResponse(responseCode = "200", description = "조회 성공")
    fun getBrand(
        ldap: String,
        brandId: Long,
    ): ApiResponse<BrandV1AdminDto.BrandDetailResponse>

    @Operation(
        summary = "브랜드 수정",
        description = "브랜드 정보를 수정합니다.",
    )
    @SwaggerResponse(responseCode = "204", description = "수정 성공")
    fun modifyBrand(
        ldap: String,
        brandId: Long,
        request: BrandV1AdminDto.UpdateRequest,
    )

    @Operation(
        summary = "브랜드 삭제",
        description = "브랜드를 삭제합니다.",
    )
    @SwaggerResponse(responseCode = "204", description = "삭제 성공")
    fun deleteBrand(
        ldap: String,
        brandId: Long,
    )
}

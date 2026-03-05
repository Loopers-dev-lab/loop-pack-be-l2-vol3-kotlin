package com.loopers.interfaces.api.admin.brand

import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "[Admin] Brand V1 API", description = "[Admin] Brand API 입니다.")
interface AdminBrandV1ApiSpec {
    @Operation(summary = "브랜드 등록", description = "새로운 브랜드를 등록합니다.")
    fun register(
        ldap: String,
        request: AdminBrandV1Request.Register,
    ): ApiResponse<AdminBrandV1Response.Register>

    @Operation(summary = "브랜드 목록 조회", description = "브랜드 목록을 조회합니다.")
    fun getList(ldap: String, pageRequest: PageRequest): ApiResponse<PageResponse<AdminBrandV1Response.Summary>>

    @Operation(summary = "브랜드 상세 조회", description = "브랜드 상세 정보를 조회합니다.")
    fun getDetail(ldap: String, brandId: Long): ApiResponse<AdminBrandV1Response.Detail>

    @Operation(summary = "브랜드 수정", description = "브랜드 정보를 수정합니다.")
    fun update(
        ldap: String,
        brandId: Long,
        request: AdminBrandV1Request.Update,
    ): ApiResponse<AdminBrandV1Response.Update>

    @Operation(summary = "브랜드 삭제", description = "브랜드를 삭제합니다.")
    fun delete(ldap: String, brandId: Long): ApiResponse<Any>
}

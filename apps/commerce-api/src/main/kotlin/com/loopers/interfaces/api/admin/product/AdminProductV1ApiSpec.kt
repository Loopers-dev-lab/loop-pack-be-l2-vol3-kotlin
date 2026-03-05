package com.loopers.interfaces.api.admin.product

import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "[Admin] Product V1 API", description = "[Admin] Product API 입니다.")
interface AdminProductV1ApiSpec {
    @Operation(summary = "상품 등록", description = "새로운 상품을 등록합니다.")
    fun register(
        ldap: String,
        request: AdminProductV1Request.Register,
    ): ApiResponse<AdminProductV1Response.Register>

    @Operation(summary = "상품 목록 조회", description = "상품 목록을 조회합니다.")
    fun getList(ldap: String, pageRequest: PageRequest, brandId: Long?): ApiResponse<PageResponse<AdminProductV1Response.Summary>>

    @Operation(summary = "상품 상세 조회", description = "상품 상세 정보를 조회합니다.")
    fun getDetail(ldap: String, productId: Long): ApiResponse<AdminProductV1Response.Detail>

    @Operation(summary = "상품 수정", description = "상품 정보를 수정합니다.")
    fun update(
        ldap: String,
        productId: Long,
        request: AdminProductV1Request.Update,
    ): ApiResponse<AdminProductV1Response.Update>

    @Operation(summary = "상품 삭제", description = "상품을 삭제합니다.")
    fun delete(ldap: String, productId: Long): ApiResponse<Any>
}

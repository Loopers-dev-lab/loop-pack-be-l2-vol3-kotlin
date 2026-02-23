package com.loopers.interfaces.api.product

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "Product Admin V1 API", description = "상품 관리 Admin API")
interface ProductAdminV1ApiSpec {

    @Operation(
        summary = "상품 목록 조회",
        description = "상품 목록을 페이지 단위로 조회합니다.",
        parameters = [
            Parameter(
                name = "X-Loopers-Ldap",
                description = "LDAP 인증 헤더 (값: loopers.admin)",
                required = true,
                schema = Schema(type = "string"),
                `in` = ParameterIn.HEADER,
            ),
        ],
    )
    fun getProducts(
        brandId: Long?,
        sortType: ProductSortType,
        pageable: Pageable,
    ): ApiResponse<Page<ProductAdminV1Dto.ProductResponse>>

    @Operation(
        summary = "상품 상세 조회",
        description = "상품 ID로 상품 상세 정보를 조회합니다.",
        parameters = [
            Parameter(
                name = "X-Loopers-Ldap",
                description = "LDAP 인증 헤더 (값: loopers.admin)",
                required = true,
                schema = Schema(type = "string"),
                `in` = ParameterIn.HEADER,
            ),
        ],
    )
    fun getProductById(@PathVariable productId: Long): ApiResponse<ProductAdminV1Dto.ProductResponse>

    @Operation(
        summary = "상품 등록",
        description = "새로운 상품을 등록합니다.",
        parameters = [
            Parameter(
                name = "X-Loopers-Ldap",
                description = "LDAP 인증 헤더 (값: loopers.admin)",
                required = true,
                schema = Schema(type = "string"),
                `in` = ParameterIn.HEADER,
            ),
        ],
    )
    fun createProduct(
        @RequestBody request: ProductAdminV1Dto.CreateProductRequest,
    ): ApiResponse<ProductAdminV1Dto.ProductResponse>

    @Operation(
        summary = "상품 정보 수정",
        description = "상품 ID로 상품 정보를 수정합니다.",
        parameters = [
            Parameter(
                name = "X-Loopers-Ldap",
                description = "LDAP 인증 헤더 (값: loopers.admin)",
                required = true,
                schema = Schema(type = "string"),
                `in` = ParameterIn.HEADER,
            ),
        ],
    )
    fun updateProduct(
        @PathVariable productId: Long,
        @RequestBody request: ProductAdminV1Dto.UpdateProductRequest,
    ): ApiResponse<ProductAdminV1Dto.ProductResponse>

    @Operation(
        summary = "상품 삭제",
        description = "상품 ID로 상품을 삭제합니다.",
        parameters = [
            Parameter(
                name = "X-Loopers-Ldap",
                description = "LDAP 인증 헤더 (값: loopers.admin)",
                required = true,
                schema = Schema(type = "string"),
                `in` = ParameterIn.HEADER,
            ),
        ],
    )
    fun deleteProduct(@PathVariable productId: Long): ApiResponse<Unit>
}

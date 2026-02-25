package com.loopers.interfaces.api.product

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "Product Admin V1 API", description = "어드민 상품 API")
interface ProductAdminV1ApiSpec {

    @Operation(summary = "상품 목록 조회", description = "상품 목록을 페이징 조회합니다. brandId로 필터링할 수 있습니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
            SwaggerApiResponse(responseCode = "401", description = "어드민 인증 실패"),
        ],
    )
    fun getAllProducts(
        @Parameter(description = "어드민 LDAP 인증", required = true)
        ldap: String?,
        @Parameter(description = "브랜드 ID (필터)")
        brandId: Long?,
        @ParameterObject pageable: Pageable,
    ): ApiResponse<Page<ProductAdminV1Dto.ProductAdminResponse>>

    @Operation(summary = "상품 상세 조회", description = "상품 ID로 상품을 조회합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
            SwaggerApiResponse(responseCode = "401", description = "어드민 인증 실패"),
            SwaggerApiResponse(responseCode = "404", description = "상품 없음"),
        ],
    )
    fun getProduct(
        @Parameter(description = "어드민 LDAP 인증", required = true)
        ldap: String?,
        @Parameter(description = "상품 ID", required = true)
        productId: Long,
    ): ApiResponse<ProductAdminV1Dto.ProductAdminResponse>

    @Operation(summary = "상품 등록", description = "새로운 상품을 등록합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "등록 성공"),
            SwaggerApiResponse(responseCode = "400", description = "잘못된 요청"),
            SwaggerApiResponse(responseCode = "401", description = "어드민 인증 실패"),
            SwaggerApiResponse(responseCode = "404", description = "브랜드 없음"),
        ],
    )
    fun createProduct(
        @Parameter(description = "어드민 LDAP 인증", required = true)
        ldap: String?,
        request: ProductAdminV1Dto.CreateRequest,
    ): ApiResponse<ProductAdminV1Dto.ProductAdminResponse>

    @Operation(summary = "상품 수정", description = "상품 정보를 수정합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "수정 성공"),
            SwaggerApiResponse(responseCode = "400", description = "잘못된 요청"),
            SwaggerApiResponse(responseCode = "401", description = "어드민 인증 실패"),
            SwaggerApiResponse(responseCode = "404", description = "상품 없음"),
        ],
    )
    fun updateProduct(
        @Parameter(description = "어드민 LDAP 인증", required = true)
        ldap: String?,
        @Parameter(description = "상품 ID", required = true)
        productId: Long,
        request: ProductAdminV1Dto.UpdateRequest,
    ): ApiResponse<ProductAdminV1Dto.ProductAdminResponse>

    @Operation(summary = "상품 삭제", description = "상품을 삭제합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "삭제 성공"),
            SwaggerApiResponse(responseCode = "401", description = "어드민 인증 실패"),
            SwaggerApiResponse(responseCode = "404", description = "상품 없음"),
        ],
    )
    fun deleteProduct(
        @Parameter(description = "어드민 LDAP 인증", required = true)
        ldap: String?,
        @Parameter(description = "상품 ID", required = true)
        productId: Long,
    ): ApiResponse<Any>
}

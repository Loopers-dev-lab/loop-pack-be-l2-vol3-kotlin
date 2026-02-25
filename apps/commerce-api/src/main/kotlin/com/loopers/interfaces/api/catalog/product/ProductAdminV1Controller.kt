package com.loopers.interfaces.api.catalog.product

import com.loopers.application.catalog.product.CreateProductCommand
import com.loopers.application.catalog.product.ProductFacade
import com.loopers.application.catalog.product.UpdateProductCommand
import com.loopers.domain.catalog.product.ProductRepository
import com.loopers.domain.catalog.product.ProductSearchCondition
import com.loopers.domain.catalog.product.ProductSort
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.security.AdminHeader
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api-admin/v1/products")
class ProductAdminV1Controller(
    private val productFacade: ProductFacade,
    private val productRepository: ProductRepository,
) : ProductAdminV1ApiSpec {

    @GetMapping
    override fun getProducts(
        @RequestParam brandId: Long?,
        @RequestParam sort: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<List<ProductAdminV1Dto.ProductSummaryResponse>> {
        val productSort = when (sort?.lowercase()) {
            "price_asc" -> ProductSort.PRICE_ASC
            "likes_desc" -> ProductSort.LIKES_DESC
            else -> ProductSort.LATEST
        }
        val condition = ProductSearchCondition(brandId = brandId, sort = productSort, page = page, size = size)
        return productFacade.findProducts(condition)
            .map { ProductAdminV1Dto.ProductSummaryResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{productId}")
    override fun getProduct(@PathVariable productId: Long): ApiResponse<ProductAdminV1Dto.ProductDetailResponse> =
        productFacade.getProductDetail(productId)
            .let { ProductAdminV1Dto.ProductDetailResponse.from(it) }
            .let { ApiResponse.success(it) }

    @PostMapping
    override fun createProduct(
        @RequestHeader(AdminHeader.HEADER_LDAP) ldap: String,
        @RequestBody request: ProductAdminV1Dto.CreateProductRequest,
    ): ApiResponse<ProductAdminV1Dto.ProductDetailResponse> {
        if (ldap != AdminHeader.LDAP_ADMIN_VALUE) throw CoreException(ErrorType.UNAUTHORIZED, "어드민 인증에 실패했습니다.")
        return productFacade.createProduct(
            CreateProductCommand(
                brandId = request.brandId,
                name = request.name,
                description = request.description,
                price = request.price,
                stock = request.stock,
            )
        )
            .let { ProductAdminV1Dto.ProductDetailResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PutMapping("/{productId}")
    override fun updateProduct(
        @RequestHeader(AdminHeader.HEADER_LDAP) ldap: String,
        @PathVariable productId: Long,
        @RequestBody request: ProductAdminV1Dto.UpdateProductRequest,
    ): ApiResponse<ProductAdminV1Dto.ProductDetailResponse> {
        if (ldap != AdminHeader.LDAP_ADMIN_VALUE) throw CoreException(ErrorType.UNAUTHORIZED, "어드민 인증에 실패했습니다.")
        return productFacade.updateProduct(
            productId,
            UpdateProductCommand(
                name = request.name,
                description = request.description,
                price = request.price,
                stock = request.stock,
            )
        )
            .let { ProductAdminV1Dto.ProductDetailResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @DeleteMapping("/{productId}")
    override fun deleteProduct(
        @RequestHeader(AdminHeader.HEADER_LDAP) ldap: String,
        @PathVariable productId: Long,
    ): ApiResponse<Any> {
        if (ldap != AdminHeader.LDAP_ADMIN_VALUE) throw CoreException(ErrorType.UNAUTHORIZED, "어드민 인증에 실패했습니다.")
        productRepository.deleteById(productId)
        return ApiResponse.success()
    }
}

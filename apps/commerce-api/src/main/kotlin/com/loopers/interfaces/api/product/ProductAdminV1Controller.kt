package com.loopers.interfaces.api.product

import com.loopers.application.product.ProductFacade
import com.loopers.application.product.ProductService
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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
    private val productService: ProductService,
    private val productFacade: ProductFacade,
) : ProductAdminV1ApiSpec {

    @GetMapping
    override fun getAllProducts(
        @RequestHeader("X-Loopers-Ldap", required = false) ldap: String?,
        @RequestParam(required = false) brandId: Long?,
        pageable: Pageable,
    ): ApiResponse<Page<ProductAdminV1Dto.ProductAdminResponse>> {
        validateAdminAuth(ldap)
        return productService.getAllProducts(brandId, pageable)
            .map { ProductAdminV1Dto.ProductAdminResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{productId}")
    override fun getProduct(
        @RequestHeader("X-Loopers-Ldap", required = false) ldap: String?,
        @PathVariable productId: Long,
    ): ApiResponse<ProductAdminV1Dto.ProductAdminResponse> {
        validateAdminAuth(ldap)
        return productService.getProductInfo(productId)
            .let { ProductAdminV1Dto.ProductAdminResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PostMapping
    override fun createProduct(
        @RequestHeader("X-Loopers-Ldap", required = false) ldap: String?,
        @RequestBody request: ProductAdminV1Dto.CreateRequest,
    ): ApiResponse<ProductAdminV1Dto.ProductAdminResponse> {
        validateAdminAuth(ldap)
        return productFacade.createProduct(request.toCriteria())
            .let { ProductAdminV1Dto.ProductAdminResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PutMapping("/{productId}")
    override fun updateProduct(
        @RequestHeader("X-Loopers-Ldap", required = false) ldap: String?,
        @PathVariable productId: Long,
        @RequestBody request: ProductAdminV1Dto.UpdateRequest,
    ): ApiResponse<ProductAdminV1Dto.ProductAdminResponse> {
        validateAdminAuth(ldap)
        return productService.updateProduct(productId, request.toCriteria())
            .let { ProductAdminV1Dto.ProductAdminResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @DeleteMapping("/{productId}")
    override fun deleteProduct(
        @RequestHeader("X-Loopers-Ldap", required = false) ldap: String?,
        @PathVariable productId: Long,
    ): ApiResponse<Any> {
        validateAdminAuth(ldap)
        productService.deleteProduct(productId)
        return ApiResponse.success()
    }

    private fun validateAdminAuth(ldap: String?) {
        if (ldap == null || ldap != ADMIN_LDAP) {
            throw CoreException(ErrorType.UNAUTHORIZED, "어드민 인증에 실패했습니다.")
        }
    }

    companion object {
        private const val ADMIN_LDAP = "loopers.admin"
    }
}

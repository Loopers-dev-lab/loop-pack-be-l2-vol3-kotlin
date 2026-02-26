package com.loopers.interfaces.api.product

import com.loopers.application.product.ProductFacade
import com.loopers.interfaces.api.ApiResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api-admin/v1/products")
class ProductAdminV1Controller(
    private val productFacade: ProductFacade,
) : ProductAdminV1ApiSpec {

    @GetMapping
    override fun getProducts(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) brandId: Long?,
    ): ApiResponse<Page<ProductAdminV1Dto.ProductAdminResponse>> {
        val pageable = PageRequest.of(page, size)
        return productFacade.getProductsForAdmin(pageable, brandId)
            .map { ProductAdminV1Dto.ProductAdminResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{productId}")
    override fun getProduct(
        @PathVariable productId: Long,
    ): ApiResponse<ProductAdminV1Dto.ProductAdminResponse> {
        return productFacade.getProduct(productId)
            .let { ProductAdminV1Dto.ProductAdminResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PostMapping
    override fun createProduct(
        @RequestBody request: ProductAdminV1Dto.CreateRequest,
    ): ApiResponse<ProductAdminV1Dto.ProductAdminResponse> {
        return productFacade.createProduct(request.toCriteria())
            .let { ProductAdminV1Dto.ProductAdminResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PutMapping("/{productId}")
    override fun updateProduct(
        @PathVariable productId: Long,
        @RequestBody request: ProductAdminV1Dto.UpdateRequest,
    ): ApiResponse<ProductAdminV1Dto.ProductAdminResponse> {
        return productFacade.updateProduct(productId, request.toCriteria())
            .let { ProductAdminV1Dto.ProductAdminResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @DeleteMapping("/{productId}")
    override fun deleteProduct(
        @PathVariable productId: Long,
    ): ApiResponse<Unit> {
        productFacade.deleteProduct(productId)
        return ApiResponse.success(Unit)
    }
}

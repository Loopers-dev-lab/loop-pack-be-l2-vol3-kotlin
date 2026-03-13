package com.loopers.interfaces.api.product

import com.loopers.application.product.ProductFacade
import com.loopers.application.product.ProductInfo
import com.loopers.interfaces.api.ApiResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/products")
class ProductV1Controller(
    private val productFacade: ProductFacade,
) {
    @GetMapping
    fun findAll(
        @RequestParam(required = false) brandId: Long?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): ApiResponse<Page<ProductInfo>> {
        return ApiResponse.success(productFacade.getProductList(brandId, pageable))
    }

    @GetMapping("/{productId}")
    fun findById(
        @PathVariable productId: Long,
    ): ApiResponse<ProductInfo> {
        return ApiResponse.success(productFacade.getProductDetail(productId))
    }
}

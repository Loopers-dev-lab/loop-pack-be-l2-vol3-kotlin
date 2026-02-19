package com.loopers.interfaces.api.product

import com.loopers.domain.catalog.CatalogService
import com.loopers.domain.catalog.product.ProductSort
import com.loopers.interfaces.api.product.dto.ProductV1Dto
import com.loopers.interfaces.api.product.spec.ProductV1ApiSpec
import com.loopers.interfaces.support.ApiResponse
import com.loopers.interfaces.support.toSpringPage
import org.springframework.data.domain.Page
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/products")
class ProductV1Controller(
    private val catalogService: CatalogService,
) : ProductV1ApiSpec {

    @GetMapping
    override fun getProducts(
        @RequestParam(required = false) brandId: Long?,
        @RequestParam(defaultValue = "LATEST") sort: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<Page<ProductV1Dto.CustomerProductResponse>> {
        val productSort = ProductSort.valueOf(sort.uppercase())
        return catalogService.getProducts(brandId, productSort, page, size)
            .map { ProductV1Dto.CustomerProductResponse.from(it) }
            .toSpringPage()
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{productId}")
    override fun getProduct(
        @PathVariable productId: Long,
    ): ApiResponse<ProductV1Dto.ProductDetailResponse> {
        return catalogService.getProductDetail(productId)
            .let { ProductV1Dto.ProductDetailResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}

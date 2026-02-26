package com.loopers.interfaces.api.product

import com.loopers.application.product.ProductFacade
import com.loopers.interfaces.api.ApiResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/products")
class ProductV1Controller(
    private val productFacade: ProductFacade,
) : ProductV1ApiSpec {

    @GetMapping
    override fun getProducts(
        @RequestParam(required = false) brandId: Long?,
        @RequestParam(name = "sort", defaultValue = "latest") sort: ProductSortType,
        pageable: Pageable,
    ): ApiResponse<Page<ProductV1Dto.ProductResponse>> =
        productFacade.getProducts(brandId, PageRequest.of(pageable.pageNumber, pageable.pageSize, sort.sort))
            .map { ProductV1Dto.ProductResponse.from(it) }
            .let { ApiResponse.success(it) }

    @GetMapping("/{productId}")
    override fun getProductById(@PathVariable productId: Long): ApiResponse<ProductV1Dto.ProductResponse> =
        productFacade.getProductById(productId)
            .let { ProductV1Dto.ProductResponse.from(it) }
            .let { ApiResponse.success(it) }
}

package com.loopers.interfaces.api.product

import com.loopers.application.product.GetProductListUseCase
import com.loopers.application.product.GetProductUseCase
import com.loopers.application.product.ProductCommand
import com.loopers.domain.product.ProductSortType
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.PageResult
import com.loopers.support.constant.ApiPaths
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(ApiPaths.Products.BASE)
class ProductV1Controller(
    private val getProductUseCase: GetProductUseCase,
    private val getProductListUseCase: GetProductListUseCase,
) {

    @GetMapping
    fun getProducts(
        @RequestParam(required = false) brandId: Long?,
        @RequestParam(defaultValue = "LATEST") sort: ProductSortType,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<PageResult<ProductResponse>> {
        val result = getProductListUseCase.execute(
            ProductCommand.Search(
                brandId = brandId,
                sort = sort,
                page = page,
                size = size,
            ),
        )
        val responseContent = PageResult.of(
            content = result.content.map { ProductResponse.from(it) },
            page = result.page,
            size = result.size,
            totalElements = result.totalElements,
        )
        return ApiResponse.success(responseContent)
    }

    @GetMapping("/{productId}")
    fun getProduct(@PathVariable productId: Long): ApiResponse<ProductDetailResponse> {
        val product = getProductUseCase.execute(productId)
        return ApiResponse.success(ProductDetailResponse.from(product))
    }
}

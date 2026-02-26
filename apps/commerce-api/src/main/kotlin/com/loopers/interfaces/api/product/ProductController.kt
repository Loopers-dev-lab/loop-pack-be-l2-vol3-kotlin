package com.loopers.interfaces.api.product

import com.loopers.application.product.ProductFacade
import com.loopers.domain.common.PageQuery
import com.loopers.domain.common.SortOrder
import com.loopers.interfaces.api.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/products")
class ProductController(
    private val productFacade: ProductFacade,
) : ProductApiSpec {

    @GetMapping
    override fun getProducts(
        @RequestParam(required = false) brandId: Long?,
        @RequestParam(defaultValue = "latest") sort: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<ProductDto.PageResponse> {
        val pageQuery = PageQuery(page, size, toSortOrder(sort))
        return productFacade.getProducts(brandId, pageQuery)
            .let { ProductDto.PageResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{productId}")
    override fun getProduct(@PathVariable productId: Long): ApiResponse<ProductDto.DetailResponse> {
        return productFacade.getProduct(productId)
            .let { ProductDto.DetailResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    private fun toSortOrder(sort: String): SortOrder {
        return when (sort) {
            "price_asc" -> SortOrder.by("price", SortOrder.Direction.ASC)
            "likes_desc" -> SortOrder.by("likes", SortOrder.Direction.DESC)
            else -> SortOrder.UNSORTED
        }
    }
}

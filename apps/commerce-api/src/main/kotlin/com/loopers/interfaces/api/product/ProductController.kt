package com.loopers.interfaces.api.product

import com.loopers.application.product.ProductFacade
import com.loopers.interfaces.api.ApiResponse
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.GetMapping
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
        val pageable = PageRequest.of(page, size, toSort(sort))
        return productFacade.getProducts(brandId, pageable)
            .let { ProductDto.PageResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    private fun toSort(sort: String): Sort {
        return when (sort) {
            "price_asc" -> Sort.by(Sort.Direction.ASC, "price")
            "likes_desc" -> Sort.by(Sort.Direction.DESC, "likes")
            else -> Sort.by(Sort.Direction.DESC, "createdAt")
        }
    }
}

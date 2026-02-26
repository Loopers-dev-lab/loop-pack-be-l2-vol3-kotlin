package com.loopers.interfaces.api.product

import com.loopers.application.product.ProductFacade
import com.loopers.domain.product.ProductSearchCondition
import com.loopers.domain.product.ProductSort
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.cursor.CursorUtils
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
        @RequestParam(required = false, defaultValue = "latest") sort: String?,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) cursor: String?,
    ): ApiResponse<ProductV1Dto.ProductListResponse> {
        val productSort = when (sort?.lowercase()) {
            "price_asc" -> ProductSort.PRICE_ASC
            "likes_desc" -> ProductSort.LIKES_DESC
            else -> ProductSort.LATEST
        }
        val decodedCursor = cursor?.let { CursorUtils.decode(it) }
        val condition = ProductSearchCondition(
            brandId = brandId,
            sort = productSort,
            size = size,
            cursor = decodedCursor,
        )
        return productFacade.getProducts(condition)
            .let { ProductV1Dto.ProductListResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{productId}")
    override fun getProduct(
        @PathVariable productId: Long,
    ): ApiResponse<ProductV1Dto.ProductResponse> {
        return productFacade.getProduct(productId)
            .let { ProductV1Dto.ProductResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}

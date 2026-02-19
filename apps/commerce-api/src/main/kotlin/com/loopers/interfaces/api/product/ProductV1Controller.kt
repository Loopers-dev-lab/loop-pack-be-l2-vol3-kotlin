package com.loopers.interfaces.api.product

import com.loopers.domain.catalog.CatalogService
import com.loopers.domain.catalog.product.ProductSort
import com.loopers.interfaces.api.product.dto.ProductV1Dto
import com.loopers.interfaces.api.product.spec.ProductV1ApiSpec
import com.loopers.interfaces.support.ApiResponse
import com.loopers.interfaces.support.toSpringPage
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
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
        @RequestParam(defaultValue = "0") @PositiveOrZero page: Int,
        @RequestParam(defaultValue = "20") @Positive @Max(100) size: Int,
    ): ApiResponse<Page<ProductV1Dto.CustomerProductResponse>> {
        val productSort = ProductSort.entries.find { it.name == sort.uppercase() }
            ?: throw CoreException(
                ErrorType.BAD_REQUEST,
                "잘못된 정렬 기준입니다. 사용 가능한 값: ${ProductSort.entries.joinToString(", ")}",
            )
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

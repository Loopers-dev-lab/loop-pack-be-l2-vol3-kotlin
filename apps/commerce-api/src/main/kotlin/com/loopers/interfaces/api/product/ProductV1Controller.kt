package com.loopers.interfaces.api.product

import com.loopers.application.api.product.ProductFacade
import com.loopers.domain.product.dto.ProductInfo
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
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

    @GetMapping("/{productId}")
    override fun getProductInfo(
        @PathVariable productId: Long,
    ): ApiResponse<ProductInfo> = ApiResponse.success(data = productFacade.getProductInfo(productId))

    @GetMapping
    override fun getProducts(
        @RequestParam(required = false) brandId: Long?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) sort: String,
    ): ApiResponse<Page<ProductInfo>> {
        if (size !in listOf(20, 50, 100)) {
            throw CoreException(ErrorType.BAD_REQUEST, "size는 20, 50, 100만 가능합니다")
        }

        if (page < 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "page는 음수일 수 없습니다")
        }

        val sortOperation = ProductSortOption.fromValue(sort)
        val pageable = PageRequest.of(page, size, Sort.by(sortOperation.sortOrder))
        return ApiResponse.success(data = productFacade.getProducts(brandId, pageable))
    }
}

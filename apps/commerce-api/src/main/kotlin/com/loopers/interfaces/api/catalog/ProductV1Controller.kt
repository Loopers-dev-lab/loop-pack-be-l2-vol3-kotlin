package com.loopers.interfaces.api.catalog

import com.loopers.application.catalog.UserGetProductUseCase
import com.loopers.application.catalog.UserGetProductsUseCase
import com.loopers.application.catalog.UserListProductsCriteria
import com.loopers.domain.catalog.ProductSortType
import com.loopers.interfaces.api.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/products")
class ProductV1Controller(
    private val userGetProductUseCase: UserGetProductUseCase,
    private val userGetProductsUseCase: UserGetProductsUseCase,
) : ProductV1ApiSpec {
    @GetMapping
    override fun getProducts(
        @RequestHeader(value = "X-Loopers-LoginId") loginId: String,
        @RequestHeader(value = "X-Loopers-LoginPw") loginPw: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) brandId: Long?,
        @RequestParam(defaultValue = "latest") sort: String,
    ): ApiResponse<ProductV1Dto.ProductSliceResponse> {
        val sortType = ProductSortType.valueOf(sort.uppercase())
        val criteria = UserListProductsCriteria(
            page = page,
            size = size,
            brandId = brandId,
            sort = sortType,
        )
        return userGetProductsUseCase.execute(criteria)
            .let { ProductV1Dto.ProductSliceResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{productId}")
    override fun getProduct(
        @RequestHeader(value = "X-Loopers-LoginId") loginId: String,
        @RequestHeader(value = "X-Loopers-LoginPw") loginPw: String,
        @PathVariable productId: Long,
    ): ApiResponse<ProductV1Dto.ProductDetailResponse> {
        return userGetProductUseCase.execute(productId)
            .let { ProductV1Dto.ProductDetailResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}

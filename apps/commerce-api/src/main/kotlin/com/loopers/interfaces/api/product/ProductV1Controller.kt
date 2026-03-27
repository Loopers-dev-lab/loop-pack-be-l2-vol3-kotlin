package com.loopers.interfaces.api.product

import com.loopers.application.product.ProductFacade
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.auth.CurrentUser
import com.loopers.support.auth.LoginUser
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/products")
class ProductV1Controller(
    private val productFacade: ProductFacade,
) : ProductV1ApiSpec {

    @GetMapping
    override fun getProducts(
        @ParameterObject request: ProductV1Dto.GetProductsRequest,
    ): ApiResponse<Page<ProductV1Dto.ProductResponse>> {
        return productFacade.getProductsForUser(request.toPageable(), request.brandId)
            .map { ProductV1Dto.ProductResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{productId}")
    override fun getProduct(
        @PathVariable productId: Long,
        @CurrentUser loginUser: LoginUser?,
    ): ApiResponse<ProductV1Dto.ProductResponse> {
        return productFacade.getProduct(productId, loginUser?.id)
            .let { ProductV1Dto.ProductResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}

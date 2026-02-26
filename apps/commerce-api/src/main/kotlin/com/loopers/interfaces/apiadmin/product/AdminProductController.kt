package com.loopers.interfaces.apiadmin.product

import com.loopers.application.product.AdminProductFacade
import com.loopers.interfaces.common.ApiResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api-admin/v1/products")
class AdminProductController(
    private val adminProductFacade: AdminProductFacade,
) : AdminProductApiSpec {

    @PostMapping
    override fun createProduct(
        @RequestBody request: AdminProductDto.CreateRequest,
    ): ApiResponse<AdminProductDto.CreateResponse> {
        return adminProductFacade.createProduct(
            name = request.name,
            description = request.description,
            price = request.price,
            stockQuantity = request.stockQuantity,
            brandId = request.brandId,
        )
            .let { AdminProductDto.CreateResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}

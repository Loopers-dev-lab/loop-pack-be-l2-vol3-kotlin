package com.loopers.interfaces.apiadmin.product

import com.loopers.application.product.AdminProductFacade
import com.loopers.domain.common.PageQuery
import com.loopers.domain.common.SortOrder
import com.loopers.interfaces.common.ApiResponse
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api-admin/v1/products")
class AdminProductController(
    private val adminProductFacade: AdminProductFacade,
) : AdminProductApiSpec {

    @GetMapping
    override fun getProducts(
        @RequestParam(required = false) brandId: Long?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<AdminProductDto.PageResponse> {
        val pageQuery = PageQuery(page, size, SortOrder.UNSORTED)
        return adminProductFacade.getProducts(brandId, pageQuery)
            .let { AdminProductDto.PageResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

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

    @PutMapping("/{productId}")
    override fun updateProduct(
        @PathVariable productId: Long,
        @RequestBody request: AdminProductDto.UpdateRequest,
    ): ApiResponse<AdminProductDto.DetailResponse> {
        return adminProductFacade.updateProduct(
            productId = productId,
            name = request.name,
            description = request.description,
            price = request.price,
            stockQuantity = request.stockQuantity,
            brandId = request.brandId,
        )
            .let { AdminProductDto.DetailResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{productId}")
    override fun getProductDetail(
        @PathVariable productId: Long,
    ): ApiResponse<AdminProductDto.DetailResponse> {
        return adminProductFacade.getProductDetail(productId)
            .let { AdminProductDto.DetailResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @DeleteMapping("/{productId}")
    override fun deleteProduct(
        @PathVariable productId: Long,
    ): ApiResponse<Unit> {
        adminProductFacade.deleteProduct(productId)
        return ApiResponse.success(Unit)
    }
}

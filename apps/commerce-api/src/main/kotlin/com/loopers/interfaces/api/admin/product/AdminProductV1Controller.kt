package com.loopers.interfaces.api.admin.product

import com.loopers.application.product.AdminProductFacade
import com.loopers.application.product.ProductCommand
import com.loopers.domain.common.PageResult
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.config.auth.AdminAuthenticated
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@AdminAuthenticated
@RestController
@RequestMapping("/api-admin/v1/products")
class AdminProductV1Controller(
    private val adminProductFacade: AdminProductFacade,
) : AdminProductV1ApiSpec {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun createProduct(
        @RequestBody @Valid request: AdminProductV1Dto.CreateRequest,
    ): ApiResponse<AdminProductV1Dto.ProductResponse> {
        val command = ProductCommand.Create(
            brandId = request.brandId,
            name = request.name,
            description = request.description,
            price = request.price,
            stockQuantity = request.stockQuantity,
            imageUrl = request.imageUrl,
        )
        return adminProductFacade.createProduct(command)
            .let { AdminProductV1Dto.ProductResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping
    override fun getProducts(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) brandId: Long?,
    ): ApiResponse<PageResult<AdminProductV1Dto.ProductResponse>> {
        val result = adminProductFacade.getProducts(page, size, brandId)
        return PageResult(
            content = result.content.map { AdminProductV1Dto.ProductResponse.from(it) },
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        ).let { ApiResponse.success(it) }
    }

    @GetMapping("/{productId}")
    override fun getProduct(
        @PathVariable productId: Long,
    ): ApiResponse<AdminProductV1Dto.ProductResponse> {
        return adminProductFacade.getProduct(productId)
            .let { AdminProductV1Dto.ProductResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PutMapping("/{productId}")
    override fun updateProduct(
        @PathVariable productId: Long,
        @RequestBody @Valid request: AdminProductV1Dto.UpdateRequest,
    ): ApiResponse<AdminProductV1Dto.ProductResponse> {
        val command = ProductCommand.Update(
            name = request.name,
            description = request.description,
            price = request.price,
            stockQuantity = request.stockQuantity,
            imageUrl = request.imageUrl,
        )
        return adminProductFacade.updateProduct(productId, command)
            .let { AdminProductV1Dto.ProductResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @DeleteMapping("/{productId}")
    override fun deleteProduct(
        @PathVariable productId: Long,
    ): ApiResponse<Any> {
        adminProductFacade.deleteProduct(productId)
        return ApiResponse.success()
    }
}

package com.loopers.interfaces.api.admin.v1.product

import com.loopers.application.product.DeleteProductUseCase
import com.loopers.application.product.GetProductListUseCase
import com.loopers.application.product.GetProductUseCase
import com.loopers.application.product.RegisterProductUseCase
import com.loopers.application.product.UpdateProductUseCase
import com.loopers.interfaces.api.ApiResponse
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

@RestController
@RequestMapping("/api-admin/v1/products")
class AdminProductController(
    private val registerProductUseCase: RegisterProductUseCase,
    private val getProductUseCase: GetProductUseCase,
    private val getProductListUseCase: GetProductListUseCase,
    private val updateProductUseCase: UpdateProductUseCase,
    private val deleteProductUseCase: DeleteProductUseCase,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody request: CreateProductRequest,
    ): ApiResponse<CreateProductResponse> {
        val id = registerProductUseCase.register(request.toCommand())
        return ApiResponse.success(CreateProductResponse(id))
    }

    @GetMapping
    fun getAll(
        @RequestParam(required = false) brandId: Long?,
    ): ApiResponse<List<AdminProductResponse>> {
        val products = getProductListUseCase.getAll(brandId)
        return ApiResponse.success(products.map { AdminProductResponse.from(it) })
    }

    @GetMapping("/{id}")
    fun getById(
        @PathVariable id: Long,
    ): ApiResponse<AdminProductResponse> {
        val productInfo = getProductUseCase.getById(id)
        return ApiResponse.success(AdminProductResponse.from(productInfo))
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateProductRequest,
    ): ApiResponse<AdminProductResponse> {
        val productInfo = updateProductUseCase.update(id, request.toCommand())
        return ApiResponse.success(AdminProductResponse.from(productInfo))
    }

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: Long,
    ): ApiResponse<Nothing?> {
        deleteProductUseCase.delete(id)
        return ApiResponse.success(null)
    }
}

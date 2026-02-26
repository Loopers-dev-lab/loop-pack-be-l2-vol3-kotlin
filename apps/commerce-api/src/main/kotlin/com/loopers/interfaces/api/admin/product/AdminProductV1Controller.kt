package com.loopers.interfaces.api.admin.product

import com.loopers.application.product.DeleteProductUseCase
import com.loopers.application.product.GetProductListUseCase
import com.loopers.application.product.GetProductUseCase
import com.loopers.application.product.ProductCommand
import com.loopers.application.product.RegisterProductUseCase
import com.loopers.application.product.UpdateProductUseCase
import com.loopers.domain.product.ProductSortType
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.auth.AdminAuth
import com.loopers.support.PageResult
import com.loopers.support.constant.ApiPaths
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
@RequestMapping(ApiPaths.AdminProducts.BASE)
class AdminProductV1Controller(
    private val registerProductUseCase: RegisterProductUseCase,
    private val getProductUseCase: GetProductUseCase,
    private val getProductListUseCase: GetProductListUseCase,
    private val updateProductUseCase: UpdateProductUseCase,
    private val deleteProductUseCase: DeleteProductUseCase,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @AdminAuth adminAuth: Unit,
        @Valid @RequestBody request: AdminProductRegisterRequest,
    ): ApiResponse<AdminProductResponse> {
        val productInfo = registerProductUseCase.execute(request.toCommand())
        return ApiResponse.success(AdminProductResponse.from(productInfo))
    }

    @GetMapping
    fun getProducts(
        @AdminAuth adminAuth: Unit,
        @RequestParam(required = false) brandId: Long?,
        @RequestParam(defaultValue = "LATEST") sort: ProductSortType,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "false") includeDeleted: Boolean,
    ): ApiResponse<PageResult<AdminProductResponse>> {
        val result = getProductListUseCase.execute(
            ProductCommand.Search(
                brandId = brandId,
                sort = sort,
                page = page,
                size = size,
                includeDeleted = includeDeleted,
            ),
        )
        val responseContent = PageResult.of(
            content = result.content.map { AdminProductResponse.from(it) },
            page = result.page,
            size = result.size,
            totalElements = result.totalElements,
        )
        return ApiResponse.success(responseContent)
    }

    @GetMapping("/{productId}")
    fun getProduct(
        @AdminAuth adminAuth: Unit,
        @PathVariable productId: Long,
    ): ApiResponse<AdminProductResponse> {
        val product = getProductUseCase.execute(productId)
        return ApiResponse.success(AdminProductResponse.from(product))
    }

    @PutMapping("/{productId}")
    fun update(
        @AdminAuth adminAuth: Unit,
        @PathVariable productId: Long,
        @Valid @RequestBody request: AdminProductUpdateRequest,
    ): ApiResponse<AdminProductResponse> {
        val productInfo = updateProductUseCase.execute(request.toCommand(productId))
        return ApiResponse.success(AdminProductResponse.from(productInfo))
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @AdminAuth adminAuth: Unit,
        @PathVariable productId: Long,
    ): ApiResponse<Unit> {
        deleteProductUseCase.execute(productId)
        return ApiResponse.success(Unit)
    }
}

package com.loopers.interfaces.api.product

import com.loopers.application.auth.AuthUseCase
import com.loopers.application.product.ProductUseCase
import com.loopers.domain.product.ProductSortType
import com.loopers.interfaces.api.ApiResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/products")
class ProductV1Controller(
    private val authUseCase: AuthUseCase,
    private val productUseCase: ProductUseCase,
) : ProductV1ApiSpec {

    @PostMapping
    override fun register(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @Valid @RequestBody request: ProductV1Dto.RegisterRequest,
    ): ApiResponse<ProductV1Dto.DetailResponse> {
        authUseCase.authenticate(loginId, password)

        return productUseCase.register(request.toCommand())
            .let { ProductV1Dto.DetailResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{id}")
    override fun getById(
        @PathVariable id: Long,
    ): ApiResponse<ProductV1Dto.DetailResponse> {
        return productUseCase.getById(id)
            .let { ProductV1Dto.DetailResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping
    override fun getAll(
        @RequestParam(defaultValue = "LATEST") sortType: ProductSortType,
        @RequestParam(required = false) brandId: Long?,
    ): ApiResponse<List<ProductV1Dto.MainResponse>> {
        return productUseCase.getAll(sortType, brandId)
            .map { ProductV1Dto.MainResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PutMapping("/{id}")
    override fun changeInfo(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @PathVariable id: Long,
        @Valid @RequestBody request: ProductV1Dto.ChangeInfoRequest,
    ): ApiResponse<ProductV1Dto.DetailResponse> {
        authUseCase.authenticate(loginId, password)

        return productUseCase.changeInfo(id, request.toCommand())
            .let { ProductV1Dto.DetailResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @DeleteMapping("/{id}")
    override fun remove(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @PathVariable id: Long,
    ): ApiResponse<Any> {
        authUseCase.authenticate(loginId, password)

        productUseCase.remove(id)
        return ApiResponse.success()
    }
}

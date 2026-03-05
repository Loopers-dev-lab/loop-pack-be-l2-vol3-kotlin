package com.loopers.interfaces.api.user.product

import com.loopers.application.user.product.UserProductDetailUseCase
import com.loopers.application.user.product.UserProductListUseCase
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1/products")
@RestController
class UserProductV1Controller(
    private val detailUseCase: UserProductDetailUseCase,
    private val listUseCase: UserProductListUseCase,
) : UserProductV1ApiSpec {

    @GetMapping
    override fun getList(
        pageRequest: PageRequest,
        @RequestParam(required = false) brandId: Long?,
        @RequestParam(required = false) sort: String?,
    ): ApiResponse<PageResponse<UserProductV1Response.Summary>> {
        return listUseCase.getList(pageRequest, brandId, sort)
            .map { UserProductV1Response.Summary.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{productId}")
    override fun getDetail(
        @PathVariable productId: Long,
    ): ApiResponse<UserProductV1Response.Detail> {
        return detailUseCase.getDetail(productId)
            .let { UserProductV1Response.Detail.from(it) }
            .let { ApiResponse.success(it) }
    }
}

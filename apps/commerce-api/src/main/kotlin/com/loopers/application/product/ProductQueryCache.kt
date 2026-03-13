package com.loopers.application.product

import com.loopers.application.user.product.UserProductResult
import com.loopers.domain.product.Product
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse

interface ProductQueryCache {
    fun getDetail(productId: Long): UserProductResult.Detail?

    fun putDetail(detail: UserProductResult.Detail)

    fun evictDetail(productId: Long)

    fun evictDetails(productIds: Collection<Long>)

    fun getList(
        pageRequest: PageRequest,
        brandId: Long?,
        sort: Product.SortType?,
    ): PageResponse<UserProductResult.Summary>?

    fun putList(
        pageRequest: PageRequest,
        brandId: Long?,
        sort: Product.SortType?,
        response: PageResponse<UserProductResult.Summary>,
    )
}

package com.loopers.domain.product

import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse

interface ProductQueryRepository {
    fun getDetail(productId: Long): ProductQueryResult.Detail

    fun getList(
        pageRequest: PageRequest,
        brandId: Long?,
        sort: Product.SortType?,
    ): PageResponse<ProductQueryResult.Summary>
}

package com.loopers.application.product

import com.loopers.support.PageResult

interface ProductCacheStore {

    fun getDetail(productId: Long): ProductInfo?

    fun putDetail(productId: Long, productInfo: ProductInfo)

    fun evictDetail(productId: Long)

    fun getList(brandId: Long?, sort: String, page: Int, size: Int): PageResult<ProductInfo>?

    fun putList(brandId: Long?, sort: String, page: Int, size: Int, result: PageResult<ProductInfo>)

    fun evictAllLists()
}

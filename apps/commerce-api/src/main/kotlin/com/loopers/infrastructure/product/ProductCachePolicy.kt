package com.loopers.infrastructure.product

import com.loopers.support.common.PageQuery
import java.time.Duration

object ProductCachePolicy {
    private const val DETAIL_KEY_PREFIX = "product:detail:"
    private const val LIST_KEY_PREFIX = "product:list:"
    val DETAIL_TTL: Duration = Duration.ofSeconds(30)
    val LIST_TTL: Duration = Duration.ofMinutes(5)

    fun detailKey(productId: Long): String = "$DETAIL_KEY_PREFIX$productId"

    fun listKey(brandId: Long?, pageQuery: PageQuery): String {
        val brand = brandId ?: "all"
        val sort = "${pageQuery.sort.property}:${pageQuery.sort.direction}"
        return "${LIST_KEY_PREFIX}brand:$brand:$sort:${pageQuery.page}:${pageQuery.size}"
    }

    fun listKeyPattern(): String = "${LIST_KEY_PREFIX}*"
}

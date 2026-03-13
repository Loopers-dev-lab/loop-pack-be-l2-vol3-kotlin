package com.loopers.infrastructure.product

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.application.product.ProductQueryCache
import com.loopers.application.user.product.UserProductResult
import com.loopers.config.redis.RedisConfig
import com.loopers.domain.product.Product
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RedisProductQueryCache(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) : ProductQueryCache {

    override fun getDetail(productId: Long): UserProductResult.Detail? =
        readValue(detailKey(productId), DETAIL_TYPE_REFERENCE)

    override fun putDetail(detail: UserProductResult.Detail) {
        writeValue(detailKey(detail.id), detail, DETAIL_TTL)
    }

    override fun evictDetail(productId: Long) {
        runCatching { redisTemplate.delete(detailKey(productId)) }
    }

    override fun evictDetails(productIds: Collection<Long>) {
        if (productIds.isEmpty()) return
        runCatching { redisTemplate.delete(productIds.map(::detailKey).toSet()) }
    }

    override fun getList(
        pageRequest: PageRequest,
        brandId: Long?,
        sort: Product.SortType?,
    ): PageResponse<UserProductResult.Summary>? = readValue(listKey(pageRequest, brandId, sort), LIST_TYPE_REFERENCE)

    override fun putList(
        pageRequest: PageRequest,
        brandId: Long?,
        sort: Product.SortType?,
        response: PageResponse<UserProductResult.Summary>,
    ) {
        writeValue(listKey(pageRequest, brandId, sort), response, LIST_TTL)
    }

    private fun <T> readValue(
        key: String,
        typeReference: TypeReference<T>,
    ): T? {
        val cachedValue = runCatching { redisTemplate.opsForValue().get(key) }
            .getOrNull()
            ?: return null
        return runCatching { objectMapper.readValue(cachedValue, typeReference) }
            .getOrNull()
    }

    private fun writeValue(
        key: String,
        value: Any,
        ttl: Duration,
    ) {
        runCatching {
            redisTemplate.opsForValue().set(
                key,
                objectMapper.writeValueAsString(value),
                ttl,
            )
        }
    }

    private fun detailKey(productId: Long): String = "product:detail:v1:$productId"

    private fun listKey(
        pageRequest: PageRequest,
        brandId: Long?,
        sort: Product.SortType?,
    ): String = buildString {
        append("product:list:v1:brand:")
        append(brandId ?: "all")
        append(":sort:")
        append(sort?.name?.lowercase() ?: "latest")
        append(":page:")
        append(pageRequest.page)
        append(":size:")
        append(pageRequest.size)
    }

    companion object {
        private val DETAIL_TTL: Duration = Duration.ofMinutes(10)
        private val LIST_TTL: Duration = Duration.ofMinutes(2)
        private val DETAIL_TYPE_REFERENCE = object : TypeReference<UserProductResult.Detail>() {}
        private val LIST_TYPE_REFERENCE = object : TypeReference<PageResponse<UserProductResult.Summary>>() {}
    }
}

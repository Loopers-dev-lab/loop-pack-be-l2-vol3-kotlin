package com.loopers.infrastructure.product

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.config.redis.RedisConfig
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductQueryResult
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import org.slf4j.LoggerFactory
import java.time.Duration

@Component
class RedisProductQueryCache(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) {

    fun getDetail(productId: Long): ProductQueryResult.Detail? =
        readValue(detailKey(productId), DETAIL_TYPE_REFERENCE)

    fun putDetail(detail: ProductQueryResult.Detail) {
        writeValue(detailKey(detail.id), detail, DETAIL_TTL)
    }

    fun evictDetails(productIds: Collection<Long>) {
        if (productIds.isEmpty()) return
        val keys = productIds.map(::detailKey).toSet()
        runCatching { redisTemplate.delete(keys) }
            .onFailure { exception ->
                log.warn(
                    "Failed to evict product detail cache. productIds={}, keys={}",
                    productIds,
                    keys,
                    exception,
                )
            }
    }

    fun getListNamespaceVersion(brandId: Long?): Long = listNamespaceVersion(listScope(brandId))

    fun invalidateListsByBrandId(brandId: Long) {
        val brandScopeKey = listNamespaceKey(listScope(brandId))
        val allScopeKey = listNamespaceKey(ALL_LIST_SCOPE)
        runCatching {
            val valueOperations = redisTemplate.opsForValue()
            valueOperations.increment(brandScopeKey)
            valueOperations.increment(allScopeKey)
        }.onFailure { exception ->
            log.warn(
                "Failed to invalidate product list namespace. brandId={}, brandScopeKey={}, allScopeKey={}",
                brandId,
                brandScopeKey,
                allScopeKey,
                exception,
            )
        }
    }

    fun getList(
        pageRequest: PageRequest,
        brandId: Long?,
        sort: Product.SortType?,
        namespaceVersion: Long,
    ): PageResponse<ProductQueryResult.Summary>? =
        readValue(listKey(pageRequest, brandId, sort, namespaceVersion), LIST_TYPE_REFERENCE)

    fun putList(
        pageRequest: PageRequest,
        brandId: Long?,
        sort: Product.SortType?,
        namespaceVersion: Long,
        response: PageResponse<ProductQueryResult.Summary>,
    ) {
        writeValue(listKey(pageRequest, brandId, sort, namespaceVersion), response, LIST_TTL)
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
        }.onFailure { exception ->
            log.warn(
                "Failed to write product query cache. key={}, ttl={}",
                key,
                ttl,
                exception,
            )
        }
    }

    private fun detailKey(productId: Long): String = "product:detail:v1:$productId"

    private fun listNamespaceVersion(scope: String): Long =
        runCatching { redisTemplate.opsForValue().get(listNamespaceKey(scope)) }
            .getOrNull()
            ?.toLongOrNull()
            ?: DEFAULT_LIST_NAMESPACE_VERSION

    private fun listNamespaceKey(scope: String): String = "product:list:namespace:v1:brand:$scope"

    private fun listKey(
        pageRequest: PageRequest,
        brandId: Long?,
        sort: Product.SortType?,
        namespaceVersion: Long,
    ): String = buildString {
        append("product:list:v2:brand:")
        append(listScope(brandId))
        append(":version:")
        append(namespaceVersion)
        append(":sort:")
        append(sort?.name?.lowercase() ?: "latest")
        append(":page:")
        append(pageRequest.page)
        append(":size:")
        append(pageRequest.size)
    }

    private fun listScope(brandId: Long?): String = brandId?.toString() ?: ALL_LIST_SCOPE

    companion object {
        private val log = LoggerFactory.getLogger(RedisProductQueryCache::class.java)
        private const val ALL_LIST_SCOPE = "all"
        private const val DEFAULT_LIST_NAMESPACE_VERSION = 0L
        private val DETAIL_TTL: Duration = Duration.ofMinutes(10)
        private val LIST_TTL: Duration = Duration.ofMinutes(2)
        private val DETAIL_TYPE_REFERENCE = object : TypeReference<ProductQueryResult.Detail>() {}
        private val LIST_TYPE_REFERENCE = object : TypeReference<PageResponse<ProductQueryResult.Summary>>() {}
    }
}

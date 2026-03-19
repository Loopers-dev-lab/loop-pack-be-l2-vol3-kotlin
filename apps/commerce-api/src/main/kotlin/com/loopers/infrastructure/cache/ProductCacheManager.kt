package com.loopers.infrastructure.cache

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.loopers.domain.catalog.ProductCache
import com.loopers.domain.catalog.ProductInfo
import com.loopers.domain.catalog.ProductSortType
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import org.springframework.data.domain.SliceImpl
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import kotlin.random.Random

@Component
class ProductCacheManager(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    private val cacheProperties: CacheProperties,
    private val cacheMetrics: CacheMetrics,
    private val stampedeGuard: CacheStampedeGuard,
) : ProductCache {
    private val log = LoggerFactory.getLogger(javaClass)

    private val detailCaffeine: Cache<Long, String> = run {
        val builder = Caffeine.newBuilder()
            .maximumSize(cacheProperties.caffeine.maxSize)
            .expireAfterWrite(Duration.ofSeconds(cacheProperties.caffeine.expireSeconds))

        if (cacheProperties.mode == CacheMode.LAYERED) {
            builder.refreshAfterWrite(Duration.ofSeconds(cacheProperties.caffeine.refreshSeconds))
                .build { key -> loadDetailFromRedis(key) }
        } else {
            builder.build()
        }
    }

    private val listCaffeine: Cache<String, String> = run {
        val builder = Caffeine.newBuilder()
            .maximumSize(cacheProperties.caffeine.maxSize)
            .expireAfterWrite(Duration.ofSeconds(cacheProperties.caffeine.expireSeconds))

        if (cacheProperties.mode == CacheMode.LAYERED) {
            builder.refreshAfterWrite(Duration.ofSeconds(cacheProperties.caffeine.refreshSeconds))
                .build { key -> loadListFromRedis(key) }
        } else {
            builder.build()
        }
    }

    override fun getProduct(id: Long, loader: () -> ProductInfo): ProductInfo {
        if (cacheProperties.mode == CacheMode.DB_ONLY) {
            return loader()
        }

        return when (cacheProperties.mode) {
            CacheMode.CAFFEINE_ONLY -> getFromCaffeineDetail(id, loader)
            CacheMode.REDIS_ONLY -> getFromRedisDetail(id, loader)
            CacheMode.LAYERED -> getFromLayeredDetail(id, loader)
            else -> loader()
        }
    }

    override fun searchProducts(
        sortType: ProductSortType,
        brandId: Long?,
        page: Int,
        size: Int,
        loader: () -> Slice<ProductInfo>,
    ): Slice<ProductInfo> {
        if (cacheProperties.mode == CacheMode.DB_ONLY) {
            return loader()
        }

        val cacheKey = listCacheKey(sortType, brandId, page, size)

        return when (cacheProperties.mode) {
            CacheMode.CAFFEINE_ONLY -> getFromCaffeineList(cacheKey, page, size, loader)
            CacheMode.REDIS_ONLY -> getFromRedisList(cacheKey, page, size, loader)
            CacheMode.LAYERED -> getFromLayeredList(cacheKey, page, size, loader)
            else -> loader()
        }
    }

    override fun evictProduct(productId: Long) {
        detailCaffeine.invalidate(productId)
        if (requiresRedis()) {
            redisTemplate.delete(detailCacheKey(productId))
        }
    }

    override fun evictProductList() {
        listCaffeine.invalidateAll()
        if (requiresRedis()) {
            deleteRedisKeysByPattern("product:list:*")
        }
    }

    override fun evictPopularList() {
        listCaffeine.invalidateAll()
        if (requiresRedis()) {
            deleteRedisKeysByPattern("product:list:POPULAR:*")
        }
    }

    private fun requiresRedis(): Boolean =
        cacheProperties.mode == CacheMode.REDIS_ONLY || cacheProperties.mode == CacheMode.LAYERED

    // ── L1: Caffeine Only ──

    private fun getFromCaffeineDetail(id: Long, loader: () -> ProductInfo): ProductInfo {
        val cached = detailCaffeine.getIfPresent(id)
        if (cached != null) {
            cacheMetrics.recordDetailHit("caffeine")
            return deserializeProductInfo(cached)
        }
        cacheMetrics.recordDetailMiss("caffeine")
        val info = loader()
        detailCaffeine.put(id, serializeProductInfo(info))
        return info
    }

    private fun getFromCaffeineList(
        cacheKey: String,
        page: Int,
        size: Int,
        loader: () -> Slice<ProductInfo>,
    ): Slice<ProductInfo> {
        val cached = listCaffeine.getIfPresent(cacheKey)
        if (cached != null) {
            cacheMetrics.recordListHit("caffeine")
            return deserializeSlice(cached, page, size)
        }
        cacheMetrics.recordListMiss("caffeine")
        val slice = loader()
        listCaffeine.put(cacheKey, serializeSlice(slice))
        return slice
    }

    // ── L2: Redis Only ──

    private fun getFromRedisDetail(id: Long, loader: () -> ProductInfo): ProductInfo {
        val redisKey = detailCacheKey(id)
        val cached = redisTemplate.opsForValue().get(redisKey)
        if (cached != null) {
            cacheMetrics.recordDetailHit("redis")
            return deserializeProductInfo(cached)
        }
        cacheMetrics.recordDetailMiss("redis")
        val info = loader()
        redisTemplate.opsForValue().set(redisKey, serializeProductInfo(info), detailTtl())
        return info
    }

    private fun getFromRedisList(
        cacheKey: String,
        page: Int,
        size: Int,
        loader: () -> Slice<ProductInfo>,
    ): Slice<ProductInfo> {
        val cached = redisTemplate.opsForValue().get(cacheKey)
        if (cached != null) {
            cacheMetrics.recordListHit("redis")
            return deserializeSlice(cached, page, size)
        }
        cacheMetrics.recordListMiss("redis")
        val slice = loader()
        redisTemplate.opsForValue().set(cacheKey, serializeSlice(slice), listTtl())
        return slice
    }

    // ── L1+L2+L3: Layered ──

    private fun getFromLayeredDetail(id: Long, loader: () -> ProductInfo): ProductInfo {
        // L1: Caffeine
        val l1 = detailCaffeine.getIfPresent(id)
        if (l1 != null) {
            cacheMetrics.recordDetailHit("caffeine")
            return deserializeProductInfo(l1)
        }
        cacheMetrics.recordDetailMiss("caffeine")

        // L2: Redis
        val redisKey = detailCacheKey(id)
        val l2 = redisTemplate.opsForValue().get(redisKey)
        if (l2 != null) {
            cacheMetrics.recordDetailHit("redis")
            detailCaffeine.put(id, l2)
            return deserializeProductInfo(l2)
        }
        cacheMetrics.recordDetailMiss("redis")

        // L3: DB (with stampede guard)
        val info = stampedeGuard.executeWithMutex(
            cacheKey = redisKey,
            loader = loader,
            cacheWriter = { value ->
                val serialized = serializeProductInfo(value)
                redisTemplate.opsForValue().set(redisKey, serialized, detailTtl())
                detailCaffeine.put(id, serialized)
            },
            cacheReader = {
                val cached = redisTemplate.opsForValue().get(redisKey)
                if (cached != null) {
                    detailCaffeine.put(id, cached)
                    deserializeProductInfo(cached)
                } else {
                    null
                }
            },
        )
        return info
    }

    private fun getFromLayeredList(
        cacheKey: String,
        page: Int,
        size: Int,
        loader: () -> Slice<ProductInfo>,
    ): Slice<ProductInfo> {
        // L1: Caffeine
        val l1 = listCaffeine.getIfPresent(cacheKey)
        if (l1 != null) {
            cacheMetrics.recordListHit("caffeine")
            return deserializeSlice(l1, page, size)
        }
        cacheMetrics.recordListMiss("caffeine")

        // L2: Redis
        val l2 = redisTemplate.opsForValue().get(cacheKey)
        if (l2 != null) {
            cacheMetrics.recordListHit("redis")
            listCaffeine.put(cacheKey, l2)
            return deserializeSlice(l2, page, size)
        }
        cacheMetrics.recordListMiss("redis")

        // L3: DB (with stampede guard)
        val slice = stampedeGuard.executeWithMutex(
            cacheKey = cacheKey,
            loader = loader,
            cacheWriter = { value ->
                val serialized = serializeSlice(value)
                redisTemplate.opsForValue().set(cacheKey, serialized, listTtl())
                listCaffeine.put(cacheKey, serialized)
            },
            cacheReader = {
                val cached = redisTemplate.opsForValue().get(cacheKey)
                if (cached != null) {
                    listCaffeine.put(cacheKey, cached)
                    deserializeSlice(cached, page, size)
                } else {
                    null
                }
            },
        )
        return slice
    }

    // ── Caffeine Loader (for refresh) ──

    private fun loadDetailFromRedis(id: Long): String? {
        return redisTemplate.opsForValue().get(detailCacheKey(id))
    }

    private fun loadListFromRedis(key: String): String? {
        return redisTemplate.opsForValue().get(key)
    }

    // ── Serialization ──

    private fun serializeProductInfo(info: ProductInfo): String {
        return objectMapper.writeValueAsString(info)
    }

    private fun deserializeProductInfo(json: String): ProductInfo {
        return objectMapper.readValue(json)
    }

    private fun serializeSlice(slice: Slice<ProductInfo>): String {
        val wrapper = SliceCacheWrapper(
            content = slice.content,
            hasNext = slice.hasNext(),
        )
        return objectMapper.writeValueAsString(wrapper)
    }

    private fun deserializeSlice(json: String, page: Int, size: Int): Slice<ProductInfo> {
        val wrapper: SliceCacheWrapper = objectMapper.readValue(json)
        return SliceImpl(wrapper.content, PageRequest.of(page, size), wrapper.hasNext)
    }

    // ── Key/TTL ──

    private fun detailCacheKey(productId: Long): String = "product:detail:$productId"

    private fun listCacheKey(sort: ProductSortType, brandId: Long?, page: Int, size: Int): String {
        val brandPart = brandId?.toString() ?: "all"
        return "product:list:${sort.name}:$brandPart:p$page:s$size"
    }

    private fun detailTtl(): Duration = Duration.ofMinutes(5).plusSeconds(Random.nextLong(0, 31))

    private fun listTtl(): Duration = Duration.ofMinutes(1).plusSeconds(Random.nextLong(0, 11))

    private fun deleteRedisKeysByPattern(pattern: String) {
        val keys = redisTemplate.keys(pattern)
        if (!keys.isNullOrEmpty()) {
            redisTemplate.delete(keys)
        }
    }

    private data class SliceCacheWrapper(
        val content: List<ProductInfo>,
        val hasNext: Boolean,
    )
}

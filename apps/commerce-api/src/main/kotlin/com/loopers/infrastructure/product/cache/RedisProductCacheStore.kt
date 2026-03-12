package com.loopers.infrastructure.product.cache

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.application.product.ProductCacheStore
import com.loopers.application.product.ProductInfo
import com.loopers.config.redis.RedisConfig
import com.loopers.domain.product.ProductSortType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Duration

@Component
class RedisProductCacheStore(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) : ProductCacheStore {
    companion object {
        private val DETAIL_TTL: Duration = Duration.ofMinutes(10)
        private val LIST_TTL: Duration = Duration.ofMinutes(3)

        private const val DETAIL_VERSION_KEY = "products:detail:version"
        private const val LIST_GLOBAL_VERSION_KEY = "products:list:version:global"
        private const val LIST_BRAND_VERSION_KEY_PREFIX = "products:list:version:brand:"
    }

    private val log = LoggerFactory.getLogger(RedisProductCacheStore::class.java)

    override fun getDetail(productId: Long, loader: () -> ProductInfo.Detail): ProductInfo.Detail {
        val version = getVersion(DETAIL_VERSION_KEY)
        val cacheKey = detailCacheKey(productId, version)

        read(cacheKey, ProductInfo.Detail::class.java)?.let { return it }

        return loader().also { detail ->
            write(cacheKey, detail, DETAIL_TTL)
        }
    }

    override fun getList(
        sortType: ProductSortType,
        brandId: Long?,
        loader: () -> List<ProductInfo.Main>,
    ): List<ProductInfo.Main> {
        val cacheKey = listCacheKey(sortType, brandId)

        read(cacheKey, object : TypeReference<List<ProductInfo.Main>>() {})?.let { return it }

        return loader().also { products ->
            write(cacheKey, products, LIST_TTL)
        }
    }

    override fun evictDetail(productId: Long) {
        runAfterCommit {
            runCatching {
                redisTemplate.delete(detailCacheKey(productId, getVersion(DETAIL_VERSION_KEY)))
            }.onFailure { ex ->
                log.warn("상품 상세 캐시 무효화에 실패했습니다. productId={}", productId, ex)
            }
        }
    }

    override fun evictAllDetails() {
        runAfterCommit {
            incrementVersion(DETAIL_VERSION_KEY)
        }
    }

    override fun evictList(brandId: Long?) {
        runAfterCommit {
            val versionKey = brandId?.let(::brandListVersionKey) ?: LIST_GLOBAL_VERSION_KEY
            incrementVersion(versionKey)
        }
    }

    private fun detailCacheKey(productId: Long, version: Long): String {
        return "products:detail:v:$version:product:$productId"
    }

    private fun listCacheKey(sortType: ProductSortType, brandId: Long?): String {
        val globalVersion = getVersion(LIST_GLOBAL_VERSION_KEY)
        val brandVersion = brandId?.let { getVersion(brandListVersionKey(it)) } ?: 0L
        val brandScope = brandId ?: "all"
        return "products:list:gv:$globalVersion:bv:$brandVersion:sort:${sortType.name}:brand:$brandScope"
    }

    private fun brandListVersionKey(brandId: Long): String {
        return "$LIST_BRAND_VERSION_KEY_PREFIX$brandId"
    }

    private fun getVersion(key: String): Long {
        return runCatching {
            redisTemplate.opsForValue().get(key)?.toLong() ?: 0L
        }.getOrElse { ex ->
            log.warn("캐시 버전 조회에 실패했습니다. key={}", key, ex)
            0L
        }
    }

    private fun incrementVersion(key: String) {
        runCatching {
            redisTemplate.opsForValue().increment(key)
        }.onFailure { ex ->
            log.warn("캐시 버전 증가에 실패했습니다. key={}", key, ex)
        }
    }

    private fun <T> read(cacheKey: String, clazz: Class<T>): T? {
        return runCatching {
            redisTemplate.opsForValue().get(cacheKey)
                ?.let { objectMapper.readValue(it, clazz) }
        }.getOrElse { ex ->
            log.warn("캐시 조회에 실패했습니다. key={}", cacheKey, ex)
            null
        }
    }

    private fun <T> read(cacheKey: String, typeReference: TypeReference<T>): T? {
        return runCatching {
            redisTemplate.opsForValue().get(cacheKey)
                ?.let { objectMapper.readValue(it, typeReference) }
        }.getOrElse { ex ->
            log.warn("캐시 조회에 실패했습니다. key={}", cacheKey, ex)
            null
        }
    }

    private fun write(cacheKey: String, value: Any, ttl: Duration) {
        runCatching {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(value), ttl)
        }.onFailure { ex ->
            log.warn("캐시 저장에 실패했습니다. key={}", cacheKey, ex)
        }
    }

    private fun runAfterCommit(action: () -> Unit) {
        if (TransactionSynchronizationManager.isSynchronizationActive() &&
            TransactionSynchronizationManager.isActualTransactionActive()
        ) {
            TransactionSynchronizationManager.registerSynchronization(
                object : TransactionSynchronization {
                    override fun afterCommit() {
                        action()
                    }
                },
            )
            return
        }

        action()
    }
}

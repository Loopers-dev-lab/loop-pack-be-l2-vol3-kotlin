package com.loopers.infrastructure.catalog.product

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.config.redis.RedisConfig.Companion.REDIS_TEMPLATE_MASTER
import com.loopers.domain.catalog.product.model.Product
import com.loopers.domain.catalog.product.repository.ProductCacheRepository
import com.loopers.domain.catalog.product.vo.Stock
import com.loopers.domain.common.vo.BrandId
import com.loopers.domain.common.vo.Money
import com.loopers.domain.common.vo.ProductId
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ScanOptions
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.Duration
import java.time.ZonedDateTime

@Repository
class ProductCacheRepositoryImpl(
    @param:Qualifier(REDIS_TEMPLATE_MASTER)
    private val redisTemplateMaster: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) : ProductCacheRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val DETAIL_KEY_PREFIX = "product:detail:"
        private const val LIST_KEY_PREFIX = "product:list::"
        private val DETAIL_BASE_TTL: Duration = Duration.ofHours(1)
        private const val DETAIL_JITTER_SECONDS = 60L
    }

    private fun detailTtl(): Duration =
        DETAIL_BASE_TTL.plusSeconds(kotlin.random.Random.nextLong(-DETAIL_JITTER_SECONDS, DETAIL_JITTER_SECONDS + 1))
            .let { if (it.isNegative || it.isZero) Duration.ofSeconds(1) else it }

    private fun detailKey(productId: ProductId) = "$DETAIL_KEY_PREFIX${productId.value}"

    override fun findProductDetail(productId: ProductId): Product? {
        val key = detailKey(productId)
        val json = try {
            redisTemplateMaster.opsForValue().get(key) ?: return null
        } catch (e: Exception) {
            log.warn("Redis 캐시 조회 실패 [key={}]: {}", key, e.message)
            return null
        }
        return try {
            objectMapper.readValue(json, ProductCacheDto::class.java).toDomain()
        } catch (e: Exception) {
            log.warn("Redis 캐시 역직렬화 실패 [key={}]: {}", key, e.message)
            try {
                redisTemplateMaster.delete(key)
                log.warn("손상된 Redis 캐시 키 삭제 완료 [key={}]", key)
            } catch (deleteException: Exception) {
                log.warn("손상된 Redis 캐시 키 삭제 실패 [key={}]: {}", key, deleteException.message)
            }
            null
        }
    }

    override fun saveProductDetail(product: Product) {
        try {
            val key = detailKey(product.id)
            val json = objectMapper.writeValueAsString(ProductCacheDto.fromDomain(product))
            redisTemplateMaster.opsForValue().set(key, json, detailTtl())
        } catch (e: Exception) {
            log.warn("Redis 캐시 저장 실패 [productId={}]: {}", product.id.value, e.message)
        }
    }

    override fun saveProductDetailIfAbsent(product: Product) {
        try {
            val key = detailKey(product.id)
            val json = objectMapper.writeValueAsString(ProductCacheDto.fromDomain(product))
            redisTemplateMaster.opsForValue().setIfAbsent(key, json, detailTtl())
        } catch (e: Exception) {
            log.warn("Redis 캐시 저장(ifAbsent) 실패 [productId={}]: {}", product.id.value, e.message)
        }
    }

    override fun evictProductDetail(productId: ProductId) {
        try {
            redisTemplateMaster.delete(detailKey(productId))
        } catch (e: Exception) {
            log.warn("Redis 캐시 삭제 실패 [productId={}]: {}", productId.value, e.message)
        }
    }

    override fun evictProductList(brandId: BrandId?) {
        try {
            if (brandId != null) {
                scanAndDelete("$LIST_KEY_PREFIX${brandId.value}:*")
                scanAndDelete("${LIST_KEY_PREFIX}all:*")
            } else {
                scanAndDelete("$LIST_KEY_PREFIX*")
            }
        } catch (e: Exception) {
            log.warn("Redis 목록 캐시 삭제 실패 [brandId={}]: {}", brandId?.value, e.message)
        }
    }

    private fun scanAndDelete(pattern: String) {
        val options = ScanOptions.scanOptions().match(pattern).count(100).build()
        redisTemplateMaster.execute<Unit> { connection ->
            val batch = mutableListOf<ByteArray>()
            connection.scan(options).use { cursor ->
                while (cursor.hasNext()) {
                    batch.add(cursor.next())
                    if (batch.size >= 1000) {
                        connection.keyCommands().del(*batch.toTypedArray())
                        batch.clear()
                    }
                }
            }
            if (batch.isNotEmpty()) {
                connection.keyCommands().del(*batch.toTypedArray())
            }
        }
    }

    private data class ProductCacheDto(
        val id: Long,
        val refBrandId: Long,
        val name: String,
        val price: BigDecimal,
        val stock: Int,
        val status: String,
        val likeCount: Int,
        val deletedAt: String?,
    ) {
        companion object {
            fun fromDomain(product: Product): ProductCacheDto = ProductCacheDto(
                id = product.id.value,
                refBrandId = product.refBrandId.value,
                name = product.name,
                price = product.price.value,
                stock = product.stock.value,
                status = product.status.name,
                likeCount = product.likeCount,
                deletedAt = product.deletedAt?.toString(),
            )
        }

        fun toDomain(): Product = Product(
            id = ProductId(id),
            refBrandId = BrandId(refBrandId),
            name = name,
            price = Money(price),
            stock = Stock(stock),
            status = Product.ProductStatus.valueOf(status),
            likeCount = likeCount,
            deletedAt = deletedAt?.let { ZonedDateTime.parse(it) },
        )
    }
}

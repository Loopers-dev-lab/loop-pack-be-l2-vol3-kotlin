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
    private val redisTemplate: RedisTemplate<String, String>,
    @param:Qualifier(REDIS_TEMPLATE_MASTER)
    private val redisTemplateMaster: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) : ProductCacheRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val DETAIL_KEY_PREFIX = "product:detail:"
        private const val LIST_KEY_PREFIX = "product:list:"
        private val DETAIL_TTL: Duration = Duration.ofHours(1)
    }

    private fun detailKey(productId: ProductId) = "$DETAIL_KEY_PREFIX${productId.value}"

    override fun findProductDetail(productId: ProductId): Product? {
        return try {
            val key = detailKey(productId)
            val json = redisTemplate.opsForValue().get(key) ?: return null
            val dto = objectMapper.readValue(json, ProductCacheDto::class.java)
            dto.toDomain()
        } catch (e: Exception) {
            log.warn("Redis 캐시 조회 실패 [key={}]: {}", detailKey(productId), e.message)
            null
        }
    }

    override fun saveProductDetail(product: Product) {
        try {
            val key = detailKey(product.id)
            val json = objectMapper.writeValueAsString(ProductCacheDto.fromDomain(product))
            redisTemplateMaster.opsForValue().set(key, json, DETAIL_TTL)
        } catch (e: Exception) {
            log.warn("Redis 캐시 저장 실패 [productId={}]: {}", product.id.value, e.message)
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
        val keys = redisTemplateMaster.execute<Set<String>> { connection ->
            val result = mutableSetOf<String>()
            connection.scan(options).use { cursor ->
                while (cursor.hasNext()) {
                    result.add(String(cursor.next()))
                }
            }
            result
        } ?: emptySet()
        if (keys.isNotEmpty()) {
            redisTemplateMaster.delete(keys)
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

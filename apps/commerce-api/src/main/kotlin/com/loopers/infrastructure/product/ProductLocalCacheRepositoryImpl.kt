package com.loopers.infrastructure.product

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.loopers.application.product.ProductDetailInfo
import com.loopers.application.product.ProductInfo
import com.loopers.application.product.ProductLocalCacheRepository
import com.loopers.support.common.PageQuery
import com.loopers.support.common.PageResult
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class ProductLocalCacheRepositoryImpl : ProductLocalCacheRepository {

    companion object {
        private val DETAIL_EXPIRE: Duration = Duration.ofSeconds(60)
        private const val DETAIL_MAX_SIZE: Long = 1_000

        private val LIST_EXPIRE: Duration = Duration.ofMinutes(5)
        private const val LIST_MAX_SIZE: Long = 200
    }

    private val detailCache: Cache<Long, ProductDetailInfo> = Caffeine.newBuilder()
        .maximumSize(DETAIL_MAX_SIZE)
        .expireAfterWrite(DETAIL_EXPIRE)
        .build()

    private val listCache: Cache<ProductListCacheKey, PageResult<ProductInfo>> = Caffeine.newBuilder()
        .maximumSize(LIST_MAX_SIZE)
        .expireAfterWrite(LIST_EXPIRE)
        .build()

    override fun getOrLoadProduct(productId: Long, loader: () -> ProductDetailInfo): ProductDetailInfo {
        return detailCache.get(productId) { loader() }!!
    }

    override fun getOrLoadProducts(
        brandId: Long?,
        pageQuery: PageQuery,
        loader: () -> PageResult<ProductInfo>,
    ): PageResult<ProductInfo> {
        return listCache.get(ProductListCacheKey(brandId, pageQuery)) { loader() }!!
    }

    override fun evictProduct(productId: Long) {
        detailCache.invalidate(productId)
    }

    override fun evictAllProducts() {
        listCache.invalidateAll()
    }

    override fun evictAll() {
        detailCache.invalidateAll()
        listCache.invalidateAll()
    }

    private data class ProductListCacheKey(
        val brandId: Long?,
        val pageQuery: PageQuery,
    )
}

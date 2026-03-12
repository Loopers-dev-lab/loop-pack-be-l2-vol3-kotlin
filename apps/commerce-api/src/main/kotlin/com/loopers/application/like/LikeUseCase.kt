package com.loopers.application.like

import com.loopers.application.product.ProductCacheStore
import com.loopers.domain.brand.BrandReader
import com.loopers.domain.like.LikeReader
import com.loopers.domain.like.LikeRegister
import com.loopers.domain.like.LikeRemover
import com.loopers.domain.product.ProductLikeCountUpdater
import com.loopers.domain.product.ProductReader
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class LikeUseCase(
    private val likeRegister: LikeRegister,
    private val likeRemover: LikeRemover,
    private val likeReader: LikeReader,
    private val productLikeCountUpdater: ProductLikeCountUpdater,
    private val productReader: ProductReader,
    private val brandReader: BrandReader,
    private val productCacheStore: ProductCacheStore,
) {

    @Transactional
    fun register(memberId: Long, productId: Long): LikeInfo.Registered {
        val product = productReader.getSellingById(productId)
        val like = likeRegister.register(memberId, productId)
        productLikeCountUpdater.increase(productId)
        productCacheStore.evictDetail(productId)
        productCacheStore.evictList()
        productCacheStore.evictList(product.brandId)
        return LikeInfo.Registered.from(like)
    }

    @Transactional
    fun remove(likeId: Long, memberId: Long) {
        val like = likeRemover.remove(likeId, memberId)
        val product = productReader.getById(like.productId)
        productLikeCountUpdater.decrease(like.productId)
        productCacheStore.evictDetail(like.productId)
        productCacheStore.evictList()
        productCacheStore.evictList(product.brandId)
    }

    @Transactional(readOnly = true)
    fun getMyLikes(memberId: Long): List<LikeInfo.Detail> {
        val likes = likeReader.getAllByMemberId(memberId)
        if (likes.isEmpty()) return emptyList()

        val productIds = likes.map { it.productId }
        val productMap = productReader.getAllByIds(productIds).associateBy { it.id }

        val brandIds = productMap.values.map { it.brandId }.distinct()
        val brandMap = brandReader.getAllByIds(brandIds).associateBy { it.id }

        return likes.mapNotNull { like ->
            val product = productMap[like.productId] ?: return@mapNotNull null
            val brand = brandMap[product.brandId]
            LikeInfo.Detail.from(like, product, brand)
        }
    }
}

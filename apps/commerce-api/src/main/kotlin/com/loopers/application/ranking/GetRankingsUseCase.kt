package com.loopers.application.ranking

import com.loopers.application.product.ProductInfo
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductSearchCondition
import com.loopers.domain.product.ProductSortType
import com.loopers.domain.product.ProductStockRepository
import com.loopers.support.PageResult
import org.slf4j.LoggerFactory
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate

@Component
class GetRankingsUseCase(
    private val rankingStore: RankingStore,
    private val rankingSnapshotStore: RankingSnapshotStore,
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
    private val productStockRepository: ProductStockRepository,
    transactionManager: PlatformTransactionManager,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val readOnlyTx = TransactionTemplate(transactionManager).apply {
        isReadOnly = true
    }

    fun execute(date: LocalDate, page: Int, size: Int): PageResult<RankingProductInfo> {
        return try {
            getRankingsFromRedis(date, page, size)
        } catch (e: RedisConnectionFailureException) {
            log.warn("Redis 장애 — 스냅샷 fallback [date={}]", date, e)
            fallback(date, page, size)
        } catch (e: Exception) {
            log.warn("랭킹 조회 실패 — 스냅샷 fallback [date={}]", date, e)
            fallback(date, page, size)
        }
    }

    private fun getRankingsFromRedis(date: LocalDate, page: Int, size: Int): PageResult<RankingProductInfo> {
        val offset = (page * size).toLong()
        val entries = rankingStore.getTopRankings(date, offset, size.toLong())

        if (entries.isEmpty()) {
            return PageResult.of(emptyList(), page, size, 0L)
        }

        val totalElements = rankingStore.size(date)
        val productIds = entries.map { it.productId }
        val productInfoMap = loadProductInfos(productIds)

        val rankings = entries.mapIndexed { index, entry ->
            val info = productInfoMap[entry.productId]
            RankingProductInfo(
                rank = (offset + index + 1).toInt(),
                score = entry.score,
                productId = entry.productId,
                productName = info?.name ?: "",
                price = info?.price ?: 0,
                brandName = info?.brandName ?: "",
                imageUrl = info?.imageUrl ?: "",
                likeCount = info?.likeCount ?: 0,
                available = info?.available ?: false,
            )
        }

        return PageResult.of(rankings, page, size, totalElements)
    }

    private fun fallback(date: LocalDate, page: Int, size: Int): PageResult<RankingProductInfo> {
        val snapshot = rankingSnapshotStore.get(date)
        if (snapshot != null) {
            log.info("스냅샷 fallback 사용 [date={}]", date)
            return snapshot
        }

        val yesterdaySnapshot = rankingSnapshotStore.get(date.minusDays(1))
        if (yesterdaySnapshot != null) {
            log.info("전날 스냅샷 fallback 사용 [date={}]", date.minusDays(1))
            return yesterdaySnapshot
        }

        log.info("스냅샷 없음 — DB 기본 랭킹 fallback (likeCount 기준)")
        return getDefaultRankings(page, size)
    }

    private fun getDefaultRankings(page: Int, size: Int): PageResult<RankingProductInfo> {
        return readOnlyTx.execute {
            val condition = ProductSearchCondition(sort = ProductSortType.LIKE_COUNT, page = page, size = size)
            val productPage = productRepository.findAllByCondition(condition)

            val brandIds = productPage.content.map { it.brandId }.distinct()
            val brandMap = brandRepository.findAllByIds(brandIds).associateBy { it.id }

            val rankings = productPage.content.mapIndexed { index, product ->
                RankingProductInfo(
                    rank = (page * size) + index + 1,
                    score = 0.0,
                    productId = product.id,
                    productName = product.name,
                    price = product.price.amount,
                    brandName = brandMap[product.brandId]?.name ?: "",
                    imageUrl = product.imageUrl,
                    likeCount = product.likeCount,
                    available = !product.isDeleted(),
                )
            }

            PageResult.of(rankings, page, size, productPage.totalElements)
        } ?: PageResult.of(emptyList(), page, size, 0L)
    }

    private fun loadProductInfos(productIds: List<Long>): Map<Long, ProductInfo> {
        return readOnlyTx.execute { loadFromDb(productIds) } ?: emptyMap()
    }

    private fun loadFromDb(productIds: List<Long>): Map<Long, ProductInfo> {
        val products = productRepository.findAllActiveByIds(productIds)
        if (products.isEmpty()) return emptyMap()

        val brandIds = products.map { it.brandId }.distinct()
        val brandMap = brandRepository.findAllByIds(brandIds).associateBy { it.id }

        val stockMap = productStockRepository.findAllByProductIds(productIds)
            .associate { it.productId to it.stock.quantity }

        return products.associate { product ->
            val brandName = brandMap[product.brandId]?.name ?: ""
            val stock = stockMap[product.id] ?: 0
            product.id to ProductInfo.from(product, brandName, stock)
        }
    }
}

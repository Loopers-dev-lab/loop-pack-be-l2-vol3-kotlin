package com.loopers.application.catalog.product

import com.loopers.application.catalog.CatalogInfo
import com.loopers.application.catalog.ProductDetail
import com.loopers.application.event.CatalogEvent
import com.loopers.domain.catalog.brand.repository.BrandRepository
import com.loopers.domain.catalog.product.repository.ProductCacheRepository
import com.loopers.domain.catalog.product.repository.ProductRepository
import com.loopers.domain.common.vo.ProductId
import com.loopers.domain.ranking.repository.RankingRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.LocalDate

@Component
class GetProductUseCase(
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
    private val productCacheRepository: ProductCacheRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val rankingRepository: RankingRepository,
    private val clock: Clock,
    transactionManager: PlatformTransactionManager,
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val readOnlyTxTemplate = TransactionTemplate(transactionManager).apply {
        isReadOnly = true
    }

    fun execute(productId: Long, userId: Long? = null): CatalogInfo {
        val detail = readOnlyTxTemplate.execute {
            val id = ProductId(productId)
            var cached = true
            val product = productCacheRepository.findProductDetail(id)
                ?: run {
                    cached = false
                    productRepository.findById(id)
                }
                ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
            if (product.isDeleted() || !product.isActive()) {
                throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
            }
            val brand = brandRepository.findById(product.refBrandId)
                ?: throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다.")
            if (brand.isDeleted()) {
                throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다.")
            }
            if (!cached) {
                productCacheRepository.saveProductDetail(product)
            }
            eventPublisher.publishEvent(CatalogEvent.ProductViewed(productId = productId, userId = userId))
            ProductDetail(product = product, brand = brand)
        }!!
        val rank = try {
            rankingRepository.getRank(LocalDate.now(clock), productId)
        } catch (e: Exception) {
            log.warn("랭킹 조회 실패 — productId={}, cause={}", productId, e.message)
            null
        }
        return CatalogInfo.from(detail, rank)
    }
}

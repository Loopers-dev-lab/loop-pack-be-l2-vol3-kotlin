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
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
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
) {

    @Transactional(readOnly = true)
    fun execute(productId: Long, userId: Long? = null): CatalogInfo {
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
        val detail = ProductDetail(product = product, brand = brand)
        eventPublisher.publishEvent(CatalogEvent.ProductViewed(productId = productId, userId = userId))
        val rank = rankingRepository.getRank(LocalDate.now(clock), productId)
        return CatalogInfo.from(detail, rank)
    }
}

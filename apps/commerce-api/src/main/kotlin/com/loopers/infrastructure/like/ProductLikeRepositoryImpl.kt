package com.loopers.infrastructure.like

import com.loopers.domain.like.ProductLike
import com.loopers.domain.like.ProductLikeRepository
import com.loopers.support.page.PageResponse
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository
import com.loopers.support.page.PageRequest as DomainPageRequest
import org.springframework.data.domain.PageRequest as SpringPageRequest

@Repository
class ProductLikeRepositoryImpl(
    private val productLikeJpaRepository: ProductLikeJpaRepository,
    private val productLikeMapper: ProductLikeMapper,
) : ProductLikeRepository {
    override fun save(productLike: ProductLike) {
        try {
            productLikeJpaRepository.saveAndFlush(productLikeMapper.toEntity(productLike))
        } catch (e: DataIntegrityViolationException) {
            // 멱등적 처리: UNIQUE 충돌 시 이미 존재하므로 정상 반환
        }
    }

    override fun deleteByUserIdAndProductId(
        userId: Long,
        productId: Long,
    ) {
        productLikeJpaRepository.deleteByUserIdAndProductId(userId, productId)
    }

    override fun existsByUserIdAndProductId(
        userId: Long,
        productId: Long,
    ): Boolean = productLikeJpaRepository.existsByUserIdAndProductId(userId, productId)

    override fun findAllByUserId(
        userId: Long,
        pageRequest: DomainPageRequest,
    ): PageResponse<ProductLike> {
        val pageable = SpringPageRequest.of(pageRequest.page, pageRequest.size, Sort.by(Sort.Direction.DESC, "id"))
        val page = productLikeJpaRepository.findAllByUserId(userId, pageable)
        return PageResponse(
            content = page.content.map { productLikeMapper.toDomain(it) },
            totalElements = page.totalElements,
            page = pageRequest.page,
            size = pageRequest.size,
        )
    }

    override fun countByProductId(productId: Long): Int =
        productLikeJpaRepository.countByProductId(productId).toInt()
}

package com.loopers.infrastructure.lock

import com.loopers.domain.lock.ProductLockRepository
import org.springframework.stereotype.Repository

@Repository
class ProductLockRepositoryImpl(
    private val productLockJpaRepository: ProductLockJpaRepository,
) : ProductLockRepository {

    override fun findByIdForUpdate(id: Long): Long? {
        return productLockJpaRepository.findWithLockById(id)?.id
    }
}

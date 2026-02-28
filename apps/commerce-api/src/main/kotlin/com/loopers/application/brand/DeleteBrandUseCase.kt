package com.loopers.application.brand

import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.product.ProductRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 교차 집계: Brand + Product(연쇄 논리삭제)
 * MSA 분리 시 Product 연쇄 삭제 → 이벤트 기반 비동기 처리로 전환
 */
@Component
class DeleteBrandUseCase(
    private val brandRepository: BrandRepository,
    private val productRepository: ProductRepository,
) {
    @Transactional
    fun delete(id: Long) {
        val brand = brandRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다: $id")

        if (brand.isDeleted()) {
            return
        }

        val deletedBrand = brand.delete()
        brandRepository.save(deletedBrand)
        productRepository.softDeleteByBrandId(id)
    }
}

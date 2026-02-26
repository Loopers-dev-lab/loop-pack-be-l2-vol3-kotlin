package com.loopers.application.catalog

import com.loopers.application.UseCase
import com.loopers.domain.catalog.BrandRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

// TODO: Product 도입 시 BrandService(Domain Service) 추출하여 cascade 삭제 처리
@Component
class AdminDeleteBrandUseCase(
    private val brandRepository: BrandRepository,
) : UseCase<Long, Unit> {

    @Transactional
    override fun execute(brandId: Long) {
        val brand = brandRepository.findById(brandId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다.")
        brand.delete()
        brandRepository.update(brand)
    }
}

package com.loopers.application.catalog.brand

import com.loopers.application.catalog.CatalogCommand
import com.loopers.domain.catalog.brand.repository.BrandRepository
import com.loopers.domain.catalog.brand.vo.BrandName
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UpdateBrandUseCase(private val brandRepository: BrandRepository) {
    @Transactional
    fun execute(brandId: Long, name: String): BrandInfo {
        val command = CatalogCommand.UpdateBrand(name = BrandName(name))
        val brand = brandRepository.findById(brandId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다.")
        brand.update(command.name)
        val saved = brandRepository.save(brand)
        return BrandInfo.from(saved)
    }
}

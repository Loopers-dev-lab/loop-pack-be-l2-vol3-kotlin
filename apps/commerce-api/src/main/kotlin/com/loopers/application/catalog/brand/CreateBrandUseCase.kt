package com.loopers.application.catalog.brand

import com.loopers.application.catalog.CatalogCommand
import com.loopers.domain.catalog.brand.model.Brand
import com.loopers.domain.catalog.brand.repository.BrandRepository
import com.loopers.domain.catalog.brand.vo.BrandName
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CreateBrandUseCase(private val brandRepository: BrandRepository) {
    @Transactional
    fun execute(name: String): BrandInfo {
        val command = CatalogCommand.CreateBrand(name = name)
        val brand = brandRepository.save(Brand(name = BrandName(command.name)))
        return BrandInfo.from(brand)
    }
}

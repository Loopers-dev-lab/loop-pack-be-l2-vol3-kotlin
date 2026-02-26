package com.loopers.application.brand

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.support.error.BrandException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RegisterBrandUseCase(
    private val brandRepository: BrandRepository,
) {

    @Transactional
    fun execute(command: BrandCommand.Register): BrandInfo {
        if (brandRepository.existsActiveByName(command.name)) {
            throw BrandException.duplicateName()
        }
        val brand = Brand.create(command.name)
        val saved = brandRepository.save(brand)
        return BrandInfo.from(saved)
    }
}

package com.loopers.application.admin.brand

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminBrandRegisterUseCase(
    private val brandRepository: BrandRepository,
) {
    @Transactional
    fun register(command: AdminBrandCommand.Register): AdminBrandResult.Register {
        val brand = Brand.register(name = command.name)
        val saved = brandRepository.save(brand, command.admin)
        return AdminBrandResult.Register.from(saved)
    }
}

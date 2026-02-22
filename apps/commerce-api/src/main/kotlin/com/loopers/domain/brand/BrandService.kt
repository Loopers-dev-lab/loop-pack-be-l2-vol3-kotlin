package com.loopers.domain.brand

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class BrandService(
    private val brandRepository: BrandRepository,
) {
    @Transactional
    fun register(command: RegisterCommand): BrandModel {
        brandRepository.findByName(command.name)?.let {
            throw CoreException(ErrorType.CONFLICT, "이미 존재하는 브랜드입니다.")
        }
        val brand = BrandModel(
            name = command.name,
            description = command.description,
            logoUrl = command.logoUrl,
        )
        return brandRepository.save(brand)
    }
}

data class RegisterCommand(
    val name: String,
    val description: String? = null,
    val logoUrl: String? = null,
)

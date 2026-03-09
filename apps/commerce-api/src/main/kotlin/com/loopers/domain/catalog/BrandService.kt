package com.loopers.domain.catalog

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BrandService(
    private val brandRepository: BrandRepository,
) {
    @Transactional
    fun register(command: RegisterBrandCommand): BrandInfo {
        brandRepository.findByName(command.name)?.let {
            throw CoreException(ErrorType.CONFLICT, "이미 존재하는 브랜드입니다.")
        }
        val brand = BrandModel(
            name = command.name,
            description = command.description,
            logoUrl = command.logoUrl,
        )
        val saved = brandRepository.save(brand)
        return BrandInfo.from(saved)
    }

    @Transactional(readOnly = true)
    fun getBrand(id: Long): BrandInfo {
        val brand = brandRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다.")
        return BrandInfo.from(brand)
    }

    @Transactional(readOnly = true)
    fun findBrand(id: Long): BrandInfo? {
        return brandRepository.findById(id)?.let { BrandInfo.from(it) }
    }

    @Transactional(readOnly = true)
    fun getBrands(pageable: Pageable): Slice<BrandInfo> {
        return brandRepository.findAll(pageable).map { BrandInfo.from(it) }
    }

    @Transactional
    fun update(id: Long, command: UpdateBrandCommand) {
        val brand = brandRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다.")

        command.newName
            ?.takeIf { it != brand.name }
            ?.let { name ->
                brandRepository.findByName(name)?.let {
                    throw CoreException(ErrorType.CONFLICT, "이미 존재하는 브랜드입니다.")
                }
            }

        brand.update(
            newName = command.newName,
            newDescription = command.newDescription,
            newLogoUrl = command.newLogoUrl,
        )
    }

    @Transactional
    fun delete(id: Long) {
        val brand = brandRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다.")
        brand.delete()
    }
}

data class RegisterBrandCommand(
    val name: String,
    val description: String? = null,
    val logoUrl: String? = null,
)

data class UpdateBrandCommand(
    val newName: String? = null,
    val newDescription: String? = null,
    val newLogoUrl: String? = null,
)

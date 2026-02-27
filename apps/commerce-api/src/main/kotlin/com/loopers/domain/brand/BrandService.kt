package com.loopers.domain.brand

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class BrandService(
    private val brandRepository: BrandRepository,
) {
    @Transactional(readOnly = true)
    fun findById(id: Long): BrandModel {
        return brandRepository.findByIdAndDeletedAtIsNull(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 브랜드입니다: $id")
    }

    @Transactional(readOnly = true)
    fun findAll(pageable: Pageable): Page<BrandModel> {
        return brandRepository.findAllByDeletedAtIsNull(pageable)
    }

    @Transactional(readOnly = true)
    fun findAllByIds(ids: List<Long>): List<BrandModel> {
        return brandRepository.findAllByIdInAndDeletedAtIsNull(ids)
    }

    @Transactional
    fun create(name: String, description: String?, logoUrl: String?): BrandModel {
        if (brandRepository.existsByNameAndDeletedAtIsNull(name)) {
            throw CoreException(ErrorType.CONFLICT, "이미 존재하는 브랜드명입니다: $name")
        }

        val brand = BrandModel(
            name = name,
            description = description,
            logoUrl = logoUrl,
        )

        return brandRepository.save(brand)
    }

    @Transactional
    fun update(id: Long, name: String, description: String?, logoUrl: String?, status: BrandStatus): BrandModel {
        val brand = findById(id)

        if (brand.name != name && brandRepository.existsByNameAndDeletedAtIsNull(name)) {
            throw CoreException(ErrorType.CONFLICT, "이미 존재하는 브랜드명입니다: $name")
        }

        brand.update(
            name = name,
            description = description,
            logoUrl = logoUrl,
            status = status,
        )

        return brandRepository.save(brand)
    }

    @Transactional
    fun delete(id: Long) {
        val brand = findById(id)
        brand.delete()
        brandRepository.save(brand)
    }
}

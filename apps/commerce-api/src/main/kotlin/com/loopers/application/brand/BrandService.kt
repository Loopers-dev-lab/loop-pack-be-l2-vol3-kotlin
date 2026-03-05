package com.loopers.application.brand

import com.loopers.domain.brand.BrandModel
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.PageQuery
import com.loopers.domain.common.PageResult
import com.loopers.domain.error.CoreException
import com.loopers.domain.error.ErrorType
import org.springframework.stereotype.Component

@Component
class BrandService(
    private val brandRepository: BrandRepository,
) {
    fun createBrand(command: BrandCommand.Create): BrandModel {
        val brand = BrandModel(
            name = command.name,
            description = command.description,
            imageUrl = command.imageUrl,
        )
        return brandRepository.save(brand)
    }

    fun getBrandForAdmin(id: Long): BrandModel {
        return brandRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 브랜드입니다.")
    }

    fun getBrand(id: Long): BrandModel {
        val brand = brandRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 브랜드입니다.")
        if (brand.isDeleted()) {
            throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 브랜드입니다.")
        }
        return brand
    }

    fun getBrands(page: Int, size: Int): PageResult<BrandModel> {
        return brandRepository.findAll(PageQuery(page, size))
    }

    fun updateBrand(id: Long, command: BrandCommand.Update): BrandModel {
        val brand = getBrandForAdmin(id)
        val updated = brand.update(
            name = command.name,
            description = command.description,
            imageUrl = command.imageUrl,
        )
        return brandRepository.save(updated)
    }

    fun deleteBrand(id: Long) {
        val brand = getBrandForAdmin(id)
        val deleted = brand.delete()
        brandRepository.save(deleted)
    }
}

package com.loopers.domain.brand

import com.loopers.domain.brand.vo.BrandName
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class BrandService(
    private val brandRepository: BrandRepository,
) {
    @Transactional
    fun createBrand(command: BrandCommand.Create): BrandModel {
        val brand = BrandModel(
            name = BrandName.of(command.name),
            description = command.description,
            imageUrl = command.imageUrl,
        )
        return brandRepository.save(brand)
    }

    @Transactional(readOnly = true)
    fun getBrandForAdmin(id: Long): BrandModel {
        return brandRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 브랜드입니다.")
    }

    @Transactional(readOnly = true)
    fun getBrand(id: Long): BrandModel {
        val brand = brandRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 브랜드입니다.")
        if (brand.isDeleted()) {
            throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 브랜드입니다.")
        }
        return brand
    }

    @Transactional(readOnly = true)
    fun getBrands(page: Int, size: Int): Page<BrandModel> {
        return brandRepository.findAll(PageRequest.of(page, size))
    }

    @Transactional
    fun updateBrand(id: Long, command: BrandCommand.Update): BrandModel {
        val brand = getBrandForAdmin(id)
        brand.update(
            name = BrandName.of(command.name),
            description = command.description,
            imageUrl = command.imageUrl,
        )
        return brand
    }

    @Transactional
    fun deleteBrand(id: Long) {
        val brand = getBrandForAdmin(id)
        brand.delete()
    }
}

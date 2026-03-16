package com.loopers.domain.brand

import com.loopers.domain.cache.CacheEvict
import com.loopers.domain.cache.CacheNames
import com.loopers.domain.cache.Cached
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

    @Transactional
    fun createBrand(command: CreateBrandCommand): Brand {
        val brand = Brand(
            name = command.name,
            description = command.description,
            imageUrl = command.imageUrl,
        )
        return brandRepository.save(brand)
    }

    @CacheEvict(cacheName = CacheNames.BRAND_INFO, key = "{brandId}")
    @Transactional
    fun updateBrand(brandId: Long, command: UpdateBrandCommand): Brand {
        val brand = findById(brandId)
        brand.update(
            name = command.name,
            description = command.description,
            imageUrl = command.imageUrl,
        )
        return brand
    }

    @CacheEvict(cacheName = CacheNames.BRAND_INFO)
    @Transactional
    fun deleteBrand(brandId: Long) {
        val brand = findById(brandId)
        brand.delete()
    }

    /**
     * 캐시된 브랜드 정보 조회.
     *
     * 캐시 히트 시 DB 조회 없이 반환. 미스 시 DB 조회 후 캐시 저장.
     * Entity가 아닌 Info(data class) 반환 — JSON 직렬화/역직렬화 가능.
     */
    @Cached(cacheName = CacheNames.BRAND_INFO)
    @Transactional(readOnly = true)
    fun getBrandInfo(brandId: Long): BrandInfo {
        val brand = findById(brandId)
        return BrandInfo.from(brand)
    }

    @Transactional(readOnly = true)
    fun findById(brandId: Long): Brand {
        return brandRepository.findById(brandId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 브랜드입니다.")
    }

    @Transactional(readOnly = true)
    fun findByIds(ids: List<Long>): List<Brand> {
        return brandRepository.findByIds(ids)
    }

    @Transactional(readOnly = true)
    fun findAll(): List<Brand> {
        return brandRepository.findAll()
    }

    @Transactional(readOnly = true)
    fun findAll(pageable: Pageable): Page<Brand> {
        return brandRepository.findAll(pageable)
    }
}

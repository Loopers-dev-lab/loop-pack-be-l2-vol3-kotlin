package com.loopers.infrastructure.brand

import com.loopers.domain.brand.Brand
import org.springframework.stereotype.Component

@Component
class BrandMapper {
    fun toDomain(entity: BrandEntity): Brand {
        return Brand.retrieve(
            id = entity.id!!,
            name = entity.name,
            status = entity.status,
        )
    }

    fun toEntity(brand: Brand, admin: String): BrandEntity {
        return BrandEntity(
            id = brand.id,
            name = brand.name.value,
            status = brand.status,
            createdBy = admin,
            updatedBy = admin,
        )
    }
}

package com.loopers.infrastructure.brand

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandStatus
import com.loopers.domain.brand.vo.BrandName
import org.springframework.stereotype.Component

@Component
class BrandMapper {

    fun toDomain(entity: BrandEntity): Brand {
        return Brand(
            id = entity.id,
            name = BrandName(entity.name),
            status = BrandStatus.valueOf(entity.status),
        )
    }

    fun toEntity(domain: Brand): BrandEntity {
        return BrandEntity(
            name = domain.name.value,
            status = domain.status.name,
        )
    }

    fun update(entity: BrandEntity, domain: Brand) {
        entity.name = domain.name.value
        entity.status = domain.status.name
    }
}

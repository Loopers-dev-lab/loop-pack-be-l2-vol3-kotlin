package com.loopers.infrastructure.brand

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandName

object BrandMapper {

    fun toDomain(entity: BrandEntity): Brand {
        val id = requireNotNull(entity.id) {
            "BrandEntity.id가 null입니다. 저장된 Entity만 Domain으로 변환 가능합니다."
        }
        return Brand.reconstitute(
            persistenceId = id,
            name = BrandName(entity.name),
            description = entity.description,
            logoUrl = entity.logoUrl,
            status = entity.status,
            deletedAt = entity.deletedAt,
        )
    }

    fun toEntity(domain: Brand): BrandEntity {
        return BrandEntity(
            id = domain.persistenceId,
            name = domain.name.value,
            description = domain.description,
            logoUrl = domain.logoUrl,
            status = domain.status,
            deletedAt = domain.deletedAt,
        )
    }
}

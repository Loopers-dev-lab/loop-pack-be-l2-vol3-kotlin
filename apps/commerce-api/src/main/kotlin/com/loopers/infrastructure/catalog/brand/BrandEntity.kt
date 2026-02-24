package com.loopers.infrastructure.catalog.brand

import com.loopers.domain.BaseEntity
import com.loopers.domain.catalog.brand.model.Brand
import com.loopers.domain.catalog.brand.vo.BrandName
import com.loopers.domain.withBaseFields
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "brands")
class BrandEntity(
    @Column(name = "name", nullable = false)
    var name: String,
) : BaseEntity() {

    companion object {
        fun fromDomain(brand: Brand): BrandEntity {
            return BrandEntity(name = brand.name.value).withBaseFields(
                id = brand.id,
                createdAt = brand.createdAt,
                updatedAt = brand.updatedAt,
                deletedAt = brand.deletedAt,
            )
        }
    }

    fun toDomain(): Brand = Brand(
        id = id,
        name = BrandName(name),
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}

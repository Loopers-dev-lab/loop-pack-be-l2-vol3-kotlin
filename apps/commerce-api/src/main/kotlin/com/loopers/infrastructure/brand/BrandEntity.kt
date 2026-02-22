package com.loopers.infrastructure.brand

import com.loopers.domain.BaseEntity
import com.loopers.domain.brand.Brand
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "brands")
class BrandEntity private constructor(
    name: String,
    deletedAt: ZonedDateTime?,
) : BaseEntity() {

    @Column(nullable = false, length = 50)
    var name: String = name
        protected set

    @Column(name = "deleted_at")
    var deletedAt: ZonedDateTime? = deletedAt
        protected set

    fun toDomain(): Brand {
        return Brand.reconstruct(
            id = id,
            name = name,
            deletedAt = deletedAt,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    fun updateFromDomain(brand: Brand) {
        this.name = brand.name
        this.deletedAt = brand.deletedAt
    }

    companion object {
        fun fromDomain(brand: Brand): BrandEntity {
            return BrandEntity(
                name = brand.name,
                deletedAt = brand.deletedAt,
            )
        }
    }
}

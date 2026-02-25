package com.loopers.infrastructure.catalog.brand

import com.loopers.domain.BaseEntity
import com.loopers.domain.catalog.brand.Brand
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "brands")
class BrandEntity(
    name: String,
    description: String,
) : BaseEntity() {

    @Column(nullable = false)
    var name: String = name
        protected set

    @Column(nullable = false, columnDefinition = "TEXT")
    var description: String = description
        protected set

    fun update(name: String, description: String) {
        this.name = name
        this.description = description
    }

    fun toDomain(): Brand = Brand(
        id = this.id,
        name = this.name,
        description = this.description,
    )

    companion object {
        fun from(brand: Brand): BrandEntity = BrandEntity(
            name = brand.name,
            description = brand.description,
        )
    }
}

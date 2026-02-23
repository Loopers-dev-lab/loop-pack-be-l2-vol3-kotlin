package com.loopers.infrastructure.catalog.brand

import com.loopers.domain.BaseEntity
import com.loopers.domain.catalog.brand.entity.Brand
import com.loopers.domain.catalog.brand.vo.BrandName
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
            return BrandEntity(name = brand.name.value).also { entity ->
                if (brand.id != 0L) {
                    setBaseEntityField(entity, "id", brand.id)
                    setBaseEntityField(entity, "createdAt", brand.createdAt)
                    setBaseEntityField(entity, "updatedAt", brand.updatedAt)
                }
                brand.deletedAt?.let { setBaseEntityField(entity, "deletedAt", it) }
            }
        }

        private fun setBaseEntityField(entity: BaseEntity, fieldName: String, value: Any) {
            BaseEntity::class.java.getDeclaredField(fieldName).apply {
                isAccessible = true
                set(entity, value)
            }
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

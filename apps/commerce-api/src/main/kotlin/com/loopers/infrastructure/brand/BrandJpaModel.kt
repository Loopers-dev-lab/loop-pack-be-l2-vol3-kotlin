package com.loopers.infrastructure.brand

import com.loopers.domain.BaseEntity
import com.loopers.domain.brand.BrandModel
import com.loopers.domain.brand.BrandStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table

@Entity
@Table(name = "brand")
class BrandJpaModel(
    name: String,
    description: String,
    imageUrl: String,
) : BaseEntity() {
    @Column(name = "name", nullable = false)
    var name: String = name
        protected set

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    var description: String = description
        protected set

    @Column(name = "image_url", nullable = false, length = 512)
    var imageUrl: String = imageUrl
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: BrandStatus = BrandStatus.ACTIVE
        protected set

    fun toModel(): BrandModel = BrandModel(
        id = id,
        name = name,
        description = description,
        imageUrl = imageUrl,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )

    fun updateFrom(model: BrandModel) {
        this.name = model.name
        this.description = model.description
        this.imageUrl = model.imageUrl
        this.status = model.status
        if (model.deletedAt != null) {
            this.deletedAt = model.deletedAt
        }
    }

    companion object {
        fun from(model: BrandModel): BrandJpaModel =
            BrandJpaModel(
                name = model.name,
                description = model.description,
                imageUrl = model.imageUrl,
            )
    }
}

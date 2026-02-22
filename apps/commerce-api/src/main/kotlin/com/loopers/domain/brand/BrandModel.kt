package com.loopers.domain.brand

import com.loopers.domain.BaseEntity
import com.loopers.domain.brand.vo.BrandName
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table

@Entity
@Table(name = "brand")
class BrandModel(
    name: BrandName,
    description: String,
    imageUrl: String,
) : BaseEntity() {
    @Column(name = "name", nullable = false)
    var name: String = name.value
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

    fun update(name: BrandName, description: String, imageUrl: String) {
        this.name = name.value
        this.description = description
        this.imageUrl = imageUrl
    }

    override fun delete() {
        this.status = BrandStatus.DELETED
        super.delete()
    }

    fun isDeleted(): Boolean = status == BrandStatus.DELETED
}

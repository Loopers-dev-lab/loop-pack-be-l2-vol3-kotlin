package com.loopers.infrastructure.brand

import com.loopers.domain.brand.Brand
import com.loopers.infrastructure.AdminAuditEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table

@Table(name = "brand")
@Entity
class BrandEntity(
    id: Long? = null,
    @Column(nullable = false)
    var name: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: Brand.Status,
    createdBy: String,
    updatedBy: String,
) : AdminAuditEntity() {

    init {
        this.id = id
        this.createdBy = createdBy
        this.updatedBy = updatedBy
    }
}

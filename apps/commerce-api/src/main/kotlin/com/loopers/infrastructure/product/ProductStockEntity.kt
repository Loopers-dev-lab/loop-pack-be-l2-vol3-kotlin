package com.loopers.infrastructure.product

import com.loopers.infrastructure.AdminAuditEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Table(name = "product_stock")
@Entity
class ProductStockEntity(
    id: Long? = null,
    @Column(name = "product_id", nullable = false, unique = true)
    val productId: Long,
    @Column(nullable = false)
    var quantity: Int,
    createdBy: String,
    updatedBy: String,
) : AdminAuditEntity() {

    init {
        this.id = id
        this.createdBy = createdBy
        this.updatedBy = updatedBy
    }
}

package com.loopers.infrastructure.product

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(
    name = "products",
    indexes = [
        Index(name = "idx_products_brand_id_status", columnList = "brand_id, status"),
        Index(name = "idx_products_status", columnList = "status"),
    ],
)
class ProductEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "brand_id", nullable = false)
    val brandId: Long,

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "price", nullable = false)
    var price: Long,

    @Column(name = "description", length = 1000)
    var description: String,

    @Column(name = "stock", nullable = false)
    var stock: Int,

    @Column(name = "status", nullable = false)
    var status: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: ZonedDateTime? = null,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: ZonedDateTime? = null,
) {
    @PrePersist
    fun prePersist() {
        val now = ZonedDateTime.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = ZonedDateTime.now()
    }
}

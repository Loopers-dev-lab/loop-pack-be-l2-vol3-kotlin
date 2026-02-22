package com.loopers.domain.product

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "tb_product_inventory")
class ProductInventoryModel(
    productId: Long,
    stock: Stock
) {

    @Id
    val productId: Long = productId

    @Column
    var stock: Stock = stock
        protected set

    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: ZonedDateTime
        protected set

    @Column(name = "updated_at", nullable = false)
    lateinit var updatedAt: ZonedDateTime
        protected set

    @Column(name = "deleted_at")
    var deletedAt: ZonedDateTime? = null
        protected set

    @PrePersist
    private fun prePersist() {
        val now = ZonedDateTime.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    private fun preUpdate() {
        updatedAt = ZonedDateTime.now()
    }

    fun updateStock(quantity: Long) {
        stock = Stock(quantity)
    }

    fun increaseStock(quantity: Long) {
        stock = Stock(stock.value + quantity)
    }

    fun decreaseStock(quantity: Long) {
        require(stock.value >= quantity) { "재고가 부족합니다." }
        stock = Stock(stock.value - quantity)
    }

    fun delete() {
        deletedAt ?: run { deletedAt = ZonedDateTime.now() }
    }
}

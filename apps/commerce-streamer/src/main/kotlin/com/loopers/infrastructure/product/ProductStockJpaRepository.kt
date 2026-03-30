package com.loopers.infrastructure.product

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class ProductStockJpaRepository(
    @PersistenceContext private val entityManager: EntityManager,
) {

    @Transactional
    fun decrementStock(productId: Long, quantity: Int): Int {
        val sql = """
            UPDATE products
            SET stock_quantity = stock_quantity - :quantity
            WHERE id = :productId AND stock_quantity >= :quantity
        """.trimIndent()
        return entityManager.createNativeQuery(sql)
            .setParameter("productId", productId)
            .setParameter("quantity", quantity)
            .executeUpdate()
    }
}

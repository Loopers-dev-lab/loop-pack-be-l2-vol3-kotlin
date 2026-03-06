package com.loopers.domain.product

import com.loopers.domain.product.ProductInventoryModel
import com.loopers.domain.product.ProductInventoryRepository
import com.loopers.domain.product.Stock
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ProductInventoryService(
    private val productInventoryRepository: ProductInventoryRepository,
) {

    @Transactional
    fun createInventory(productId: Long, stock: Long): ProductInventoryModel {
        if (productInventoryRepository.findByProductId(productId) != null) {
            throw CoreException(
                errorType = ErrorType.CONFLICT,
                customMessage = "이미 재고가 존재하는 상품입니다.",
            )
        }

        val inventory = ProductInventoryModel(
            productId = productId,
            stock = Stock(stock),
        )

        return productInventoryRepository.save(inventory)
    }

    @Transactional(readOnly = true)
    fun getInventory(productId: Long): ProductInventoryModel {
        return productInventoryRepository.findByProductId(productId) ?: throw CoreException(
            errorType = ErrorType.NOT_FOUND,
            customMessage = "해당 상품의 재고를 찾을 수 없습니다.",
        )
    }

    @Transactional
    fun increaseStock(productId: Long, quantity: Long): ProductInventoryModel {
        val inventory = productInventoryRepository.findByProductId(productId) ?: throw CoreException(
            errorType = ErrorType.NOT_FOUND,
            customMessage = "해당 상품의 재고를 찾을 수 없습니다.",
        )

        inventory.increaseStock(quantity)
        return inventory
    }

    @Transactional
    fun decreaseStock(productId: Long, quantity: Long): ProductInventoryModel {
        val inventory = productInventoryRepository.findByProductIdForUpdate(productId) ?: throw CoreException(
            errorType = ErrorType.NOT_FOUND,
            customMessage = "해당 상품의 재고를 찾을 수 없습니다.",
        )
        try {
            inventory.decreaseStock(quantity)
        } catch (e: IllegalArgumentException) {
            throw CoreException(
                errorType = ErrorType.BAD_REQUEST,
                customMessage = e.message,
            )
        }
        return inventory
    }

    @Transactional
    fun updateStock(productId: Long, quantity: Long): ProductInventoryModel {
        val inventory = productInventoryRepository.findByProductId(productId) ?: throw CoreException(
            errorType = ErrorType.NOT_FOUND,
            customMessage = "해당 상품의 재고를 찾을 수 없습니다.",
        )

        inventory.updateStock(quantity)
        return inventory
    }

    @Transactional
    fun deleteInventory(productId: Long) {
        val inventory = productInventoryRepository.findByProductId(productId) ?: return
        inventory.delete()
    }
}

package com.loopers.application.product

import com.loopers.domain.common.CursorResult
import com.loopers.domain.common.PageQuery
import com.loopers.domain.common.PageResult
import com.loopers.domain.common.event.ProductCreatedEvent
import com.loopers.domain.common.event.ProductDeletedEvent
import com.loopers.domain.common.event.ProductUpdatedEvent
import com.loopers.domain.common.event.StockDeductedEvent
import com.loopers.domain.common.event.StockRestoredEvent
import com.loopers.domain.error.CoreException
import com.loopers.domain.error.ErrorType
import com.loopers.domain.product.ProductModel
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductSearchCondition
import com.loopers.domain.product.ProductStatus
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class ProductService(
    private val productRepository: ProductRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {
    fun createProduct(command: ProductCommand.Create): ProductModel {
        val product = ProductModel(
            brandId = command.brandId,
            name = command.name,
            description = command.description,
            price = command.price,
            stockQuantity = command.stockQuantity,
            imageUrl = command.imageUrl,
        )
        val saved = productRepository.save(product)
        eventPublisher.publishEvent(ProductCreatedEvent(productId = saved.id, brandId = saved.brandId))
        return saved
    }

    fun getProductForAdmin(id: Long): ProductModel {
        return productRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다.")
    }

    fun getProduct(id: Long): ProductModel {
        val product = productRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다.")
        if (product.isDeleted()) {
            throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다.")
        }
        return product
    }

    fun getProductsForAdmin(page: Int, size: Int, brandId: Long?): PageResult<ProductModel> {
        return if (brandId != null) {
            productRepository.findAllByBrandId(brandId, PageQuery(page, size))
        } else {
            productRepository.findAll(PageQuery(page, size))
        }
    }

    fun getProducts(condition: ProductSearchCondition): CursorResult<ProductModel> {
        return productRepository.findActiveProducts(condition)
    }

    fun getProductsByIds(ids: List<Long>): List<ProductModel> {
        return productRepository.findAllByIdIn(ids)
            .filter { !it.isDeleted() }
    }

    fun updateProduct(id: Long, command: ProductCommand.Update): ProductModel {
        val product = getProductForAdmin(id)
        val updated = product.update(
            name = command.name,
            description = command.description,
            price = command.price,
            stockQuantity = command.stockQuantity,
            imageUrl = command.imageUrl,
        )
        val saved = productRepository.save(updated)
        eventPublisher.publishEvent(ProductUpdatedEvent(productId = saved.id))
        return saved
    }

    fun deleteProduct(id: Long) {
        val product = getProductForAdmin(id)
        val deleted = product.delete()
        productRepository.save(deleted)
        eventPublisher.publishEvent(ProductDeletedEvent(productId = id))
    }

    fun deleteProductsByBrandId(brandId: Long) {
        val products = productRepository.findAllByBrandIdAndStatus(brandId, ProductStatus.ACTIVE)
        products.forEach { product ->
            val deleted = product.delete()
            productRepository.save(deleted)
            eventPublisher.publishEvent(ProductDeletedEvent(productId = product.id))
        }
    }

    fun deductStock(productId: Long, quantity: Int) {
        val product = productRepository.findByIdWithLock(productId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다.")
        val deducted = product.deductStock(quantity)
        productRepository.save(deducted)
        eventPublisher.publishEvent(StockDeductedEvent(productId = productId, quantity = quantity))
    }

    fun restoreStock(productId: Long, quantity: Int) {
        val product = productRepository.findByIdWithLock(productId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다.")
        val restored = product.restoreStock(quantity)
        productRepository.save(restored)
        eventPublisher.publishEvent(StockRestoredEvent(productId = productId, quantity = quantity))
    }
}

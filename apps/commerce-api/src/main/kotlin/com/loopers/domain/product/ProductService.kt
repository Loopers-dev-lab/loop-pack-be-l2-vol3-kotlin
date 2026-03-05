package com.loopers.domain.product

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ProductService(
    private val productRepository: ProductRepository,
) {

    @Transactional
    fun createProduct(command: CreateProductCommand): Product {
        val product = Product(
            brandId = command.brandId,
            name = command.name,
            description = command.description,
            price = command.price,
            stockQuantity = command.stockQuantity,
            displayYn = command.displayYn,
            imageUrl = command.imageUrl,
        )
        return productRepository.save(product)
    }

    @Transactional
    fun updateProduct(productId: Long, command: UpdateProductCommand): Product {
        val product = findById(productId)
        product.update(
            name = command.name,
            description = command.description,
            price = command.price,
            stockQuantity = command.stockQuantity,
            status = command.status,
            displayYn = command.displayYn,
            imageUrl = command.imageUrl,
        )
        return product
    }

    @Transactional
    fun deleteProduct(productId: Long) {
        val product = findById(productId)
        product.softDelete()
    }

    @Transactional(readOnly = true)
    fun findById(productId: Long): Product {
        return productRepository.findById(productId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다.")
    }

    @Transactional(readOnly = true)
    fun findByIds(ids: List<Long>): List<Product> {
        return productRepository.findByIds(ids)
    }

    @Transactional(readOnly = true)
    fun findByBrandId(brandId: Long): List<Product> {
        return productRepository.findByBrandId(brandId)
    }

    @Transactional(readOnly = true)
    fun findAll(): List<Product> {
        return productRepository.findAll()
    }

    @Transactional(readOnly = true)
    fun findAllForUser(pageable: Pageable, brandId: Long?): Page<Product> {
        return productRepository.findAllForUser(pageable, brandId)
    }

    @Transactional(readOnly = true)
    fun findAllForAdmin(pageable: Pageable, brandId: Long?): Page<Product> {
        return productRepository.findAllForAdmin(pageable, brandId)
    }

    /**
     * 재고 차감 — Atomic Update.
     *
     * UPDATE SET stock = stock - quantity WHERE stock >= quantity
     * → DB 레벨에서 원자적으로 차감하므로 Lost Update 불가.
     * → affectedRows=0이면 재고 부족 → 비즈니스 실패이므로 재시도 불필요.
     */
    @Transactional
    fun decreaseStock(productId: Long, quantity: Int) {
        val affectedRows = productRepository.decreaseStock(productId, quantity)
        if (affectedRows == 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "상품의 재고가 부족합니다.")
        }
    }

    /**
     * 좋아요 수 증가 — Atomic Update.
     *
     * 왜 dirty checking(product.increaseLikeCount())이 아닌가?
     * - 인기 상품에 다수 유저가 동시 좋아요 → Lost Update 위험
     * - Product에 @Version 걸면 좋아요/재고/상품수정이 같은 version 공유 → false contention
     * - Atomic Update는 DB 레벨에서 원자적 증감 → 동시성 안전 + 재시도 불필요
     */
    @Transactional
    fun increaseLikeCount(productId: Long) {
        productRepository.increaseLikeCount(productId)
    }

    @Transactional
    fun decreaseLikeCount(productId: Long) {
        productRepository.decreaseLikeCount(productId)
    }

    @Transactional
    fun decreaseLikeCountIfExists(productId: Long) {
        productRepository.decreaseLikeCount(productId)
    }
}

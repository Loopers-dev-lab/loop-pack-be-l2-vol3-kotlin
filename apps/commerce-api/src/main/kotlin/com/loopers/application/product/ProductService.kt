package com.loopers.application.product

import com.loopers.domain.product.ProductCommand
import com.loopers.domain.product.ProductModel
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductSearchCondition
import com.loopers.domain.product.ProductStatus
import com.loopers.domain.product.vo.ProductDescription
import com.loopers.domain.product.vo.ProductName
import com.loopers.domain.product.vo.StockQuantity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ProductService(
    private val productRepository: ProductRepository,
) {
    @Transactional
    fun createProduct(command: ProductCommand.Create): ProductModel {
        val product = ProductModel(
            brandId = command.brandId,
            name = ProductName.of(command.name),
            description = ProductDescription.of(command.description),
            price = command.price,
            stockQuantity = StockQuantity.of(command.stockQuantity),
            imageUrl = command.imageUrl,
        )
        return productRepository.save(product)
    }

    @Transactional(readOnly = true)
    fun getProductForAdmin(id: Long): ProductModel {
        return productRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다.")
    }

    @Transactional(readOnly = true)
    fun getProduct(id: Long): ProductModel {
        val product = productRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다.")
        if (product.isDeleted()) {
            throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다.")
        }
        return product
    }

    @Transactional(readOnly = true)
    fun getProductsForAdmin(page: Int, size: Int, brandId: Long?): Page<ProductModel> {
        return if (brandId != null) {
            productRepository.findAllByBrandId(brandId, PageRequest.of(page, size))
        } else {
            productRepository.findAll(PageRequest.of(page, size))
        }
    }

    @Transactional(readOnly = true)
    fun getProducts(condition: ProductSearchCondition): List<ProductModel> {
        return productRepository.findActiveProducts(condition)
    }

    @Transactional(readOnly = true)
    fun getProductsByIds(ids: List<Long>): List<ProductModel> {
        return productRepository.findAllByIdIn(ids)
            .filter { !it.isDeleted() }
    }

    @Transactional
    fun updateProduct(id: Long, command: ProductCommand.Update): ProductModel {
        val product = getProductForAdmin(id)
        product.update(
            name = ProductName.of(command.name),
            description = ProductDescription.of(command.description),
            price = command.price,
            stockQuantity = StockQuantity.of(command.stockQuantity),
            imageUrl = command.imageUrl,
        )
        return product
    }

    @Transactional
    fun deleteProduct(id: Long) {
        val product = getProductForAdmin(id)
        product.delete()
    }

    @Transactional
    fun deleteProductsByBrandId(brandId: Long) {
        val products = productRepository.findAllByBrandIdAndStatus(brandId, ProductStatus.ACTIVE)
        products.forEach { it.delete() }
    }

    @Transactional
    fun deductStock(productId: Long, quantity: Int) {
        val product = productRepository.findByIdWithLock(productId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다.")
        product.deductStock(quantity)
    }
}

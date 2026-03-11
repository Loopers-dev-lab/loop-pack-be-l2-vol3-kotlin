package com.loopers.domain.catalog.product

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProductService(
    private val productRepository: ProductRepository,
) {

    @Transactional
    fun createProduct(
        brandId: Long,
        name: String,
        description: String,
        price: Int,
        stock: Int,
    ): Product {
        val product = Product(
            brandId = brandId,
            name = name,
            description = description,
            price = price,
            stock = stock,
        )
        return productRepository.save(product)
    }

    @Transactional(readOnly = true)
    fun getById(id: Long): Product =
        productRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "[$id] 해당 ID에 해당하는 상품이 존재하지 않습니다.")

    @Transactional(readOnly = true)
    fun getActiveById(id: Long): Product {
        val product = productRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "[$id] 해당 ID에 해당하는 상품이 존재하지 않습니다.")
        if (product.status != ProductStatus.ACTIVE)
            throw CoreException(ErrorType.NOT_FOUND, "[$id] 해당 ID에 해당하는 상품이 존재하지 않습니다.")
        return product
    }

    @Transactional
    fun update(id: Long, name: String, description: String, price: Int, stock: Int): Product {
        val product = getById(id)
        product.update(name, description, price, stock)
        return productRepository.save(product)
    }

    @Transactional
    fun decrementStock(productId: Long, quantity: Int): Product {
        val product = getById(productId)
        product.decrementStock(quantity)
        return productRepository.save(product)
    }

    @Transactional
    fun incrementLikeCount(productId: Long): Product {
        val product = getById(productId)
        product.incrementLike()
        return productRepository.save(product)
    }

    @Transactional
    fun decrementLikeCount(productId: Long): Product {
        val product = getById(productId)
        product.decrementLike()
        return productRepository.save(product)
    }

    @Transactional
    fun delete(id: Long) {
        productRepository.deleteById(id)
    }

    @Transactional
    fun deleteAllByBrandId(brandId: Long) {
        productRepository.deleteAllByBrandId(brandId)
    }

    @Transactional(readOnly = true)
    fun findAll(condition: ProductSearchCondition): List<Product> =
        productRepository.findAll(condition)
}

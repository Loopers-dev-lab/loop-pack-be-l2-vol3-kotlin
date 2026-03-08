package com.loopers.domain.catalog

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class ProductService(
    private val productRepository: ProductRepository,
) {
    @Transactional
    fun register(command: RegisterProductCommand): ProductInfo {
        val product = ProductModel(
            brandId = command.brandId,
            name = command.name,
            quantity = command.quantity,
            price = command.price,
        )
        val saved = productRepository.save(product)
        return ProductInfo.from(saved)
    }

    @Transactional(readOnly = true)
    fun getProduct(id: Long): ProductInfo {
        val product = productRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
        return ProductInfo.from(product)
    }

    @Transactional(readOnly = true)
    fun findProduct(id: Long): ProductInfo? {
        return productRepository.findById(id)?.let { ProductInfo.from(it) }
    }

    @Transactional(readOnly = true)
    fun getProducts(
        pageable: Pageable,
        brandId: Long? = null,
        sortType: ProductSortType = ProductSortType.LATEST,
    ): Slice<ProductInfo> {
        val slice = productRepository.search(sortType, brandId, pageable)
        return slice.map { ProductInfo.from(it) }
    }

    @Transactional
    fun update(id: Long, command: UpdateProductCommand) {
        val product = productRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
        product.update(
            newName = command.newName,
            newQuantity = command.newQuantity,
            newPrice = command.newPrice,
        )
    }

    @Transactional
    fun delete(id: Long) {
        val product = productRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
        product.delete()
    }

    @Transactional
    fun deleteAllByBrandId(brandId: Long) {
        productRepository.findAllByBrandId(brandId).forEach { it.delete() }
    }

    @Transactional
    fun increaseLikeCount(productId: Long) {
        productRepository.increaseLikeCount(productId)
    }

    @Transactional
    fun decreaseLikeCount(productId: Long) {
        productRepository.decreaseLikeCount(productId)
    }
}

data class RegisterProductCommand(
    val brandId: Long,
    val name: String,
    val quantity: Int,
    val price: BigDecimal,
)

data class UpdateProductCommand(
    val newName: String,
    val newQuantity: Int,
    val newPrice: BigDecimal,
)

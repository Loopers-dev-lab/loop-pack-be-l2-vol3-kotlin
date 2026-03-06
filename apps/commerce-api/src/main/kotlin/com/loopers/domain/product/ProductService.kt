package com.loopers.domain.product

import com.loopers.application.api.order.dto.OrderItemCriteria
import com.loopers.domain.brand.Brand
import com.loopers.domain.product.dto.ProductInfo
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
@Transactional(readOnly = true)
class ProductService(
    private val productDomainService: ProductDomainService,
    private val productRepository: ProductRepository,
) {

    fun getProductInfo(id: Long): ProductInfo {
        val findProduct = findActiveProduct(id)
        return ProductInfo.from(findProduct)
    }

    fun getProducts(brandId: Long?, pageable: Pageable): Page<ProductInfo> =
        productRepository.findWithPaging(brandId, pageable).map { ProductInfo.from(it) }

    fun getActiveProducts(brandId: Long?, pageable: Pageable): Page<ProductInfo> =
        productRepository.findActiveProductsWithPaging(brandId, pageable).map { ProductInfo.from(it) }

    @Transactional
    fun createProduct(
        brand: Brand,
        name: String,
        price: BigDecimal,
        stock: Int,
        status: ProductStatus,
    ): Long {
        val newProduct = Product.create(
            brand = brand,
            name = name,
            price = price,
            stock = stock,
            status = status,
        )
        val savedProduct = productRepository.save(newProduct)
        return savedProduct.id
    }

    @Transactional
    fun updateProduct(
        id: Long,
        name: String,
        price: BigDecimal,
        stock: Int,
        status: ProductStatus,
    ) {
        val findProduct = findProduct(id)
        productDomainService.updateProductInfo(findProduct, name, price, stock, status)
    }

    @Transactional
    fun deleteProduct(id: Long) {
        val findProduct = findProduct(id)
        findProduct.delete()
    }

    @Transactional
    fun deleteProductsByBrand(brandId: Long) {
        productRepository.findByBrandId(brandId).forEach(Product::delete)
    }

    @Transactional
    fun decreaseProductsStock(orderItemRequest: List<OrderItemCriteria>) {
        orderItemRequest.sortedBy { it.productId }.forEach {
            val product = findProductWithLock(it.productId)
            product.minusStock(it.quantity)
        }
    }

    private fun findProductWithLock(id: Long) =
        productRepository.findProductWithLock(id)
            ?.takeIf { !it.isDeleted() }
            ?.takeIf { it.status != ProductStatus.INACTIVE }
            ?: throw CoreException(ErrorType.NOT_FOUND, "상품이 존재하지 않습니다.")

    fun getProduct(productId: Long): Product = findActiveProduct(productId)

    private fun findActiveProduct(id: Long) =
        productRepository.findById(id)
        ?.takeIf { !it.isDeleted() }
        ?.takeIf { it.status != ProductStatus.INACTIVE }
        ?: throw CoreException(ErrorType.NOT_FOUND, "상품이 존재하지 않습니다.")

    private fun findProduct(id: Long) = (
        productRepository.findById(id)
        ?.takeIf { !it.isDeleted() }
        ?: throw CoreException(ErrorType.NOT_FOUND, "상품이 존재하지 않습니다.")
    )
}

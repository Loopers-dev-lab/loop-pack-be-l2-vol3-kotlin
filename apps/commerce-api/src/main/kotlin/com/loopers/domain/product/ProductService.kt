package com.loopers.domain.product

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
    private val productRepository: ProductRepository,
) {

    fun getProductInfo(id: Long): ProductInfo {
        val findProduct = productRepository.findById(id)
            .takeIf { !it.isDeleted() }
            ?.takeIf { it.status != ProductStatus.INACTIVE }
            ?: throw CoreException(ErrorType.NOT_FOUND, "상품이 존재하지 않습니다.")
        return ProductInfo.from(findProduct)
    }

    fun getProducts(brandId: Long?, pageable: Pageable): Page<ProductInfo> =
        productRepository.findWithPaging(brandId, pageable).map { ProductInfo.from(it) }

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
        val findProduct = productRepository.findById(id)
            .takeIf { !it.isDeleted() }
            ?: throw CoreException(ErrorType.NOT_FOUND, "상품이 존재하지 않습니다.")

        findProduct.updateInfo(name, price)
        findProduct.changeStatus(status)
        findProduct.updateStock(stock)
    }

    @Transactional
    fun deleteProduct(id: Long) {
        val findProduct = productRepository.findById(id)
            .takeIf { !it.isDeleted() }
            ?: throw CoreException(ErrorType.NOT_FOUND, "상품이 존재하지 않습니다.")
        findProduct.delete()
    }

    @Transactional
    fun deleteProductsByBrand(brandId: Long) {
        productRepository.findByBrandId(brandId).forEach(Product::delete)
    }
}

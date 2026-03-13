package com.loopers.domain.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.product.dto.ProductInfo
import com.loopers.domain.productlike.ProductLikeCountRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.cache.annotation.CacheEvict
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
    private val productLikeCountRepository: ProductLikeCountRepository,
) {

    fun getProductInfo(id: Long): ProductInfo {
        val findProduct = findActiveProduct(id)
        val likeCount = productLikeCountRepository.findByProductId(id)?.likeCount ?: 0
        return ProductInfo.from(findProduct, likeCount)
    }

    fun getProducts(brandId: Long?, pageable: Pageable): Page<ProductInfo> =
        productRepository.findWithPaging(brandId, pageable).map { product ->
            val likeCount = productLikeCountRepository.findByProductId(product.id)?.likeCount ?: 0
            ProductInfo.from(product, likeCount)
        }

    fun getActiveProducts(brandId: Long?, pageable: Pageable): Page<ProductInfo> =
        productRepository.findActiveProductsWithPaging(brandId, pageable).map { product ->
            val likeCount = productLikeCountRepository.findByProductId(product.id)?.likeCount ?: 0
            ProductInfo.from(product, likeCount)
        }

    @Transactional
    fun createProduct(
        brand: Brand,
        name: String,
        price: BigDecimal,
        status: ProductStatus,
    ): Long {
        val newProduct = Product.create(
            brand = brand,
            name = name,
            price = price,
            status = status,
        )
        val savedProduct = productRepository.save(newProduct)
        return savedProduct.id
    }

    @Transactional
    @CacheEvict(value = ["product-info"], key = "#id")
    fun updateProduct(
        id: Long,
        name: String,
        price: BigDecimal,
        status: ProductStatus,
    ) {
        val findProduct = findProduct(id)
        productDomainService.updateProductInfo(findProduct, name, price, status)
    }

    @Transactional
    @CacheEvict(value = ["product-info"], key = "#id")
    fun deleteProduct(id: Long) {
        val findProduct = findProduct(id)
        findProduct.delete()
    }

    @Transactional
    @CacheEvict(value = ["product-info"], allEntries = true)
    fun deleteProductsByBrand(brandId: Long) {
        productRepository.findByBrandId(brandId).forEach(Product::delete)
    }

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

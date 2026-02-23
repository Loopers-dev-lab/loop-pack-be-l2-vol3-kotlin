package com.loopers.domain.catalog

import com.loopers.domain.PageResult
import com.loopers.domain.catalog.brand.entity.Brand
import com.loopers.domain.catalog.brand.repository.BrandRepository
import com.loopers.domain.catalog.product.entity.Product
import com.loopers.domain.catalog.product.ProductSort
import com.loopers.domain.catalog.product.repository.ProductRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CatalogService(
    private val brandRepository: BrandRepository,
    private val productRepository: ProductRepository,
) {

    // === Brand CRUD ===

    @Transactional
    fun createBrand(command: CatalogCommand.CreateBrand): Brand {
        return brandRepository.save(Brand(name = command.name))
    }

    @Transactional
    fun updateBrand(brandId: Long, command: CatalogCommand.UpdateBrand): Brand {
        val brand = getBrand(brandId)
        brand.update(command.name)
        return brandRepository.save(brand)
    }

    @Transactional
    fun deleteBrand(brandId: Long) {
        val brand = brandRepository.findById(brandId) ?: return
        brand.delete()
        brandRepository.save(brand)
        productRepository.findAllByBrandId(brandId).forEach {
            it.delete()
            productRepository.save(it)
        }
    }

    @Transactional
    fun restoreBrand(brandId: Long): Brand {
        val brand = getBrand(brandId)
        brand.restore()
        return brandRepository.save(brand)
    }

    // === Brand 조회 ===

    @Transactional(readOnly = true)
    fun getActiveBrand(brandId: Long): Brand {
        val brand = brandRepository.findById(brandId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다.")
        if (brand.isDeleted()) {
            throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다.")
        }
        return brand
    }

    @Transactional(readOnly = true)
    fun getBrand(brandId: Long): Brand {
        return brandRepository.findById(brandId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다.")
    }

    @Transactional(readOnly = true)
    fun getBrands(page: Int, size: Int): PageResult<Brand> {
        return brandRepository.findAll(page, size)
    }

    // === Product CRUD ===

    @Transactional
    fun createProduct(command: CatalogCommand.CreateProduct): Product {
        getActiveBrand(command.brandId)
        return productRepository.save(
            Product(
                refBrandId = command.brandId,
                name = command.name,
                price = command.price,
                stock = command.stock,
            ),
        )
    }

    @Transactional
    fun updateProduct(productId: Long, command: CatalogCommand.UpdateProduct): Product {
        val product = getProduct(productId)
        product.update(command.name, command.price, command.stock, command.status)
        return productRepository.save(product)
    }

    @Transactional
    fun deleteProduct(productId: Long) {
        val product = productRepository.findById(productId) ?: return
        product.delete()
        productRepository.save(product)
    }

    @Transactional
    fun restoreProduct(productId: Long): Product {
        val product = getProduct(productId)
        product.restore()
        return productRepository.save(product)
    }

    // === 대고객 조회 ===

    @Transactional(readOnly = true)
    fun getProducts(brandId: Long?, sort: ProductSort, page: Int, size: Int): PageResult<Product> {
        return productRepository.findActiveProducts(brandId, sort, page, size)
    }

    @Transactional(readOnly = true)
    fun getProductDetail(productId: Long): ProductDetail {
        val product = getActiveProduct(productId)
        val brand = getBrand(product.refBrandId)
        return ProductDetail(product = product, brand = brand)
    }

    // === 내부용 조회 ===

    @Transactional(readOnly = true)
    fun getProduct(productId: Long): Product {
        return productRepository.findById(productId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
    }

    @Transactional(readOnly = true)
    fun getActiveProduct(productId: Long): Product {
        val product = getProduct(productId)
        if (!product.isActive()) {
            throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
        }
        return product
    }

    @Transactional(readOnly = true)
    fun getActiveProductsByIds(productIds: List<Long>): List<Product> {
        return productRepository.findAllByIds(productIds).filter { it.isActive() }
    }

    @Transactional(readOnly = true)
    fun getProductsForOrder(productIds: List<Long>): List<Product> {
        val products = productRepository.findAllByIds(productIds)

        val foundIds = products.map { it.id }.toSet()
        val missingIds = productIds.filter { it !in foundIds }
        if (missingIds.isNotEmpty()) {
            throw CoreException(ErrorType.BAD_REQUEST, "존재하지 않는 상품이 포함되어 있습니다.")
        }

        products.forEach { product ->
            if (!product.isAvailableForOrder()) {
                throw CoreException(ErrorType.BAD_REQUEST, "주문 가능한 상태가 아닌 상품이 포함되어 있습니다.")
            }
        }

        return products
    }

    // === 재고 관리 ===

    @Transactional
    fun decreaseStocks(items: Map<Long, Int>) {
        val products = productRepository.findAllByIds(items.keys.toList())
        val productMap = products.associateBy { it.id }
        items.forEach { (productId, quantity) ->
            val product = productMap[productId]
                ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
            product.decreaseStock(quantity)
            productRepository.save(product)
        }
    }

    // === likeCount ===

    @Transactional
    fun increaseLikeCount(productId: Long) {
        val product = getProduct(productId)
        product.increaseLikeCount()
        productRepository.save(product)
    }

    @Transactional
    fun decreaseLikeCount(productId: Long) {
        val product = getProduct(productId)
        if (product.isDeleted()) return
        product.decreaseLikeCount()
        productRepository.save(product)
    }

    // === 어드민 조회 ===

    @Transactional(readOnly = true)
    fun getAdminProducts(page: Int, size: Int): PageResult<Product> {
        return productRepository.findAll(page, size)
    }

    @Transactional(readOnly = true)
    fun getAdminProduct(productId: Long): Product {
        return getProduct(productId)
    }
}

package com.loopers.domain.catalog.product

import com.loopers.domain.PageResult
import com.loopers.domain.catalog.product.model.Product
import com.loopers.domain.catalog.product.repository.ProductRepository
import com.loopers.domain.common.vo.BrandId
import com.loopers.domain.common.vo.ProductId

class FakeProductRepository : ProductRepository {

    private val products = mutableListOf<Product>()
    private var sequence = 1L

    override fun save(product: Product): Product {
        if (product.id != ProductId(0)) {
            products.removeIf { it.id == product.id }
            products.add(product)
            return product
        }
        val saved = Product(
            id = ProductId(sequence++),
            refBrandId = product.refBrandId,
            name = product.name,
            price = product.price,
            stock = product.stock,
            status = product.status,
            likeCount = product.likeCount,
            deletedAt = product.deletedAt,
        )
        products.add(saved)
        return saved
    }

    override fun saveAll(products: List<Product>): List<Product> {
        return products.map { save(it) }
    }

    override fun findById(id: ProductId): Product? {
        return products.find { it.id == id }
    }

    override fun findByIdForUpdate(id: ProductId): Product? {
        return products.find { it.id == id }
    }

    override fun findAll(page: Int, size: Int): PageResult<Product> {
        val offset = page * size
        val active = products.filter { it.deletedAt == null }
        val content = active.drop(offset).take(size)
        return PageResult(content, active.size.toLong(), page, size)
    }

    override fun findAllIncludeDeleted(page: Int, size: Int): PageResult<Product> {
        val offset = page * size
        val content = products.drop(offset).take(size)
        return PageResult(content, products.size.toLong(), page, size)
    }

    override fun findActiveProducts(brandId: BrandId?, sort: ProductSort, page: Int, size: Int): PageResult<Product> {
        var filtered = products.filter {
            it.deletedAt == null && it.status != Product.ProductStatus.HIDDEN
        }
        brandId?.let { id -> filtered = filtered.filter { it.refBrandId == id } }

        val sorted = when (sort) {
            ProductSort.LATEST -> filtered.sortedByDescending { it.id.value }
            ProductSort.PRICE_ASC -> filtered.sortedBy { it.price.value }
            ProductSort.LIKES_DESC -> filtered.sortedByDescending { it.likeCount }
        }

        val offset = page * size
        val content = sorted.drop(offset).take(size)
        return PageResult(content, sorted.size.toLong(), page, size)
    }

    override fun findAllByBrandId(brandId: BrandId): List<Product> {
        return products.filter { it.refBrandId == brandId && it.deletedAt == null }
    }

    override fun findAllByIds(ids: List<ProductId>): List<Product> {
        return products.filter { it.id in ids && it.deletedAt == null }
    }

    override fun findAllByIdsForUpdate(ids: List<ProductId>): List<Product> {
        return products.filter { it.id in ids && it.deletedAt == null }.sortedBy { it.id.value }
    }
}

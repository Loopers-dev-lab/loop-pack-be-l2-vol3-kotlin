package com.loopers.domain.catalog.product

import com.loopers.domain.PageResult
import com.loopers.domain.catalog.product.entity.Product
import com.loopers.domain.catalog.product.repository.ProductRepository

class FakeProductRepository : ProductRepository {

    private val products = mutableListOf<Product>()
    private var sequence = 1L

    override fun save(product: Product): Product {
        if (product.id != 0L) {
            products.removeIf { it.id == product.id }
            products.add(product)
            return product
        }
        val saved = Product(
            id = sequence++,
            refBrandId = product.refBrandId,
            name = product.name,
            price = product.price,
            stock = product.stock,
            status = product.status,
            likeCount = product.likeCount,
            createdAt = product.createdAt,
            updatedAt = product.updatedAt,
            deletedAt = product.deletedAt,
        )
        products.add(saved)
        return saved
    }

    override fun findById(id: Long): Product? {
        return products.find { it.id == id }
    }

    override fun findAll(page: Int, size: Int): PageResult<Product> {
        val offset = page * size
        val content = products.drop(offset).take(size)
        return PageResult(content, products.size.toLong(), page, size)
    }

    override fun findActiveProducts(brandId: Long?, sort: ProductSort, page: Int, size: Int): PageResult<Product> {
        var filtered = products.filter {
            it.deletedAt == null && it.status != Product.ProductStatus.HIDDEN
        }
        brandId?.let { id -> filtered = filtered.filter { it.refBrandId == id } }

        val sorted = when (sort) {
            ProductSort.LATEST -> filtered.sortedByDescending { it.id }
            ProductSort.PRICE_ASC -> filtered.sortedBy { it.price.value }
            ProductSort.LIKES_DESC -> filtered.sortedByDescending { it.likeCount }
        }

        val offset = page * size
        val content = sorted.drop(offset).take(size)
        return PageResult(content, sorted.size.toLong(), page, size)
    }

    override fun findAllByBrandId(brandId: Long): List<Product> {
        return products.filter { it.refBrandId == brandId }
    }

    override fun findAllByIds(ids: List<Long>): List<Product> {
        return products.filter { it.id in ids }
    }
}

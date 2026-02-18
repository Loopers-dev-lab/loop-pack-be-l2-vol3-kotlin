package com.loopers.domain.product.fixture

import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductSortType
import com.loopers.domain.product.Stock

class FakeProductRepository : ProductRepository {

    private val store = HashMap<Long, Product>()
    private var sequence = 1L

    override fun save(product: Product): Long {
        val id = product.persistenceId ?: sequence++
        val images = product.images.mapIndexed { index, image ->
            if (image.persistenceId != null) {
                image
            } else {
                com.loopers.domain.product.ProductImage.reconstitute(
                    persistenceId = (id * 1000) + index,
                    imageUrl = image.imageUrl,
                    displayOrder = image.displayOrder,
                )
            }
        }
        val persisted = Product.reconstitute(
            persistenceId = id,
            brandId = product.brandId,
            name = product.name,
            description = product.description,
            price = product.price,
            stock = product.stock,
            thumbnailUrl = product.thumbnailUrl,
            status = product.status,
            likeCount = product.likeCount,
            deletedAt = product.deletedAt,
            images = images,
        )
        store[id] = persisted
        return id
    }

    override fun findById(id: Long): Product? {
        return store[id]
    }

    override fun findByIdForUpdate(id: Long): Product? {
        return store[id]
    }

    override fun decreaseStock(id: Long, quantity: Int): Int {
        val product = store[id] ?: return 0
        if (product.stock.quantity < quantity) return 0
        val updated = Product.reconstitute(
            persistenceId = product.persistenceId!!,
            brandId = product.brandId,
            name = product.name,
            description = product.description,
            price = product.price,
            stock = Stock(product.stock.quantity - quantity),
            thumbnailUrl = product.thumbnailUrl,
            status = product.status,
            likeCount = product.likeCount,
            deletedAt = product.deletedAt,
            images = product.images,
        )
        store[id] = updated
        return 1
    }

    override fun increaseStock(id: Long, quantity: Int): Int {
        val product = store[id] ?: return 0
        val updated = Product.reconstitute(
            persistenceId = product.persistenceId!!,
            brandId = product.brandId,
            name = product.name,
            description = product.description,
            price = product.price,
            stock = Stock(product.stock.quantity + quantity),
            thumbnailUrl = product.thumbnailUrl,
            status = product.status,
            likeCount = product.likeCount,
            deletedAt = product.deletedAt,
            images = product.images,
        )
        store[id] = updated
        return 1
    }

    override fun incrementLikeCount(id: Long): Int {
        val product = store[id] ?: return 0
        val updated = Product.reconstitute(
            persistenceId = product.persistenceId!!,
            brandId = product.brandId,
            name = product.name,
            description = product.description,
            price = product.price,
            stock = product.stock,
            thumbnailUrl = product.thumbnailUrl,
            status = product.status,
            likeCount = product.likeCount + 1,
            deletedAt = product.deletedAt,
            images = product.images,
        )
        store[id] = updated
        return 1
    }

    override fun decrementLikeCount(id: Long): Int {
        val product = store[id] ?: return 0
        if (product.likeCount <= 0) return 0
        val updated = Product.reconstitute(
            persistenceId = product.persistenceId!!,
            brandId = product.brandId,
            name = product.name,
            description = product.description,
            price = product.price,
            stock = product.stock,
            thumbnailUrl = product.thumbnailUrl,
            status = product.status,
            likeCount = product.likeCount - 1,
            deletedAt = product.deletedAt,
            images = product.images,
        )
        store[id] = updated
        return 1
    }

    override fun softDeleteByBrandId(brandId: Long): Int {
        var count = 0
        store.values.filter { it.brandId == brandId && !it.isDeleted() }.forEach { product ->
            val deleted = product.delete()
            store[product.persistenceId!!] = deleted
            count++
        }
        return count
    }

    override fun findAllActive(brandId: Long?, sortType: ProductSortType): List<Product> {
        var result = store.values.filter { !it.isDeleted() }
        if (brandId != null) {
            result = result.filter { it.brandId == brandId }
        }
        return when (sortType) {
            ProductSortType.LIKE_COUNT -> result.sortedByDescending { it.likeCount }
            ProductSortType.PRICE_ASC -> result.sortedBy { it.price.amount }
            ProductSortType.CREATED_AT -> result.sortedByDescending { it.persistenceId }
        }
    }

    override fun findAll(brandId: Long?): List<Product> {
        var result = store.values.toList()
        if (brandId != null) {
            result = result.filter { it.brandId == brandId }
        }
        return result.sortedByDescending { it.persistenceId }
    }
}

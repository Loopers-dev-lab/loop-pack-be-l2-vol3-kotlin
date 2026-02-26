package com.loopers.infrastructure.product

import com.loopers.domain.product.ProductModel
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductSearchCondition
import com.loopers.domain.product.ProductSort
import com.loopers.domain.product.ProductStatus
import jakarta.persistence.EntityManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class ProductRepositoryImpl(
    private val productJpaRepository: ProductJpaRepository,
    private val entityManager: EntityManager,
) : ProductRepository {
    override fun save(product: ProductModel): ProductModel {
        return productJpaRepository.save(product)
    }

    override fun findById(id: Long): ProductModel? {
        return productJpaRepository.findById(id).orElse(null)
    }

    override fun findByIdWithLock(id: Long): ProductModel? {
        return productJpaRepository.findByIdWithLock(id)
    }

    override fun findAll(pageable: Pageable): Page<ProductModel> {
        return productJpaRepository.findAll(pageable)
    }

    override fun findAllByBrandId(brandId: Long, pageable: Pageable): Page<ProductModel> {
        return productJpaRepository.findAllByBrandId(brandId, pageable)
    }

    override fun findAllByIdIn(ids: List<Long>): List<ProductModel> {
        if (ids.isEmpty()) return emptyList()
        return productJpaRepository.findAllByIdIn(ids)
    }

    override fun findActiveProducts(condition: ProductSearchCondition): List<ProductModel> {
        val sb = StringBuilder("SELECT p FROM ProductModel p WHERE p.status = :status")
        val params = mutableMapOf<String, Any>("status" to ProductStatus.ACTIVE)

        if (condition.brandId != null) {
            sb.append(" AND p.brandId = :brandId")
            params["brandId"] = condition.brandId
        }

        if (condition.cursor != null) {
            appendCursorCondition(sb, params, condition.sort, condition.cursor)
        }

        appendOrderBy(sb, condition.sort)

        val query = entityManager.createQuery(sb.toString(), ProductModel::class.java)
        params.forEach { (key, value) -> query.setParameter(key, value) }
        query.maxResults = condition.size + 1

        return query.resultList
    }

    override fun findAllByBrandIdAndStatus(brandId: Long, status: ProductStatus): List<ProductModel> {
        return productJpaRepository.findAllByBrandIdAndStatus(brandId, status)
    }

    private fun appendCursorCondition(
        sb: StringBuilder,
        params: MutableMap<String, Any>,
        sort: ProductSort,
        cursor: Map<String, Any>,
    ) {
        when (sort) {
            ProductSort.LATEST -> {
                val cursorId = (cursor["id"] as Number).toLong()
                sb.append(" AND p.id < :cursorId")
                params["cursorId"] = cursorId
            }
            ProductSort.PRICE_ASC -> {
                val cursorPrice = (cursor["price"] as Number).toLong()
                val cursorId = (cursor["id"] as Number).toLong()
                sb.append(" AND (p.price > :cursorPrice OR (p.price = :cursorPrice AND p.id < :cursorId))")
                params["cursorPrice"] = cursorPrice
                params["cursorId"] = cursorId
            }
            ProductSort.LIKES_DESC -> {
                val cursorLikeCount = (cursor["likeCount"] as Number).toInt()
                val cursorId = (cursor["id"] as Number).toLong()
                sb.append(" AND (p.likeCount < :cursorLikeCount OR (p.likeCount = :cursorLikeCount AND p.id < :cursorId))")
                params["cursorLikeCount"] = cursorLikeCount
                params["cursorId"] = cursorId
            }
        }
    }

    private fun appendOrderBy(sb: StringBuilder, sort: ProductSort) {
        when (sort) {
            ProductSort.LATEST -> sb.append(" ORDER BY p.id DESC")
            ProductSort.PRICE_ASC -> sb.append(" ORDER BY p.price ASC, p.id DESC")
            ProductSort.LIKES_DESC -> sb.append(" ORDER BY p.likeCount DESC, p.id DESC")
        }
    }
}

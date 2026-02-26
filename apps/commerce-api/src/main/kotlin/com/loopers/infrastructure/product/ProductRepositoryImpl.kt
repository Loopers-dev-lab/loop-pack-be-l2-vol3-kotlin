package com.loopers.infrastructure.product

import com.loopers.domain.common.PageQuery
import com.loopers.domain.common.PageResult
import com.loopers.domain.product.ProductModel
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductSearchCondition
import com.loopers.domain.product.ProductSort
import com.loopers.domain.product.ProductStatus
import jakarta.persistence.EntityManager
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class ProductRepositoryImpl(
    private val productJpaRepository: ProductJpaRepository,
    private val entityManager: EntityManager,
) : ProductRepository {
    override fun save(product: ProductModel): ProductModel {
        if (product.id == 0L) {
            return productJpaRepository.save(ProductJpaModel.from(product)).toModel()
        }
        val existing = productJpaRepository.findById(product.id).orElseThrow()
        existing.updateFrom(product)
        return existing.toModel()
    }

    override fun findById(id: Long): ProductModel? {
        return productJpaRepository.findById(id).orElse(null)?.toModel()
    }

    override fun findByIdWithLock(id: Long): ProductModel? {
        return productJpaRepository.findByIdWithLock(id)?.toModel()
    }

    override fun findAll(pageQuery: PageQuery): PageResult<ProductModel> {
        val pageable = PageRequest.of(pageQuery.page, pageQuery.size)
        val page = productJpaRepository.findAll(pageable)
        return PageResult(
            content = page.content.map { it.toModel() },
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )
    }

    override fun findAllByBrandId(brandId: Long, pageQuery: PageQuery): PageResult<ProductModel> {
        val pageable = PageRequest.of(pageQuery.page, pageQuery.size)
        val page = productJpaRepository.findAllByBrandId(brandId, pageable)
        return PageResult(
            content = page.content.map { it.toModel() },
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )
    }

    override fun findAllByIdIn(ids: List<Long>): List<ProductModel> {
        if (ids.isEmpty()) return emptyList()
        return productJpaRepository.findAllByIdIn(ids).map { it.toModel() }
    }

    override fun findActiveProducts(condition: ProductSearchCondition): List<ProductModel> {
        val sb = StringBuilder("SELECT p FROM ProductJpaModel p WHERE p.status = :status")
        val params = mutableMapOf<String, Any>("status" to ProductStatus.ACTIVE)

        if (condition.brandId != null) {
            sb.append(" AND p.brandId = :brandId")
            params["brandId"] = condition.brandId
        }

        if (condition.cursor != null) {
            appendCursorCondition(sb, params, condition.sort, condition.cursor)
        }

        appendOrderBy(sb, condition.sort)

        val query = entityManager.createQuery(sb.toString(), ProductJpaModel::class.java)
        params.forEach { (key, value) -> query.setParameter(key, value) }
        query.maxResults = condition.size + 1

        return query.resultList.map { it.toModel() }
    }

    override fun findAllByBrandIdAndStatus(brandId: Long, status: ProductStatus): List<ProductModel> {
        return productJpaRepository.findAllByBrandIdAndStatus(brandId, status).map { it.toModel() }
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

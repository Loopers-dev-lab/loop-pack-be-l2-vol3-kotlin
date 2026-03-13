package com.loopers.infrastructure.like

import com.loopers.domain.like.ProductLike
import com.loopers.domain.like.ProductLikeRepository
import com.loopers.support.page.PageResponse
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.domain.Sort
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.Clock
import java.time.Instant
import com.loopers.support.page.PageRequest as DomainPageRequest
import org.springframework.data.domain.PageRequest as SpringPageRequest

@Repository
class ProductLikeRepositoryImpl(
    private val productLikeJpaRepository: ProductLikeJpaRepository,
    private val productLikeMapper: ProductLikeMapper,
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val clock: Clock,
) : ProductLikeRepository {
    override fun save(productLike: ProductLike): Boolean {
        val sql = "INSERT INTO product_like (user_id, product_id, created_at) VALUES (:userId, :productId, :createdAt)"
        val params = MapSqlParameterSource()
            .addValue("userId", productLike.userId)
            .addValue("productId", productLike.productId)
            .addValue("createdAt", Timestamp.from(Instant.now(clock)))
        return try {
            jdbcTemplate.update(sql, params) > 0
        } catch (e: DuplicateKeyException) {
            false
        }
    }

    override fun deleteByUserIdAndProductId(
        userId: Long,
        productId: Long,
    ): Boolean {
        val sql = "DELETE FROM product_like WHERE user_id = :userId AND product_id = :productId"
        val params = MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("productId", productId)
        return jdbcTemplate.update(sql, params) > 0
    }

    override fun existsByUserIdAndProductId(
        userId: Long,
        productId: Long,
    ): Boolean = productLikeJpaRepository.existsByUserIdAndProductId(userId, productId)

    override fun findAllByUserId(
        userId: Long,
        pageRequest: DomainPageRequest,
    ): PageResponse<ProductLike> {
        val pageable = SpringPageRequest.of(pageRequest.page, pageRequest.size, Sort.by(Sort.Direction.DESC, "id"))
        val page = productLikeJpaRepository.findAllByUserId(userId, pageable)
        return PageResponse(
            content = page.content.map { productLikeMapper.toDomain(it) },
            totalElements = page.totalElements,
            page = pageRequest.page,
            size = pageRequest.size,
        )
    }

    override fun countByProductId(productId: Long): Int =
        productLikeJpaRepository.countByProductId(productId).toInt()
}

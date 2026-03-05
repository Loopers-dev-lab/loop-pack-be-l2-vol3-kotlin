package com.loopers.infrastructure.product

import com.loopers.domain.common.CursorResult
import com.loopers.domain.product.ProductModel
import com.loopers.domain.product.ProductSearchCondition
import com.loopers.domain.product.ProductSort
import com.loopers.domain.product.ProductStatus
import com.loopers.infrastructure.support.CursorUtils
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Component

@Component
class ProductQueryDslRepository(
    private val queryFactory: JPAQueryFactory,
) {
    private val product = QProductJpaModel.productJpaModel

    fun findActiveProducts(condition: ProductSearchCondition): CursorResult<ProductModel> {
        val cursor = condition.cursor?.let { decodeCursor(it, condition.sort) }

        val fetched = queryFactory
            .selectFrom(product)
            .where(
                product.status.eq(ProductStatus.ACTIVE),
                brandIdEq(condition.brandId),
                cursorCondition(cursor),
            )
            .orderBy(*orderSpecifiers(condition.sort))
            .limit((condition.size + 1).toLong())
            .fetch()

        val hasNext = fetched.size > condition.size
        val content = (if (hasNext) fetched.dropLast(1) else fetched).map { it.toModel() }

        val nextCursor = if (hasNext && content.isNotEmpty()) {
            encodeCursor(condition.sort, content.last())
        } else {
            null
        }

        return CursorResult(
            content = content,
            nextCursor = nextCursor,
            hasNext = hasNext,
        )
    }

    private fun decodeCursor(cursor: String, sort: ProductSort): ProductCursor {
        val map = CursorUtils.decode(cursor)
        return when (sort) {
            ProductSort.LATEST -> ProductCursor.Latest(
                id = (map["id"] as Number).toLong(),
            )
            ProductSort.PRICE_ASC -> ProductCursor.PriceAsc(
                price = (map["price"] as Number).toLong(),
                id = (map["id"] as Number).toLong(),
            )
            ProductSort.LIKES_DESC -> ProductCursor.LikesDesc(
                likeCount = (map["likeCount"] as Number).toInt(),
                id = (map["id"] as Number).toLong(),
            )
        }
    }

    private fun encodeCursor(sort: ProductSort, last: ProductModel): String {
        val cursorMap = when (sort) {
            ProductSort.LATEST -> mapOf("id" to last.id)
            ProductSort.PRICE_ASC -> mapOf("price" to last.price, "id" to last.id)
            ProductSort.LIKES_DESC -> mapOf("likeCount" to last.likeCount, "id" to last.id)
        }
        return CursorUtils.encode(cursorMap)
    }

    private fun brandIdEq(brandId: Long?): BooleanExpression? {
        return brandId?.let { product.brandId.eq(it) }
    }

    private fun cursorCondition(cursor: ProductCursor?): BooleanExpression? {
        if (cursor == null) return null
        return when (cursor) {
            is ProductCursor.Latest ->
                product.id.lt(cursor.id)
            is ProductCursor.PriceAsc ->
                product.price.gt(cursor.price)
                    .or(product.price.eq(cursor.price).and(product.id.lt(cursor.id)))
            is ProductCursor.LikesDesc ->
                product.likeCount.lt(cursor.likeCount)
                    .or(product.likeCount.eq(cursor.likeCount).and(product.id.lt(cursor.id)))
        }
    }

    private fun orderSpecifiers(sort: ProductSort): Array<OrderSpecifier<*>> {
        return when (sort) {
            ProductSort.LATEST -> arrayOf(product.id.desc())
            ProductSort.PRICE_ASC -> arrayOf(product.price.asc(), product.id.desc())
            ProductSort.LIKES_DESC -> arrayOf(product.likeCount.desc(), product.id.desc())
        }
    }
}

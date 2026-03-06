package com.loopers.infrastructure.order

import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderRepository
import com.loopers.domain.order.QOrder
import com.querydsl.core.types.Order as QueryDslOrder
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class OrderRepositoryImpl(
    private val orderJpaRepository: OrderJpaRepository,
    private val queryFactory: JPAQueryFactory,
) : OrderRepository {

    override fun save(order: Order): Order = orderJpaRepository.save(order)

    override fun findById(id: Long): Order? = orderJpaRepository.findByIdOrNull(id)

    override fun findByUserId(userId: Long, pageable: Pageable): Page<Order> {
        val qOrder = QOrder.order

        val content = queryFactory
            .selectFrom(qOrder)
            .leftJoin(qOrder._orderItems).fetchJoin()
            .where(qOrder.userId.eq(userId))
            .distinct()
            .orderBy(OrderSpecifier(QueryDslOrder.DESC, qOrder.createdAt))
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val total = queryFactory
            .select(qOrder.countDistinct())
            .from(qOrder)
            .where(qOrder.userId.eq(userId))
            .fetchOne() ?: 0L

        return PageImpl(content, pageable, total)
    }

    override fun findOrders(pageable: Pageable): Page<Order> {
        val qOrder = QOrder.order

        val content = queryFactory
            .selectFrom(qOrder)
            .leftJoin(qOrder._orderItems).fetchJoin()
            .distinct()
            .orderBy(OrderSpecifier(QueryDslOrder.DESC, qOrder.createdAt))
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val total = queryFactory
            .select(qOrder.countDistinct())
            .from(qOrder)
            .fetchOne() ?: 0L

        return PageImpl(content, pageable, total)
    }
}

package com.loopers.infrastructure.order

import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderStatusQueryRepository
import com.loopers.infrastructure.order.QOrderEntity.orderEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

@Repository
class OrderStatusQueryRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : OrderStatusQueryRepository {

    override fun findStatusById(orderId: Long): Order.Status? =
        queryFactory
            .select(orderEntity.status)
            .from(orderEntity)
            .where(
                orderEntity.id.eq(orderId),
                orderEntity.deletedAt.isNull,
            )
            .fetchOne()
}

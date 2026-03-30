package com.loopers.batch.job.stock

import org.springframework.batch.item.database.JdbcPagingItemReader
import org.springframework.batch.item.database.Order
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder
import org.springframework.jdbc.core.RowMapper
import javax.sql.DataSource

fun stockReconciliationReader(dataSource: DataSource, pageSize: Int): JdbcPagingItemReader<ProductStock> {
    return JdbcPagingItemReaderBuilder<ProductStock>()
        .name("stockReconciliationReader")
        .dataSource(dataSource)
        .selectClause("SELECT id, stock_quantity")
        .fromClause("FROM products")
        .whereClause("WHERE deleted_at IS NULL")
        .sortKeys(mapOf("id" to Order.ASCENDING))
        .rowMapper(
            RowMapper { rs, _ ->
                ProductStock(
                    productId = rs.getLong("id"),
                    dbStock = rs.getLong("stock_quantity"),
                )
            },
        )
        .pageSize(pageSize)
        .build()
}

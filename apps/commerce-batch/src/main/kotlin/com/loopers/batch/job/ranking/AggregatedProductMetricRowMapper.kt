package com.loopers.batch.job.ranking

import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet

class AggregatedProductMetricRowMapper : RowMapper<AggregatedProductMetric> {
    override fun mapRow(rs: ResultSet, rowNum: Int): AggregatedProductMetric =
        AggregatedProductMetric(
            productId = rs.getLong("product_id"),
            viewCount = rs.getInt("view_count"),
            likeCount = rs.getInt("like_count"),
            unitsSold = rs.getInt("units_sold"),
            salesAmount = rs.getLong("sales_amount"),
            orderScore = rs.getDouble("order_score"),
            totalScore = rs.getDouble("total_score"),
        )
}

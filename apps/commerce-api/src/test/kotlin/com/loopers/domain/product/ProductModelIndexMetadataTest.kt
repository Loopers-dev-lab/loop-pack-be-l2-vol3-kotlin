package com.loopers.domain.product

import jakarta.persistence.Table
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ProductModel index metadata")
class ProductModelIndexMetadataTest {
    @DisplayName("round5 조회 최적화용 인덱스를 선언한다")
    @Test
    fun declaresRound5Indexes() {
        // arrange
        val table = ProductModel::class.java.getAnnotation(Table::class.java)

        // act
        val indexes = table.indexes.associateBy { it.name }

        // assert
        assertThat(indexes.keys).contains(
            "idx_product_brand_deleted_at_price",
            "idx_product_brand_deleted_at_likes_count",
            "idx_product_deleted_at_created_at",
        )
        assertThat(indexes["idx_product_brand_deleted_at_likes_count"]?.columnList)
            .isEqualTo("brand_id, deleted_at, likes_count, id")
    }
}

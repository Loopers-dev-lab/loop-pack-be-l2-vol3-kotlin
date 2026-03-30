package com.loopers.infrastructure.order

import com.loopers.domain.common.Money
import com.loopers.domain.common.StockQuantity
import com.loopers.domain.order.StockReservationRepository
import com.loopers.domain.product.ProductService
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate

@SpringBootTest
class StockCacheWarmerTest @Autowired constructor(
    private val stockCacheWarmer: StockCacheWarmer,
    private val productService: ProductService,
    private val stockReservationRepository: StockReservationRepository,
    private val redisTemplate: RedisTemplate<String, String>,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
        redisCleanUp.truncateAll()
    }

    @Test
    @DisplayName("모든 상품의 stockQuantity가 Redis에 정확히 반영된다")
    fun `모든 상품의 stockQuantity가 Redis에 정확히 반영된다`() {
        // given
        val product1 = productService.createProduct("상품A", null, Money.of(1000), StockQuantity.of(50), 1L)
        val product2 = productService.createProduct("상품B", null, Money.of(2000), StockQuantity.of(30), 1L)
        val product3 = productService.createProduct("상품C", null, Money.of(3000), StockQuantity.of(0), 1L)

        // when
        stockCacheWarmer.warmUp()

        // then
        assertThat(redisTemplate.opsForValue().get("stock:${product1.id}")).isEqualTo("50")
        assertThat(redisTemplate.opsForValue().get("stock:${product2.id}")).isEqualTo("30")
        assertThat(redisTemplate.opsForValue().get("stock:${product3.id}")).isEqualTo("0")
    }
}

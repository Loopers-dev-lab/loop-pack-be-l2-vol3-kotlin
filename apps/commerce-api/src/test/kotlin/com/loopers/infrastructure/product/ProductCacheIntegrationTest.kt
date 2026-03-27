package com.loopers.infrastructure.product

import com.loopers.application.product.ProductCachePort
import com.loopers.application.product.ProductInfo
import com.loopers.domain.product.ProductSortType
import com.loopers.testcontainers.MySqlTestContainersConfig
import com.loopers.testcontainers.RedisTestContainersConfig
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@Import(MySqlTestContainersConfig::class, RedisTestContainersConfig::class)
@ActiveProfiles("test")
class ProductCacheIntegrationTest {

    @Autowired
    private lateinit var productCachePort: ProductCachePort

    @Autowired
    private lateinit var redisTemplate: RedisTemplate<String, String>

    @Autowired
    private lateinit var redisCleanUp: RedisCleanUp

    @BeforeEach
    fun setUp() {
        redisCleanUp.truncateAll()
    }

    @Test
    fun `상품 상세 캐시를 저장하고 조회할 수 있다`() {
        val productInfo = createProductInfo(1L)

        productCachePort.setProductDetail(1L, productInfo)
        val cached = productCachePort.getProductDetail(1L)

        assertThat(cached).isNotNull
        assertThat(cached!!.id).isEqualTo(1L)
        assertThat(cached.name).isEqualTo("테스트상품")
        assertThat(cached.price).isEqualTo(10000L)
    }

    @Test
    fun `상품 상세 캐시가 Redis에 실제로 저장된다`() {
        val productInfo = createProductInfo(2L)

        productCachePort.setProductDetail(2L, productInfo)

        val rawValue = redisTemplate.opsForValue().get("product:detail:2")
        assertThat(rawValue).isNotNull
        assertThat(rawValue).contains("\"id\":2")
        assertThat(rawValue).contains("테스트상품")
    }

    @Test
    fun `상품 상세 캐시를 삭제할 수 있다`() {
        val productInfo = createProductInfo(3L)
        productCachePort.setProductDetail(3L, productInfo)

        productCachePort.evictProductDetail(3L)

        val cached = productCachePort.getProductDetail(3L)
        assertThat(cached).isNull()
    }

    @Test
    fun `존재하지 않는 캐시를 조회하면 null을 반환한다`() {
        val cached = productCachePort.getProductDetail(999L)
        assertThat(cached).isNull()
    }

    @Test
    fun `상품 목록 캐시를 저장하고 조회할 수 있다`() {
        val list = listOf(createProductInfo(1L), createProductInfo(2L))

        productCachePort.setProductList(1L, ProductSortType.LIKE_COUNT, list)
        val cached = productCachePort.getProductList(1L, ProductSortType.LIKE_COUNT)

        assertThat(cached).isNotNull
        assertThat(cached).hasSize(2)
        assertThat(cached!![0].id).isEqualTo(1L)
        assertThat(cached[1].id).isEqualTo(2L)
    }

    @Test
    fun `brandId가 null인 목록 캐시도 정상 동작한다`() {
        val list = listOf(createProductInfo(1L))

        productCachePort.setProductList(null, ProductSortType.CREATED_AT, list)
        val cached = productCachePort.getProductList(null, ProductSortType.CREATED_AT)

        assertThat(cached).isNotNull
        assertThat(cached).hasSize(1)

        val key = "product:list:brand:all:sort:CREATED_AT"
        assertThat(redisTemplate.hasKey(key)).isTrue()
    }

    @Test
    fun `TTL이 설정되어 있다`() {
        productCachePort.setProductDetail(10L, createProductInfo(10L))

        val ttl = redisTemplate.getExpire("product:detail:10")
        assertThat(ttl).isGreaterThan(0)
        assertThat(ttl).isLessThanOrEqualTo(600) // 10분 = 600초
    }

    private fun createProductInfo(id: Long) = ProductInfo(
        id = id,
        brandId = 1L,
        brandName = "테스트브랜드",
        brandLogoUrl = null,
        name = "테스트상품",
        description = "설명",
        price = 10000L,
        stock = 100,
        thumbnailUrl = null,
        status = "ACTIVE",
        likeCount = 0,
        deletedAt = null,
        images = emptyList(),
    )
}

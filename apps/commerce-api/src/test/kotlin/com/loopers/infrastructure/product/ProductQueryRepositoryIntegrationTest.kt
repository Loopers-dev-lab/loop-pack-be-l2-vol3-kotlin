package com.loopers.infrastructure.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.Money
import com.loopers.domain.common.Quantity
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductQueryRepository
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductStock
import com.loopers.domain.product.ProductStockRepository
import com.loopers.infrastructure.metric.ProductMetricEntity
import com.loopers.infrastructure.metric.ProductMetricJpaRepository
import com.loopers.support.page.PageRequest
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.SpyBean
import java.math.BigDecimal

@DisplayName("ProductQueryRepository integration")
@SpringBootTest
class ProductQueryRepositoryIntegrationTest
@Autowired
constructor(
    private val productQueryRepository: ProductQueryRepository,
    private val productRepository: ProductRepository,
    private val productStockRepository: ProductStockRepository,
    private val brandRepository: BrandRepository,
    private val productMetricJpaRepository: ProductMetricJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
) {
    @SpyBean
    private lateinit var redisProductQueryCache: RedisProductQueryCache

    private lateinit var activeBrand: Brand

    @BeforeEach
    fun setUp() {
        val savedBrand = brandRepository.save(Brand.register(name = "Query Brand"), ADMIN)
        activeBrand = brandRepository.save(savedBrand.update("Query Brand", "ACTIVE"), ADMIN)
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
        redisCleanUp.truncateAll()
    }

    @Test
    @DisplayName("detail cache hit은 product_metrics.like_count를 다시 읽어 likeCount를 갱신한다")
    fun getDetail_refreshesLikeCount_fromMetrics() {
        val product = createActiveProduct("상세 상품")
        val productId = product.id!!

        assertThat(productQueryRepository.getDetail(productId).likeCount).isEqualTo(0)
        putLikeCount(productId, 7)

        val refreshed = productQueryRepository.getDetail(productId)

        assertThat(refreshed.likeCount).isEqualTo(7)
        assertThat(redisProductQueryCache.getDetail(productId)?.likeCount).isEqualTo(7)
    }

    @Test
    @DisplayName("non-LIKES_DESC list cache hit은 product_metrics.like_count를 다시 읽어 likeCount를 갱신한다")
    fun getList_refreshesLikeCount_fromMetrics() {
        val first = createActiveProduct("상품 A")
        val second = createActiveProduct("상품 B")
        putLikeCount(first.id!!, 1)
        putLikeCount(second.id!!, 2)

        val pageRequest = PageRequest()
        val brandId = activeBrand.id!!

        assertThat(productQueryRepository.getList(pageRequest, brandId, null).content.map { it.id to it.likeCount })
            .containsExactlyInAnyOrder(
                first.id!! to 1,
                second.id!! to 2,
            )

        putLikeCount(first.id!!, 8)
        putLikeCount(second.id!!, 3)

        val refreshed = productQueryRepository.getList(pageRequest, brandId, null)

        assertThat(refreshed.content.map { it.id to it.likeCount })
            .containsExactlyInAnyOrder(
                first.id!! to 8,
                second.id!! to 3,
            )

        val namespaceVersion = redisProductQueryCache.getListNamespaceVersion(brandId)
        val cached = redisProductQueryCache.getList(pageRequest, brandId, null, namespaceVersion)

        assertThat((cached?.content ?: emptyList()).map { it.id to it.likeCount })
            .containsExactlyInAnyOrder(
                first.id!! to 8,
                second.id!! to 3,
            )
    }

    @Test
    @DisplayName("LIKES_DESC는 Redis cache를 우회하고 product_metrics 기준으로만 조회한다")
    fun getList_likesDesc_bypassesCache() {
        val low = createActiveProduct("낮은 좋아요")
        val high = createActiveProduct("높은 좋아요")
        putLikeCount(low.id!!, 1)
        putLikeCount(high.id!!, 5)

        clearInvocations(redisProductQueryCache)
        val result = productQueryRepository.getList(PageRequest(), activeBrand.id!!, Product.SortType.LIKES_DESC)

        assertThat(result.content.map { it.id }).containsExactly(high.id!!, low.id!!)
        assertThat(result.content.map { it.likeCount }).containsExactly(5, 1)
        verify(redisProductQueryCache, never()).getListNamespaceVersion(any())
        verify(redisProductQueryCache, never()).getList(any(), any(), any(), any())
        verify(redisProductQueryCache, never()).putList(any(), any(), any(), any(), any())
    }

    private fun createActiveProduct(name: String): Product {
        val registered = Product.register(
            name = name,
            regularPrice = Money(BigDecimal("10000")),
            sellingPrice = Money(BigDecimal("9000")),
            brandId = activeBrand.id!!,
        )
        val activated = productRepository.save(registered, ADMIN).activate()
        val savedActive = productRepository.save(activated, ADMIN)

        productStockRepository.save(
            ProductStock.create(productId = savedActive.id!!, initialQuantity = Quantity(10)),
            ADMIN,
        )

        return savedActive
    }

    private fun putLikeCount(productId: Long, likeCount: Int) {
        val metric = productMetricJpaRepository.findByProductId(productId)
            ?: ProductMetricEntity(productId = productId)
        metric.likeCount = likeCount
        productMetricJpaRepository.saveAndFlush(metric)
    }

    companion object {
        private const val ADMIN = "loopers.admin"
    }
}

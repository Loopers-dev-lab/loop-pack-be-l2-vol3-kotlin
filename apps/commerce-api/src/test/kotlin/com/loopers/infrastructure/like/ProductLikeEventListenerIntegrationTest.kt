package com.loopers.infrastructure.like

import com.loopers.application.user.like.ProductLikeCanceledEvent
import com.loopers.application.user.like.ProductLikeRegisteredEvent
import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.Money
import com.loopers.domain.common.Quantity
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductQueryResult
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductStock
import com.loopers.domain.product.ProductStockRepository
import com.loopers.infrastructure.product.RedisProductQueryCache
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.context.ApplicationEventPublisher
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal

@DisplayName("Product like event listener integration")
@SpringBootTest
class ProductLikeEventListenerIntegrationTest
@Autowired
constructor(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val productRepository: ProductRepository,
    private val productStockRepository: ProductStockRepository,
    private val brandRepository: BrandRepository,
    private val databaseCleanUp: DatabaseCleanUp,
    platformTransactionManager: PlatformTransactionManager,
) {
    @SpyBean
    private lateinit var redisProductQueryCache: RedisProductQueryCache

    private val transactionTemplate = TransactionTemplate(platformTransactionManager)

    private var productId: Long = 0
    private lateinit var activeBrand: Brand

    @BeforeEach
    fun setUp() {
        val savedBrand = brandRepository.save(Brand.register(name = "Listener Brand"), ADMIN)
        activeBrand = brandRepository.save(savedBrand.update("Listener Brand", "ACTIVE"), ADMIN)
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Test
    @DisplayName("like registered event is handled after commit to increment likeCount and evict detail cache")
    fun handle_productLikeRegistered_afterCommit() {
        productId = createActiveProduct(likeCount = 0)
        putDetailCache(productId, likeCount = 0)

        transactionTemplate.executeWithoutResult {
            applicationEventPublisher.publishEvent(
                ProductLikeRegisteredEvent(
                    userId = USER_ID,
                    productId = productId,
                ),
            )

            assertThat(productRepository.findById(productId)!!.likeCount).isEqualTo(0)
            assertThat(redisProductQueryCache.getDetail(productId)).isNotNull()
        }

        assertThat(productRepository.findById(productId)!!.likeCount).isEqualTo(1)
        assertThat(redisProductQueryCache.getDetail(productId)).isNull()
    }

    @Test
    @DisplayName("like registered event keeps likeCount when detail cache eviction fails")
    fun handle_productLikeRegistered_preservesLikeCount_whenCacheEvictionFails() {
        productId = createActiveProduct(likeCount = 0)
        putDetailCache(productId, likeCount = 0)
        doThrow(RuntimeException("boom"))
            .whenever(redisProductQueryCache)
            .evictDetails(listOf(productId))

        assertDoesNotThrow {
            transactionTemplate.executeWithoutResult {
                applicationEventPublisher.publishEvent(
                    ProductLikeRegisteredEvent(
                        userId = USER_ID,
                        productId = productId,
                    ),
                )
            }
        }

        assertThat(productRepository.findById(productId)!!.likeCount).isEqualTo(1)
        assertThat(redisProductQueryCache.getDetail(productId)).isNotNull()
    }

    @Test
    @DisplayName("like registered event is not handled when transaction rolls back")
    fun handle_productLikeRegistered_notHandled_onRollback() {
        productId = createActiveProduct(likeCount = 0)
        putDetailCache(productId, likeCount = 0)

        assertThrows<RuntimeException> {
            transactionTemplate.executeWithoutResult {
                applicationEventPublisher.publishEvent(
                    ProductLikeRegisteredEvent(
                        userId = USER_ID,
                        productId = productId,
                    ),
                )
                throw RuntimeException("rollback")
            }
        }

        assertThat(productRepository.findById(productId)!!.likeCount).isEqualTo(0)
        assertThat(redisProductQueryCache.getDetail(productId)).isNotNull()
    }

    @Test
    @DisplayName("like canceled event is handled after commit to decrement likeCount and evict detail cache")
    fun handle_productLikeCanceled_afterCommit() {
        productId = createActiveProduct(likeCount = 1)
        putDetailCache(productId, likeCount = 1)

        transactionTemplate.executeWithoutResult {
            applicationEventPublisher.publishEvent(
                ProductLikeCanceledEvent(
                    userId = USER_ID,
                    productId = productId,
                ),
            )

            assertThat(productRepository.findById(productId)!!.likeCount).isEqualTo(1)
            assertThat(redisProductQueryCache.getDetail(productId)).isNotNull()
        }

        assertThat(productRepository.findById(productId)!!.likeCount).isEqualTo(0)
        assertThat(redisProductQueryCache.getDetail(productId)).isNull()
    }

    @Test
    @DisplayName("like canceled event is not handled when transaction rolls back")
    fun handle_productLikeCanceled_notHandled_onRollback() {
        productId = createActiveProduct(likeCount = 1)
        putDetailCache(productId, likeCount = 1)

        assertThrows<RuntimeException> {
            transactionTemplate.executeWithoutResult {
                applicationEventPublisher.publishEvent(
                    ProductLikeCanceledEvent(
                        userId = USER_ID,
                        productId = productId,
                    ),
                )
                throw RuntimeException("rollback")
            }
        }

        assertThat(productRepository.findById(productId)!!.likeCount).isEqualTo(1)
        assertThat(redisProductQueryCache.getDetail(productId)).isNotNull()
    }

    @Test
    @DisplayName("like canceled event keeps likeCount when detail cache eviction fails")
    fun handle_productLikeCanceled_preservesLikeCount_whenCacheEvictionFails() {
        productId = createActiveProduct(likeCount = 1)
        putDetailCache(productId, likeCount = 1)
        doThrow(RuntimeException("boom"))
            .whenever(redisProductQueryCache)
            .evictDetails(listOf(productId))

        assertDoesNotThrow {
            transactionTemplate.executeWithoutResult {
                applicationEventPublisher.publishEvent(
                    ProductLikeCanceledEvent(
                        userId = USER_ID,
                        productId = productId,
                    ),
                )
            }
        }

        assertThat(productRepository.findById(productId)!!.likeCount).isEqualTo(0)
        assertThat(redisProductQueryCache.getDetail(productId)).isNotNull()
    }

    private fun createActiveProduct(likeCount: Int): Long {
        val registered = Product.register(
            name = "Listener Product",
            regularPrice = Money(BigDecimal("10000")),
            sellingPrice = Money(BigDecimal("9000")),
            brandId = activeBrand.id!!,
        )
        val activated = productRepository.save(registered, ADMIN).activate()
        val savedActive = productRepository.save(activated, ADMIN)
        val savedWithLikeCount = Product.retrieve(
            id = savedActive.id!!,
            name = savedActive.name,
            regularPrice = savedActive.regularPrice,
            sellingPrice = savedActive.sellingPrice,
            brandId = savedActive.brandId,
            imageUrl = savedActive.imageUrl,
            thumbnailUrl = savedActive.thumbnailUrl,
            likeCount = likeCount,
            status = savedActive.status,
        )
        val product = productRepository.save(savedWithLikeCount, ADMIN)

        productStockRepository.save(
            ProductStock.create(productId = product.id!!, initialQuantity = Quantity(10)),
            ADMIN,
        )

        return product.id!!
    }

    private fun putDetailCache(productId: Long, likeCount: Int) {
        val product = productRepository.findById(productId)!!
        val stock = productStockRepository.findByProductId(productId)!!

        redisProductQueryCache.putDetail(
            ProductQueryResult.Detail.from(product, activeBrand, stock).copy(likeCount = likeCount),
        )
    }

    companion object {
        private const val ADMIN = "loopers.admin"
        private const val USER_ID = 1L
    }
}

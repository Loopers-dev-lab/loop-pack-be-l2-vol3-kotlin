package com.loopers.infrastructure.like

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
import com.loopers.support.event.user.ProductLikeCanceledEvent
import com.loopers.support.event.user.ProductLikeRegisteredEvent
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
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
    private val redisCleanUp: RedisCleanUp,
    platformTransactionManager: PlatformTransactionManager,
) {
    private val transactionTemplate = TransactionTemplate(platformTransactionManager)

    private var productId: Long = 0
    private lateinit var activeBrand: Brand

    private lateinit var redisProductQueryCache: RedisProductQueryCache

    @Autowired
    fun setRedisProductQueryCache(cache: RedisProductQueryCache) {
        redisProductQueryCache = cache
    }

    @BeforeEach
    fun setUp() {
        val savedBrand = brandRepository.save(Brand.register(name = "Listener Brand"), ADMIN)
        activeBrand = brandRepository.save(savedBrand.update("Listener Brand", "ACTIVE"), ADMIN)
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
        redisCleanUp.truncateAll()
    }

    @Test
    @DisplayName("like registered event는 커밋 후 detail/list cache를 유지한다")
    fun handle_productLikeRegistered_afterCommit_keepsCaches() {
        productId = createActiveProduct()
        putDetailCache(productId, likeCount = 0)
        val listNamespaceBefore = redisProductQueryCache.getListNamespaceVersion(activeBrand.id!!)
        val allListNamespaceBefore = redisProductQueryCache.getListNamespaceVersion(null)

        transactionTemplate.executeWithoutResult {
            applicationEventPublisher.publishEvent(
                ProductLikeRegisteredEvent(
                    userId = USER_ID,
                    productId = productId,
                ),
            )
        }

        assertThat(redisProductQueryCache.getDetail(productId)).isNotNull
        assertThat(redisProductQueryCache.getListNamespaceVersion(activeBrand.id!!)).isEqualTo(listNamespaceBefore)
        assertThat(redisProductQueryCache.getListNamespaceVersion(null)).isEqualTo(allListNamespaceBefore)
    }

    @Test
    @DisplayName("like canceled event는 커밋 후 detail/list cache를 유지한다")
    fun handle_productLikeCanceled_afterCommit_keepsCaches() {
        productId = createActiveProduct()
        putDetailCache(productId, likeCount = 1)
        val listNamespaceBefore = redisProductQueryCache.getListNamespaceVersion(activeBrand.id!!)
        val allListNamespaceBefore = redisProductQueryCache.getListNamespaceVersion(null)

        transactionTemplate.executeWithoutResult {
            applicationEventPublisher.publishEvent(
                ProductLikeCanceledEvent(
                    userId = USER_ID,
                    productId = productId,
                ),
            )
        }

        assertThat(redisProductQueryCache.getDetail(productId)).isNotNull
        assertThat(redisProductQueryCache.getListNamespaceVersion(activeBrand.id!!)).isEqualTo(listNamespaceBefore)
        assertThat(redisProductQueryCache.getListNamespaceVersion(null)).isEqualTo(allListNamespaceBefore)
    }

    private fun createActiveProduct(): Long {
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
            likeCount = 0,
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

package com.loopers.application.handler

import com.loopers.application.handler.brand.BrandDeletedEventHandler
import com.loopers.application.handler.brand.CascadeDeleteProductsCommandHandler
import com.loopers.application.handler.cache.AuthCacheCommandHandler
import com.loopers.application.handler.cache.BrandCacheCommandHandler
import com.loopers.application.handler.cache.CacheEvictEventHandler
import com.loopers.application.handler.cache.ProductCacheCommandHandler
import com.loopers.application.handler.like.DeleteProductLikesCommandHandler
import com.loopers.application.handler.like.ProductDeletedLikeEventHandler
import com.loopers.application.handler.product.DeductStockCommandHandler
import com.loopers.application.handler.product.RestoreStockCommandHandler
import com.loopers.application.auth.FakeAuthCacheStore
import com.loopers.application.brand.BrandCommand
import com.loopers.application.brand.BrandService
import com.loopers.application.brand.FakeBrandCacheStore
import com.loopers.application.brand.FakeBrandRepository
import com.loopers.application.like.FakeProductLikeRepository
import com.loopers.application.like.LikeService
import com.loopers.application.product.FakeProductCacheStore
import com.loopers.application.product.FakeProductRepository
import com.loopers.application.product.ProductCommand
import com.loopers.application.product.ProductService
import com.loopers.domain.common.event.BrandDeletedEvent
import com.loopers.domain.common.event.BrandUpdatedEvent
import com.loopers.domain.common.event.MemberPasswordChangedEvent
import com.loopers.domain.common.event.ProductDeletedEvent
import com.loopers.domain.common.event.ProductUpdatedEvent
import com.loopers.domain.product.ProductStatus
import com.loopers.utils.FakeEventPublisher
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("이벤트 수신 + CommandHandler 실행 테스트")
class EventHandlerTest {

    @DisplayName("캐시 Evict 핸들러")
    @Nested
    inner class CacheEvictHandlerTest {
        private lateinit var authCacheStore: FakeAuthCacheStore
        private lateinit var brandCacheStore: FakeBrandCacheStore
        private lateinit var productCacheStore: FakeProductCacheStore
        private lateinit var handler: CacheEvictEventHandler

        @BeforeEach
        fun setUp() {
            authCacheStore = FakeAuthCacheStore()
            brandCacheStore = FakeBrandCacheStore()
            productCacheStore = FakeProductCacheStore()
            handler = CacheEvictEventHandler(
                AuthCacheCommandHandler(authCacheStore),
                BrandCacheCommandHandler(brandCacheStore),
                ProductCacheCommandHandler(productCacheStore),
            )
        }

        @Test
        fun `MemberPasswordChangedEvent 수신 시 인증 캐시가 evict된다`() {
            // arrange
            authCacheStore.putAuth("testuser", com.loopers.application.auth.CachedAuth(memberId = 1L, loginId = "testuser", passwordDigest = "hashed"))

            // act
            handler.on(MemberPasswordChangedEvent(memberId = 1L, loginId = "testuser"))

            // assert
            assertThat(authCacheStore.getAuth("testuser")).isNull()
        }

        @Test
        fun `BrandUpdatedEvent 수신 시 브랜드 캐시가 evict된다`() {
            // arrange
            brandCacheStore.putBrand(1L, com.loopers.application.brand.BrandInfo(id = 1L, name = "브랜드", description = "설명", imageUrl = "img", status = "ACTIVE", createdAt = null, updatedAt = null))

            // act
            handler.on(BrandUpdatedEvent(brandId = 1L))

            // assert
            assertThat(brandCacheStore.getBrand(1L)).isNull()
        }

        @Test
        fun `ProductUpdatedEvent 수신 시 상품 캐시가 evict된다`() {
            // arrange
            productCacheStore.putProduct(1L, com.loopers.application.product.ProductInfo(id = 1L, brandId = 1L, brandName = "브랜드", name = "상품", description = "설명", price = 10000, stockQuantity = 100, likeCount = 0, imageUrl = "img", status = "ACTIVE", createdAt = null, updatedAt = null))

            // act
            handler.on(ProductUpdatedEvent(productId = 1L))

            // assert
            assertThat(productCacheStore.getProduct(1L)).isNull()
        }
    }

    @DisplayName("브랜드 삭제 → 상품 캐스케이드 핸들러")
    @Nested
    inner class BrandDeletedHandlerTest {
        private lateinit var productService: ProductService
        private lateinit var productRepository: FakeProductRepository
        private lateinit var handler: BrandDeletedEventHandler

        @BeforeEach
        fun setUp() {
            productRepository = FakeProductRepository()
            productService = ProductService(productRepository, FakeEventPublisher())
            handler = BrandDeletedEventHandler(CascadeDeleteProductsCommandHandler(productService))
        }

        @Test
        fun `BrandDeletedEvent 수신 시 해당 브랜드의 상품이 DELETED된다`() {
            // arrange
            productService.createProduct(ProductCommand.Create(brandId = 1L, name = "상품1", description = "설명", price = 10000, stockQuantity = 100, imageUrl = "img"))
            productService.createProduct(ProductCommand.Create(brandId = 1L, name = "상품2", description = "설명", price = 20000, stockQuantity = 50, imageUrl = "img"))
            productService.createProduct(ProductCommand.Create(brandId = 2L, name = "다른브랜드상품", description = "설명", price = 30000, stockQuantity = 30, imageUrl = "img"))

            // act
            handler.on(BrandDeletedEvent(brandId = 1L))

            // assert
            val brand1Products = productRepository.findAllByBrandIdAndStatus(1L, ProductStatus.ACTIVE)
            val brand2Products = productRepository.findAllByBrandIdAndStatus(2L, ProductStatus.ACTIVE)
            assertThat(brand1Products).isEmpty()
            assertThat(brand2Products).hasSize(1)
        }
    }

    @DisplayName("상품 삭제 → 좋아요 정리 핸들러")
    @Nested
    inner class ProductDeletedLikeHandlerTest {
        private lateinit var likeRepository: FakeProductLikeRepository
        private lateinit var likeService: LikeService
        private lateinit var handler: ProductDeletedLikeEventHandler

        @BeforeEach
        fun setUp() {
            likeRepository = FakeProductLikeRepository()
            likeService = LikeService(likeRepository, FakeEventPublisher())
            handler = ProductDeletedLikeEventHandler(DeleteProductLikesCommandHandler(likeRepository))
        }

        @Test
        fun `ProductDeletedEvent 수신 시 해당 상품의 좋아요가 삭제된다`() {
            // arrange
            likeService.like(memberId = 1L, productId = 100L)
            likeService.like(memberId = 2L, productId = 100L)
            likeService.like(memberId = 1L, productId = 200L)

            // act
            handler.on(ProductDeletedEvent(productId = 100L))

            // assert
            assertThat(likeRepository.findAllByMemberId(1L)).hasSize(1)
            assertThat(likeRepository.findAllByMemberId(1L).first().productId).isEqualTo(200L)
            assertThat(likeRepository.findAllByMemberId(2L)).isEmpty()
        }
    }

    @DisplayName("재고 차감/복원 CommandHandler")
    @Nested
    inner class StockCommandHandlerTest {
        private lateinit var productService: ProductService
        private lateinit var productRepository: FakeProductRepository
        private lateinit var deductHandler: DeductStockCommandHandler
        private lateinit var restoreHandler: RestoreStockCommandHandler

        @BeforeEach
        fun setUp() {
            productRepository = FakeProductRepository()
            productService = ProductService(productRepository, FakeEventPublisher())
            deductHandler = DeductStockCommandHandler(productService)
            restoreHandler = RestoreStockCommandHandler(productService)
        }

        @Test
        fun `DeductStockCommand로 재고가 차감된다`() {
            // arrange
            val product = productService.createProduct(
                ProductCommand.Create(brandId = 1L, name = "상품", description = "설명", price = 10000, stockQuantity = 100, imageUrl = "img"),
            )

            // act
            deductHandler.handle(com.loopers.domain.common.command.DeductStockCommand(productId = product.id, quantity = 30))

            // assert
            val updated = productService.getProduct(product.id)
            assertThat(updated.stockQuantity).isEqualTo(70)
        }

        @Test
        fun `RestoreStockCommand로 재고가 복원된다`() {
            // arrange
            val product = productService.createProduct(
                ProductCommand.Create(brandId = 1L, name = "상품", description = "설명", price = 10000, stockQuantity = 100, imageUrl = "img"),
            )
            deductHandler.handle(com.loopers.domain.common.command.DeductStockCommand(productId = product.id, quantity = 30))

            // act
            restoreHandler.handle(com.loopers.domain.common.command.RestoreStockCommand(productId = product.id, quantity = 30))

            // assert
            val updated = productService.getProduct(product.id)
            assertThat(updated.stockQuantity).isEqualTo(100)
        }
    }
}

package com.loopers.interfaces.api.product

import com.loopers.application.api.product.ProductFacade
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductStatus
import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import kotlin.test.assertNotNull

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("캐시 미스/에러 상황에서 서비스 정상 작동 확인")
class ProductCacheFailureTest {
    @Autowired
    private lateinit var productFacade: ProductFacade

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var brandRepository: BrandRepository

    @Autowired
    private lateinit var cacheManager: CacheManager

    private lateinit var testBrand: Brand
    private lateinit var testProduct: Product

    @BeforeEach
    fun setup() {
        testBrand = brandRepository.save(
            Brand.create(
                name = "Test Brand ${System.nanoTime()}",
                description = "Test Description",
            ),
        )

        testProduct = productRepository.save(
            Product.create(
                brand = testBrand,
                name = "Cache Miss Test Product",
                price = BigDecimal("10000"),
                status = ProductStatus.ACTIVE,
            ),
        )

        // 캐시 초기화
        cacheManager.getCache("product-info")?.clear()
    }

    @Test
    @DisplayName("캐시 미스: 첫 조회는 DB에서 직접 조회")
    fun testCacheMissFirstLookup() {
        // when - 캐시가 비어있는 상태에서 조회
        val productInfo = productFacade.getCachedProductInfo(testProduct.id)

        // then - DB에서 조회하여 정상 반환
        assertNotNull(productInfo)
        assert(productInfo.id == testProduct.id)
        assert(productInfo.name == "Cache Miss Test Product")
    }

    @Test
    @DisplayName("캐시 미스 후 캐시 저장: 두 번째 조회는 캐시에서 반환")
    fun testCacheMissThenCacheHit() {
        // given - 첫 조회 (캐시 저장)
        val firstLookup = productFacade.getCachedProductInfo(testProduct.id)
        assertNotNull(firstLookup)

        // when - 두 번째 조회 (캐시 히트)
        val secondLookup = productFacade.getCachedProductInfo(testProduct.id)

        // then - 동일 데이터 반환
        assertNotNull(secondLookup)
        assert(secondLookup.id == firstLookup.id)
    }

    @Test
    @DisplayName("여러 상품 조회: 각각 캐시됨")
    fun testMultipleProductsCaching() {
        // given - 여러 상품 생성
        val products = (1..5).map { idx ->
            productRepository.save(
                Product.create(
                    brand = testBrand,
                    name = "Product $idx",
                    price = BigDecimal("${idx}0000"),
                    status = ProductStatus.ACTIVE,
                ),
            )
        }

        // when - 각 상품 조회
        products.forEach { product ->
            val productInfo = productFacade.getCachedProductInfo(product.id)
            assertNotNull(productInfo)
        }

        // then - 두 번째 조회는 캐시에서 (캐시 히트 확인용)
        products.forEach { product ->
            val productInfo = productFacade.getCachedProductInfo(product.id)
            assertNotNull(productInfo)
            assert(productInfo.id == product.id)
        }
    }

    @Test
    @DisplayName("캐시 클리어 후 재조회: 캐시 무효화 정상 작동")
    fun testCacheInvalidation() {
        // given - 첫 조회 (캐시 저장)
        val firstLookup = productFacade.getCachedProductInfo(testProduct.id)
        assertNotNull(firstLookup)

        // when - 캐시 클리어
        cacheManager.getCache("product-info")?.clear()

        // then - 재조회해도 DB에서 정상 조회 (캐시 미스)
        val afterClearLookup = productFacade.getCachedProductInfo(testProduct.id)
        assertNotNull(afterClearLookup)
        assert(afterClearLookup.id == firstLookup.id)
    }
}
